package com.example.cafestatus.status.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("StatusSseRegistry 단위 테스트")
class StatusSseRegistryTest {

    StatusSseRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new StatusSseRegistry();
    }

    @Test
    @DisplayName("subscribe 하면 SseEmitter를 반환한다")
    void subscribe_returnsEmitter() {
        SseEmitter emitter = registry.subscribe(List.of(1L, 2L));
        assertThat(emitter).isNotNull();
    }

    @Test
    @DisplayName("구독자가 없는 cafeId에 publish해도 예외가 발생하지 않는다")
    void publish_noSubscribers_doesNotThrow() {
        assertThatCode(() -> registry.publish(999L, Map.of("key", "value")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("구독 후 publish하면 예외가 발생하지 않는다")
    void subscribe_thenPublish_doesNotThrow() {
        registry.subscribe(List.of(1L));

        assertThatCode(() -> registry.publish(1L, Map.of("cafeId", 1L, "status", "NORMAL")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ping 호출이 예외 없이 동작한다")
    void ping_doesNotThrow() {
        registry.subscribe(List.of(1L));
        assertThatCode(() -> registry.ping()).doesNotThrowAnyException();
    }
}
