package com.example.cafestatus.status.service;

import com.example.cafestatus.cafe.entity.Cafe;
import com.example.cafestatus.cafe.service.CafeService;
import com.example.cafestatus.common.exception.NotFoundException;
import com.example.cafestatus.status.dto.CafeStatusSseEvent;
import com.example.cafestatus.status.dto.UpdateCafeStatusRequest;
import com.example.cafestatus.status.entity.CafeLiveStatus;
import com.example.cafestatus.status.mapper.StatusViewMapper;
import com.example.cafestatus.status.repository.CafeLiveStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class CafeStatusService {

    private static final Logger log = LoggerFactory.getLogger(CafeStatusService.class);
    private static final Duration TTL = Duration.ofMinutes(30);

    private final CafeService cafeService;
    private final CafeLiveStatusRepository repo;
    private final StatusSseRegistry sseRegistry;

    public CafeStatusService(CafeService cafeService,
                             CafeLiveStatusRepository repo,
                             StatusSseRegistry sseRegistry) {
        this.cafeService = cafeService;
        this.repo = repo;
        this.sseRegistry = sseRegistry;
    }

    public CafeLiveStatus getOrThrow(Long cafeId) {
        return repo.findById(cafeId)
                .orElseThrow(() -> new NotFoundException("Status not found for cafeId: " + cafeId));
    }

    @Transactional
    public CafeLiveStatus upsert(Long cafeId, UpdateCafeStatusRequest req) {
        log.info("카페 상태 업데이트: cafeId={}, crowdLevel={}", cafeId, req.crowdLevel());
        Cafe cafe = cafeService.get(cafeId);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(TTL);

        CafeLiveStatus saved = repo.findById(cafeId)
                .map(existing -> {
                    existing.update(
                            req.crowdLevel(),
                            req.party2(),
                            req.party3(),
                            req.party4(),
                            now,
                            expiresAt
                    );
                    return existing;
                })
                .orElseGet(() -> repo.save(new CafeLiveStatus(
                        cafe,
                        req.crowdLevel(),
                        req.party2(),
                        req.party3(),
                        req.party4(),
                        now,
                        expiresAt
                )));

        var statusDto = StatusViewMapper.from(saved, now);
        sseRegistry.publish(cafeId, new CafeStatusSseEvent(cafeId, statusDto));

        return saved;
    }
}
