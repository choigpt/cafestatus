package com.example.cafestatus.status.service;

import com.example.cafestatus.status.cache.StatusCacheModel;
import com.example.cafestatus.status.dto.StatusSummary;
import com.example.cafestatus.status.entity.CafeLiveStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CafeStatusCacheService {

    Optional<StatusCacheModel> get(Long cafeId);

    Map<Long, StatusCacheModel> getMultiple(List<Long> cafeIds);

    void put(Long cafeId, CafeLiveStatus status);

    boolean publishUpdate(Long cafeId, StatusSummary summary);
}
