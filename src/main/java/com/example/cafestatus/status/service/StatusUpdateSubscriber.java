package com.example.cafestatus.status.service;

import com.example.cafestatus.status.dto.CafeStatusSseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "true")
public class StatusUpdateSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(StatusUpdateSubscriber.class);

    private final ObjectMapper objectMapper;
    private final StatusSseRegistry sseRegistry;

    public StatusUpdateSubscriber(ObjectMapper objectMapper, StatusSseRegistry sseRegistry) {
        this.objectMapper = objectMapper;
        this.sseRegistry = sseRegistry;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            CafeStatusSseEvent event = objectMapper.readValue(json, CafeStatusSseEvent.class);
            log.debug("Redis Pub/Sub 수신: cafeId={}", event.cafeId());
            sseRegistry.publish(event.cafeId(), event);
        } catch (Exception e) {
            log.warn("Redis Pub/Sub 메시지 처리 실패: error={}", e.getMessage());
        }
    }
}
