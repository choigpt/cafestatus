package com.example.cafestatus.status.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class StatusSseRegistry {

    private static final Logger log = LoggerFactory.getLogger(StatusSseRegistry.class);

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByCafeId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(List<Long> cafeIds) {
        log.info("SSE 구독 요청: cafeIds={}", cafeIds);
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(30).toMillis());

        Runnable cleanup = () -> {
            log.debug("SSE 연결 정리: cafeIds={}", cafeIds);
            cafeIds.forEach(id -> removeEmitter(id, emitter));
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            log.debug("SSE 타임아웃: cafeIds={}", cafeIds);
            cleanup.run();
        });
        emitter.onError(e -> {
            log.warn("SSE 오류 발생: cafeIds={}, error={}", cafeIds, e.getMessage());
            cleanup.run();
        });

        cafeIds.forEach(id -> emittersByCafeId
                .computeIfAbsent(id, k -> new CopyOnWriteArrayList<>())
                .add(emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
            log.debug("SSE 연결 완료: cafeIds={}", cafeIds);
        } catch (IOException e) {
            log.warn("SSE 초기 연결 실패: cafeIds={}, error={}", cafeIds, e.getMessage());
            cleanup.run();
        }

        return emitter;
    }

    public void publish(Long cafeId, Object payload) {
        List<SseEmitter> list = emittersByCafeId.get(cafeId);
        if (list == null) {
            log.debug("SSE 발행 대상 없음: cafeId={}", cafeId);
            return;
        }

        log.debug("SSE 상태 발행: cafeId={}, subscribers={}", cafeId, list.size());
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("status").data(payload));
            } catch (IOException e) {
                log.warn("SSE 발행 실패, 연결 제거: cafeId={}", cafeId);
                removeEmitter(cafeId, emitter);
            }
        }
    }

    private void removeEmitter(Long cafeId, SseEmitter emitter) {
        var list = emittersByCafeId.get(cafeId);
        if (list == null) return;
        list.remove(emitter);
        if (list.isEmpty()) {
            emittersByCafeId.remove(cafeId);
            log.debug("SSE 구독자 모두 제거됨: cafeId={}", cafeId);
        }
    }

    @Scheduled(fixedRate = 25000)
    public void ping() {
        int totalEmitters = emittersByCafeId.values().stream().mapToInt(List::size).sum();
        if (totalEmitters > 0) {
            log.trace("SSE ping 전송: totalEmitters={}", totalEmitters);
        }
        emittersByCafeId.forEach((cafeId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().name("ping").data("ok"));
                } catch (Exception e) {
                    log.debug("SSE ping 실패, 연결 제거: cafeId={}", cafeId);
                    removeEmitter(cafeId, emitter);
                }
            }
        });
    }
}
