package com.example.cafestatus.cafe.service;

import com.example.cafestatus.cafe.dto.CafeMapItemResponse;
import com.example.cafestatus.cafe.entity.Cafe;
import com.example.cafestatus.status.cache.StatusCacheModel;
import com.example.cafestatus.status.dto.StatusSummary;
import com.example.cafestatus.status.entity.CafeLiveStatus;
import com.example.cafestatus.status.mapper.StatusViewMapper;
import com.example.cafestatus.status.repository.CafeLiveStatusRepository;
import com.example.cafestatus.status.service.CafeStatusCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CafeSearchService {

    private static final Logger log = LoggerFactory.getLogger(CafeSearchService.class);

    private final CafeService cafeService;
    private final CafeLiveStatusRepository statusRepository;
    private final CafeStatusCacheService cacheService;

    public CafeSearchService(CafeService cafeService,
                             CafeLiveStatusRepository statusRepository,
                             CafeStatusCacheService cacheService) {
        this.cafeService = cafeService;
        this.statusRepository = statusRepository;
        this.cacheService = cacheService;
    }

    public List<CafeMapItemResponse> findNearbyWithStatus(double lat, double lng, double radiusMeters, int limit) {
        log.debug("근처 카페 상태 포함 검색: lat={}, lng={}, radius={}m, limit={}", lat, lng, radiusMeters, limit);
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("limit must be between 1 and 200");
        }

        List<Cafe> cafes = cafeService.findNearby(lat, lng, radiusMeters);

        cafes = cafes.stream()
                .sorted(Comparator.comparingDouble(c -> haversineMeters(lat, lng, c.getLatitude(), c.getLongitude())))
                .limit(limit)
                .toList();

        Instant now = Instant.now();
        List<Long> ids = cafes.stream().map(Cafe::getId).toList();

        // Try cache first
        Map<Long, StatusCacheModel> cachedMap = cacheService.getMultiple(ids);

        // Find IDs that missed cache
        List<Long> missIds = ids.stream()
                .filter(id -> !cachedMap.containsKey(id))
                .toList();

        // Fetch misses from DB and backfill cache
        Map<Long, CafeLiveStatus> dbStatusMap = Map.of();
        if (!missIds.isEmpty()) {
            dbStatusMap = statusRepository.findByCafeIdIn(missIds).stream()
                    .collect(Collectors.toMap(CafeLiveStatus::getCafeId, Function.identity()));
            for (var entry : dbStatusMap.entrySet()) {
                cacheService.put(entry.getKey(), entry.getValue());
            }
        }

        Map<Long, CafeLiveStatus> finalDbStatusMap = dbStatusMap;
        return cafes.stream()
                .map(c -> {
                    StatusSummary summary = null;
                    StatusCacheModel cached = cachedMap.get(c.getId());
                    if (cached != null) {
                        summary = cached.toSummary(now);
                    }
                    if (summary == null) {
                        CafeLiveStatus s = finalDbStatusMap.get(c.getId());
                        summary = (s == null) ? StatusViewMapper.unknown() : StatusViewMapper.from(s, now);
                    }
                    return new CafeMapItemResponse(
                            c.getId(), c.getName(),
                            c.getLatitude(), c.getLongitude(),
                            c.getAddress(), c.getCreatedAt(),
                            summary
                    );
                })
                .toList();
    }

    static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
