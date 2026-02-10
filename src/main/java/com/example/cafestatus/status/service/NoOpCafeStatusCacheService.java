package com.example.cafestatus.status.service;

import com.example.cafestatus.status.cache.StatusCacheModel;
import com.example.cafestatus.status.dto.StatusSummary;
import com.example.cafestatus.status.entity.CafeLiveStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "cache.redis.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpCafeStatusCacheService implements CafeStatusCacheService {

    @Override
    public Optional<StatusCacheModel> get(Long cafeId) {
        return Optional.empty();
    }

    @Override
    public Map<Long, StatusCacheModel> getMultiple(List<Long> cafeIds) {
        return Map.of();
    }

    @Override
    public void put(Long cafeId, CafeLiveStatus status) {
        // no-op
    }

    @Override
    public boolean publishUpdate(Long cafeId, StatusSummary summary) {
        return false;
    }
}
