package com.example.cafestatus.cafe.service;

import com.example.cafestatus.cafe.dto.CafeCreateRequest;
import com.example.cafestatus.cafe.dto.CafeOwnerTokenResponse;
import com.example.cafestatus.cafe.entity.Cafe;
import com.example.cafestatus.cafe.repository.CafeRepository;
import com.example.cafestatus.common.config.OwnerTokenEncoder;
import com.example.cafestatus.common.exception.ForbiddenException;
import com.example.cafestatus.common.exception.NotFoundException;
import com.example.cafestatus.common.exception.UnauthorizedException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CafeService 단위 테스트")
class CafeServiceTest {

    @Mock
    CafeRepository cafeRepository;

    @Mock
    OwnerTokenEncoder ownerTokenEncoder;

    CafeService cafeService;

    @BeforeEach
    void setUp() {
        cafeService = new CafeService(cafeRepository, ownerTokenEncoder);
    }

    @Nested
    @DisplayName("카페 생성")
    class Create {

        @Test
        @DisplayName("유효한 요청이면 카페가 생성되고 평문 토큰이 반환된다")
        void success() {
            CafeCreateRequest req = new CafeCreateRequest("테스트카페", 37.5665, 126.9780, "서울시");
            Cafe savedCafe = new Cafe("테스트카페", 37.5665, 126.9780, "서울시", "hashed_token");

            given(ownerTokenEncoder.encode(anyString())).willReturn("hashed_token");
            given(cafeRepository.save(any(Cafe.class))).willReturn(savedCafe);

            CafeOwnerTokenResponse result = cafeService.create(req);

            assertThat(result.cafeId()).isEqualTo(savedCafe.getId());
            assertThat(result.ownerToken()).isNotEqualTo("hashed_token");
        }

        @Test
        @DisplayName("위도가 범위를 벗어나면 예외가 발생한다")
        void invalidLatitude() {
            CafeCreateRequest req = new CafeCreateRequest("카페", 91.0, 126.9780, null);

            assertThatThrownBy(() -> cafeService.create(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid latitude");
        }

        @Test
        @DisplayName("경도가 범위를 벗어나면 예외가 발생한다")
        void invalidLongitude() {
            CafeCreateRequest req = new CafeCreateRequest("카페", 37.5665, 181.0, null);

            assertThatThrownBy(() -> cafeService.create(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid longitude");
        }
    }

    @Nested
    @DisplayName("카페 조회")
    class Get {

        @Test
        @DisplayName("존재하는 ID로 조회하면 카페를 반환한다")
        void found() {
            Cafe cafe = new Cafe("카페", 37.5665, 126.9780, null, "token");
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
    @DisplayName("소유자 인증")
    class VerifyOwner {

        @Test
        @DisplayName("토큰이 null이면 UnauthorizedException이 발생한다")
        void nullToken() {
            assertThatThrownBy(() -> cafeService.verifyOwner(1L, null))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("토큰이 빈 문자열이면 UnauthorizedException이 발생한다")
        void blankToken() {
            assertThatThrownBy(() -> cafeService.verifyOwner(1L, "  "))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("토큰이 일치하지 않으면 ForbiddenException이 발생한다")
        void invalidToken() {
            Cafe cafe = new Cafe("카페", 37.5665, 126.9780, null, "hashed");
            given(cafeRepository.findById(1L)).willReturn(Optional.of(cafe));
            given(ownerTokenEncoder.matches("wrong", "hashed")).willReturn(false);

            assertThatThrownBy(() -> cafeService.verifyOwner(1L, "wrong"))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("토큰이 일치하면 카페를 반환한다")
        void validToken() {
            Cafe cafe = new Cafe("카페", 37.5665, 126.9780, null, "hashed");
            given(cafeRepository.findById(1L)).willReturn(Optional.of(cafe));
            given(ownerTokenEncoder.matches("correct", "hashed")).willReturn(true);

            Cafe result = cafeService.verifyOwner(1L, "correct");
            assertThat(result.getName()).isEqualTo("카페");
        }
    }
}
