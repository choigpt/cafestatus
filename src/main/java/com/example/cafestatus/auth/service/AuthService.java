package com.example.cafestatus.auth.service;

import com.example.cafestatus.auth.dto.LoginRequest;
import com.example.cafestatus.auth.dto.RefreshRequest;
import com.example.cafestatus.auth.dto.SignUpRequest;
import com.example.cafestatus.auth.dto.TokenResponse;
import com.example.cafestatus.auth.entity.Owner;
import com.example.cafestatus.auth.entity.RefreshToken;
import com.example.cafestatus.auth.jwt.JwtProvider;
import com.example.cafestatus.auth.repository.OwnerRepository;
import com.example.cafestatus.auth.repository.RefreshTokenRepository;
import com.example.cafestatus.common.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final OwnerRepository ownerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(OwnerRepository ownerRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider) {
        this.ownerRepository = ownerRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public TokenResponse signUp(SignUpRequest request) {
        if (ownerRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        Owner owner = new Owner(request.email(), encodedPassword);
        Owner saved = ownerRepository.save(owner);

        log.info("사장 회원가입 완료: id={}", saved.getId());
        return issueTokens(saved);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Owner owner = ownerRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(request.password(), owner.getPassword())) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        log.info("사장 로그인 성공: id={}", owner.getId());
        return issueTokens(owner);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 리프레시 토큰입니다"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("리프레시 토큰이 만료되었습니다");
        }

        Owner owner = refreshToken.getOwner();
        refreshTokenRepository.delete(refreshToken);

        log.info("토큰 갱신 성공: ownerId={}", owner.getId());
        return issueTokens(owner);
    }

    @Transactional
    public void logout(Long ownerId) {
        refreshTokenRepository.deleteByOwnerId(ownerId);
        log.info("로그아웃 완료: ownerId={}", ownerId);
    }

    private TokenResponse issueTokens(Owner owner) {
        String accessToken = jwtProvider.generateAccessToken(owner.getId());
        String refreshTokenStr = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusMillis(jwtProvider.getRefreshExpirationMs());

        RefreshToken refreshToken = new RefreshToken(owner, refreshTokenStr, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, refreshTokenStr);
    }
}
