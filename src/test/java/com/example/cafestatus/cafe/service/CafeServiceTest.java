package com.example.cafestatus.cafe.service;

import com.example.cafestatus.auth.entity.Owner;
import com.example.cafestatus.auth.repository.OwnerRepository;
import com.example.cafestatus.cafe.dto.CafeCreateRequest;
import com.example.cafestatus.cafe.entity.Cafe;
import com.example.cafestatus.cafe.repository.CafeRepository;
import com.example.cafestatus.common.exception.ForbiddenException;
import com.example.cafestatus.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CafeService 단위 테스트")
class CafeServiceTest {

    @Mock
    CafeRepository cafeRepository;

    @Mock
    OwnerRepository ownerRepository;

    CafeService cafeService;

    @BeforeEach
    void setUp() {
        cafeService = new CafeService(cafeRepository, ownerRepository);
    }

    @Nested
    @DisplayName("카페 생성")
    class Create {

        @Test
        @DisplayName("유효한 요청이면 카페가 생성된다")
        void success() {
            CafeCreateRequest req = new CafeCreateRequest("테스트카페", 37.5665, 126.9780, "서울시");
            Owner owner = new Owner("test@test.com", "encoded");
            Cafe savedCafe = new Cafe("테스트카페", 37.5665, 126.9780, "서울시", owner);

            given(ownerRepository.findById(1L)).willReturn(Optional.of(owner));
            given(cafeRepository.save(any(Cafe.class))).willReturn(savedCafe);

            Cafe result = cafeService.create(req, 1L);

            assertThat(result.getName()).isEqualTo("테스트카페");
            assertThat(result.getOwner()).isEqualTo(owner);
        }

        @Test
        @DisplayName("위도가 범위를 벗어나면 예외가 발생한다")
        void invalidLatitude() {
            CafeCreateRequest req = new CafeCreateRequest("카페", 91.0, 126.9780, null);

            assertThatThrownBy(() -> cafeService.create(req, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid latitude");
        }

        @Test
        @DisplayName("경도가 범위를 벗어나면 예외가 발생한다")
        void invalidLongitude() {
            CafeCreateRequest req = new CafeCreateRequest("카페", 37.5665, 181.0, null);

            assertThatThrownBy(() -> cafeService.create(req, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid longitude");
        }

        @Test
        @DisplayName("존재하지 않는 소유자면 NotFoundException이 발생한다")
        void ownerNotFound() {
            CafeCreateRequest req = new CafeCreateRequest("카페", 37.5665, 126.9780, null);
            given(ownerRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> cafeService.create(req, 999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Owner not found");
        }
    }

    @Nested
    @DisplayName("카페 조회")
    class Get {

        @Test
        @DisplayName("존재하는 ID로 조회하면 카페를 반환한다")
        void found() {
            Cafe cafe = new Cafe("카페", 37.5665, 126.9780, null, null);
            given(cafeRepository.findById(1L)).willReturn(Optional.of(cafe));

            Cafe result = cafeService.get(1L);

            assertThat(result.getName()).isEqualTo("카페");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 NotFoundException이 발생한다")
        void notFound() {
            given(cafeRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> cafeService.get(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Cafe not found");
        }
    }

    @Nested
    @DisplayName("근처 카페 검색")
    class FindNearby {

        @Test
        @DisplayName("반경이 0 이하이면 예외가 발생한다")
        void invalidRadiusZero() {
            assertThatThrownBy(() -> cafeService.findNearby(37.5665, 126.9780, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("radiusMeters must be between 1 and 10000");
        }

        @Test
        @DisplayName("반경이 10000을 초과하면 예외가 발생한다")
        void invalidRadiusTooLarge() {
            assertThatThrownBy(() -> cafeService.findNearby(37.5665, 126.9780, 10001))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("radiusMeters must be between 1 and 10000");
        }
    }

    @Nested
    @DisplayName("소유권 검증")
    class VerifyOwnership {

        @Test
        @DisplayName("소유자가 아니면 ForbiddenException이 발생한다")
        void notOwner() {
            Owner owner = new Owner("test@test.com", "encoded");
            // Use reflection to set ID for testing
            try {
                var idField = Owner.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(owner, 2L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Cafe cafe = new Cafe("카페", 37.5665, 126.9780, null, owner);
            given(cafeRepository.findByIdWithOwner(1L)).willReturn(Optional.of(cafe));

            assertThatThrownBy(() -> cafeService.verifyOwnership(1L, 999L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("소유자가 일치하면 Cafe를 반환한다")
        void success() {
            Owner owner = new Owner("test@test.com", "encoded");
            try {
                var idField = Owner.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(owner, 1L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Cafe cafe = new Cafe("카페", 37.5665, 126.9780, null, owner);
            given(cafeRepository.findByIdWithOwner(1L)).willReturn(Optional.of(cafe));

            Cafe result = cafeService.verifyOwnership(1L, 1L);

            assertThat(result.getName()).isEqualTo("카페");
        }

        @Test
        @DisplayName("소유자가 null이면 ForbiddenException이 발생한다")
        void nullOwner() {
            Cafe cafe = new Cafe("카페", 37.5665, 126.9780, null, null);
            given(cafeRepository.findByIdWithOwner(1L)).willReturn(Optional.of(cafe));

            assertThatThrownBy(() -> cafeService.verifyOwnership(1L, 1L))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
