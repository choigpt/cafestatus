package com.example.cafestatus.status.service;

import com.example.cafestatus.cafe.entity.Cafe;
import com.example.cafestatus.cafe.service.CafeService;
import com.example.cafestatus.common.exception.NotFoundException;
import com.example.cafestatus.status.dto.UpdateCafeStatusRequest;
import com.example.cafestatus.status.entity.Availability;
import com.example.cafestatus.status.entity.CafeLiveStatus;
import com.example.cafestatus.status.entity.CrowdLevel;
import com.example.cafestatus.status.repository.CafeLiveStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CafeStatusService 단위 테스트")
class CafeStatusServiceTest {

    @Mock
    CafeService cafeService;

    @Mock
    CafeLiveStatusRepository statusRepository;

    @Mock
    StatusSseRegistry sseRegistry;

    @Mock
    CafeStatusCacheService cacheService;

    CafeStatusService cafeStatusService;

    @BeforeEach
    void setUp() {
        cafeStatusService = new CafeStatusService(cafeService, statusRepository, sseRegistry, cacheService);
    }

    @Nested
    @DisplayName("상태 조회")
    class GetOrThrow {

        @Test
        @DisplayName("존재하는 카페 상태를 조회하면 상태를 반환한다")
        void found() {
            Cafe cafe = new Cafe("카페", 37.5665, 126.9780, null, null);
            CafeLiveStatus status = new CafeLiveStatus(
                    cafe, CrowdLevel.NORMAL, Availability.YES, Availability.MAYBE, Availability.NO,
                    Instant.now(), Instant.now().plusSeconds(1800)
            );
            given(statusRepository.findById(1L)).willReturn(Optional.of(status));

            CafeLiveStatus result = cafeStatusService.getOrThrow(1L);

            assertThat(result.getCrowdLevel()).isEqualTo(CrowdLevel.NORMAL);
        }

        @Test
        @DisplayName("존재하지 않는 카페 상태를 조회하면 NotFoundException이 발생한다")
        void notFound() {
            given(statusRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> cafeStatusService.getOrThrow(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Status not found for cafeId: 999");
        }
    }

    @Nested
    @DisplayName("상태 업데이트")
    class Upsert {

        @Test
        @DisplayName("새 상태를 생성하면 캐시에 저장되고 SSE가 발행된다")
        void createNewStatus() {
            Cafe cafe = new Cafe("카페", 37.5665, 126.9780, null, null);
            UpdateCafeStatusRequest req = new UpdateCafeStatusRequest(
                    CrowdLevel.RELAXED, Availability.YES, Availability.YES, Availability.YES
            );

            given(cafeService.get(1L)).willReturn(cafe);
            given(statusRepository.findById(1L)).willReturn(Optional.empty());
            given(statusRepository.save(any(CafeLiveStatus.class))).willAnswer(inv -> inv.getArgument(0));
            given(cacheService.publishUpdate(any(), any())).willReturn(false);

            CafeLiveStatus result = cafeStatusService.upsert(1L, req);

            assertThat(result.getCrowdLevel()).isEqualTo(CrowdLevel.RELAXED);
            verify(cacheService).put(any(), any());
            verify(sseRegistry).publish(any(), any());
        }

        @Test
        @DisplayName("기존 상태를 업데이트하면 캐시에 저장되고 SSE가 발행된다")
        void updateExistingStatus() {
            Cafe cafe = new Cafe("카페", 37.5665, 126.9780, null, null);
            CafeLiveStatus existingStatus = new CafeLiveStatus(
                    cafe, CrowdLevel.NORMAL, Availability.YES, Availability.YES, Availability.YES,
                    Instant.now().minusSeconds(300), Instant.now().plusSeconds(1500)
            );
            UpdateCafeStatusRequest req = new UpdateCafeStatusRequest(
                    CrowdLevel.FULL, Availability.NO, Availability.NO, Availability.NO
            );

            given(cafeService.get(1L)).willReturn(cafe);
            given(statusRepository.findById(1L)).willReturn(Optional.of(existingStatus));
            given(cacheService.publishUpdate(any(), any())).willReturn(false);

            CafeLiveStatus result = cafeStatusService.upsert(1L, req);

            assertThat(result.getCrowdLevel()).isEqualTo(CrowdLevel.FULL);
            assertThat(result.getParty2()).isEqualTo(Availability.NO);
            verify(cacheService).put(any(), any());
            verify(sseRegistry).publish(any(), any());
        }

        @Test
        @DisplayName("Redis Pub/Sub가 활성화되면 로컬 SSE 직접 발행을 건너뛴다")
        void redisPubSubActive_skipsLocalSse() {
            Cafe cafe = new Cafe("카페", 37.5665, 126.9780, null, null);
            UpdateCafeStatusRequest req = new UpdateCafeStatusRequest(
                    CrowdLevel.RELAXED, Availability.YES, Availability.YES, Availability.YES
            );

            given(cafeService.get(1L)).willReturn(cafe);
            given(statusRepository.findById(1L)).willReturn(Optional.empty());
            given(statusRepository.save(any(CafeLiveStatus.class))).willAnswer(inv -> inv.getArgument(0));
            given(cacheService.publishUpdate(any(), any())).willReturn(true);

            cafeStatusService.upsert(1L, req);

            verify(cacheService).put(any(), any());
            verify(cacheService).publishUpdate(any(), any());
        }
    }
}
