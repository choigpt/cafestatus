package com.example.cafestatus.cafe.service;

import com.example.cafestatus.auth.entity.Owner;
import com.example.cafestatus.auth.repository.OwnerRepository;
import com.example.cafestatus.cafe.dto.CafeCreateRequest;
import com.example.cafestatus.cafe.dto.CafeUpdateRequest;
import com.example.cafestatus.cafe.entity.Cafe;
import com.example.cafestatus.cafe.repository.CafeRepository;
import com.example.cafestatus.common.exception.ForbiddenException;
import com.example.cafestatus.common.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CafeService {

    private static final Logger log = LoggerFactory.getLogger(CafeService.class);

    private final CafeRepository cafeRepository;
    private final OwnerRepository ownerRepository;

    public CafeService(CafeRepository cafeRepository, OwnerRepository ownerRepository) {
        this.cafeRepository = cafeRepository;
        this.ownerRepository = ownerRepository;
    }

    @Transactional
    public Cafe create(CafeCreateRequest req, Long ownerId) {
        log.info("카페 생성 요청: name={}, lat={}, lng={}, ownerId={}", req.name(), req.latitude(), req.longitude(), ownerId);
        validateCoordinates(req.latitude(), req.longitude());

        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Owner not found: " + ownerId));

        Cafe cafe = new Cafe(req.name(), req.latitude(), req.longitude(), req.address(), owner);
        Cafe saved = cafeRepository.save(cafe);
        log.info("카페 생성 완료: id={}", saved.getId());
        return saved;
    }

    public Cafe get(Long id) {
        return cafeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + id));
    }

    public Page<Cafe> list(Pageable pageable) {
        return cafeRepository.findAll(pageable);
    }

    public Page<Cafe> searchByName(String name, Pageable pageable) {
        return cafeRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    public List<Cafe> findNearby(double lat, double lng, double radiusMeters) {
        validateCoordinates(lat, lng);
        if (radiusMeters <= 0 || radiusMeters > 10_000) {
            throw new IllegalArgumentException("radiusMeters must be between 1 and 10000");
        }

        double latDelta = radiusMeters / 111_000.0;
        double lngDelta = radiusMeters / (111_000.0 * Math.cos(Math.toRadians(lat)));

        return cafeRepository.findInBoundingBox(
                lat - latDelta, lat + latDelta,
                lng - lngDelta, lng + lngDelta
        );
    }

    public Cafe verifyOwnership(Long cafeId, Long ownerId) {
        Cafe cafe = cafeRepository.findByIdWithOwner(cafeId)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + cafeId));
        if (cafe.getOwner() == null || !cafe.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("해당 카페의 소유자가 아닙니다");
        }
        return cafe;
    }

    @Transactional
    public Cafe update(Long cafeId, Long ownerId, CafeUpdateRequest req) {
        Cafe cafe = verifyOwnership(cafeId, ownerId);
        cafe.update(req.name(), req.address());
        log.info("카페 정보 수정 완료: id={}", cafeId);
        return cafe;
    }

    @Transactional
    public void delete(Long cafeId, Long ownerId) {
        Cafe cafe = verifyOwnership(cafeId, ownerId);
        cafeRepository.delete(cafe);
        log.info("카페 삭제 완료: id={}", cafeId);
    }

    public Page<Cafe> findByOwner(Long ownerId, Pageable pageable) {
        return cafeRepository.findByOwnerId(ownerId, pageable);
    }

    private void validateCoordinates(double lat, double lng) {
        if (lat < -90 || lat > 90) throw new IllegalArgumentException("Invalid latitude");
        if (lng < -180 || lng > 180) throw new IllegalArgumentException("Invalid longitude");
    }
}
