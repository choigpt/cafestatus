package com.example.cafestatus.cafe.service;

import com.example.cafestatus.cafe.dto.CafeCreateRequest;
import com.example.cafestatus.cafe.dto.CafeOwnerTokenResponse;
import com.example.cafestatus.cafe.dto.CafeUpdateRequest;
import com.example.cafestatus.cafe.entity.Cafe;
import com.example.cafestatus.cafe.repository.CafeRepository;
import com.example.cafestatus.common.config.OwnerTokenEncoder;
import com.example.cafestatus.common.exception.ForbiddenException;
import com.example.cafestatus.common.exception.NotFoundException;
import com.example.cafestatus.common.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CafeService {

    private static final Logger log = LoggerFactory.getLogger(CafeService.class);

    private final CafeRepository cafeRepository;
    private final OwnerTokenEncoder ownerTokenEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public CafeService(CafeRepository cafeRepository, OwnerTokenEncoder ownerTokenEncoder) {
        this.cafeRepository = cafeRepository;
        this.ownerTokenEncoder = ownerTokenEncoder;
    }

    @Transactional
    public CafeOwnerTokenResponse create(CafeCreateRequest req) {
        log.info("카페 생성 요청: name={}, lat={}, lng={}", req.name(), req.latitude(), req.longitude());
        validateCoordinates(req.latitude(), req.longitude());

        String plainToken = generateOwnerToken();
        String hashedToken = ownerTokenEncoder.encode(plainToken);

        Cafe cafe = new Cafe(req.name(), req.latitude(), req.longitude(), req.address(), hashedToken);
        Cafe saved = cafeRepository.save(cafe);
        log.info("카페 생성 완료: id={}", saved.getId());
        return new CafeOwnerTokenResponse(saved.getId(), plainToken);
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

    public Cafe verifyOwner(Long cafeId, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthorizedException("Missing owner token");
        }
        Cafe cafe = get(cafeId);
        if (!ownerTokenEncoder.matches(rawToken, cafe.getOwnerToken())) {
            throw new ForbiddenException("Invalid owner token");
        }
        return cafe;
    }

    @Transactional
    public Cafe update(Long cafeId, String rawToken, CafeUpdateRequest req) {
        Cafe cafe = verifyOwner(cafeId, rawToken);
        cafe.update(req.name(), req.address());
        log.info("카페 정보 수정 완료: id={}", cafeId);
        return cafe;
    }

    @Transactional
    public void delete(Long cafeId, String rawToken) {
        Cafe cafe = verifyOwner(cafeId, rawToken);
        cafeRepository.delete(cafe);
        log.info("카페 삭제 완료: id={}", cafeId);
    }

    @Transactional
    public CafeOwnerTokenResponse rotateOwnerToken(Long cafeId, String currentRawToken) {
        Cafe cafe = verifyOwner(cafeId, currentRawToken);
        String newPlainToken = generateOwnerToken();
        String newHashedToken = ownerTokenEncoder.encode(newPlainToken);
        cafe.rotateOwnerToken(newHashedToken);
        log.info("카페 토큰 갱신 완료: id={}", cafeId);
        return new CafeOwnerTokenResponse(cafeId, newPlainToken);
    }

    private void validateCoordinates(double lat, double lng) {
        if (lat < -90 || lat > 90) throw new IllegalArgumentException("Invalid latitude");
        if (lng < -180 || lng > 180) throw new IllegalArgumentException("Invalid longitude");
    }

    private String generateOwnerToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
