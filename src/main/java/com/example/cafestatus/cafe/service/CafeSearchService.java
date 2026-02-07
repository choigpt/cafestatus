package com.example.cafestatus.cafe.service;

import com.example.cafestatus.cafe.dto.CafeMapItemResponse;
import com.example.cafestatus.cafe.entity.Cafe;
import com.example.cafestatus.status.entity.CafeLiveStatus;
import com.example.cafestatus.status.mapper.StatusViewMapper;
import com.example.cafestatus.status.repository.CafeLiveStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CafeSearchService {

    private static final Logger log = LoggerFactory.getLogger(CafeSearchService.class);

    private final CafeService cafeService;
    private final CafeLiveStatusRepository statusRepository;

    public CafeSearchService(CafeService cafeService, CafeLiveStatusRepository statusRepository) {
        this.cafeService = cafeService;
        this.statusRepository = statusRepository;
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

        Map<Long, CafeLiveStatus> statusMap = statusRepository.findByCafeIdIn(ids).stream()
                .collect(Collectors.toMap(CafeLiveStatus::getCafeId, Function.identity()));

        return cafes.stream()
                .map(c -> {
                    CafeLiveStatus s = statusMap.get(c.getId());
                    var summary = (s == null) ? StatusViewMapper.unknown() : StatusViewMapper.from(s, now);
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
