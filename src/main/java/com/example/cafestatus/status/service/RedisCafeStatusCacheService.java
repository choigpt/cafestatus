package com.example.cafestatus.status.service;

import com.example.cafestatus.status.cache.StatusCacheModel;
import com.example.cafestatus.status.dto.CafeStatusSseEvent;
import com.example.cafestatus.status.dto.StatusSummary;
import com.example.cafestatus.status.entity.CafeLiveStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "true")
public class RedisCafeStatusCacheService implements CafeStatusCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCafeStatusCacheService.class);
    private static final String KEY_PREFIX = "cafe:status:";
    private static final String CHANNEL = "cafe:status:updates";
    private static final long TTL_SECONDS = 1800;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCafeStatusCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<StatusCacheModel> get(Long cafeId) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + cafeId);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, StatusCacheModel.class));
        } catch (Exception e) {
            log.warn("Redis GET 실패: cafeId={}, error={}", cafeId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Map<Long, StatusCacheModel> getMultiple(List<Long> cafeIds) {
        if (cafeIds.isEmpty()) {
            return Map.of();
        }
        try {
            List<String> keys = cafeIds.stream()
                    .map(id -> KEY_PREFIX + id)
                    .toList();
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null) {
                return Map.of();
            }

            Map<Long, StatusCacheModel> result = new HashMap<>();
            for (int i = 0; i < Math.min(cafeIds.size(), values.size()); i++) {
                String json = values.get(i);
                if (json != null) {
                    try {
                        result.put(cafeIds.get(i), objectMapper.readValue(json, StatusCacheModel.class));
                    } catch (JsonProcessingException e) {
                        log.warn("Redis MGET 역직렬화 실패: cafeId={}", cafeIds.get(i));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Redis MGET 실패: error={}", e.getMessage());
            return Map.of();
        }
    }

    @Override
    public void put(Long cafeId, CafeLiveStatus status) {
        try {
            StatusCacheModel model = StatusCacheModel.from(status);
            String json = objectMapper.writeValueAsString(model);
            redisTemplate.opsForValue().set(KEY_PREFIX + cafeId, json, TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Redis SET 완료: cafeId={}", cafeId);
        } catch (Exception e) {
            log.warn("Redis SET 실패: cafeId={}, error={}", cafeId, e.getMessage());
        }
    }

    @Override
    public boolean publishUpdate(Long cafeId, StatusSummary summary) {
        try {
            CafeStatusSseEvent event = new CafeStatusSseEvent(cafeId, summary);
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(CHANNEL, json);
            log.debug("Redis PUBLISH 완료: cafeId={}", cafeId);
            return true;
        } catch (Exception e) {
            log.warn("Redis PUBLISH 실패: cafeId={}, error={}", cafeId, e.getMessage());
            return false;
        }
    }
}
