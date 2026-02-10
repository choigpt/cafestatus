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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock OwnerRepository ownerRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtProvider jwtProvider;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(ownerRepository, refreshTokenRepository, passwordEncoder, jwtProvider);
    }

    private Owner ownerWithId(Long id, String email) {
        Owner owner = new Owner(email, "encoded");
        try {
            var idField = Owner.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(owner, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return owner;
    }

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        @Test
        @DisplayName("성공하면 Access + Refresh 토큰을 반환한다")
        void success() {
            SignUpRequest req = new SignUpRequest("test@test.com", "password123");
            Owner owner = ownerWithId(1L, "test@test.com");

            given(ownerRepository.existsByEmail("test@test.com")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("encoded");
            given(ownerRepository.save(any(Owner.class))).willReturn(owner);
            given(jwtProvider.generateAccessToken(1L)).willReturn("jwt-access-token");
            given(jwtProvider.getRefreshExpirationMs()).willReturn(604800000L);
            given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));

            TokenResponse result = authService.signUp(req);

            assertThat(result.accessToken()).isEqualTo("jwt-access-token");
            assertThat(result.refreshToken()).isNotBlank();
            assertThat(result.tokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("이미 등록된 이메일이면 예외가 발생한다")
        void duplicateEmail() {
            SignUpRequest req = new SignUpRequest("test@test.com", "password123");
            given(ownerRepository.existsByEmail("test@test.com")).willReturn(true);

            assertThatThrownBy(() -> authService.signUp(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 등록된 이메일");
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("성공하면 Access + Refresh 토큰을 반환한다")
        void success() {
            LoginRequest req = new LoginRequest("test@test.com", "password123");
            Owner owner = ownerWithId(1L, "test@test.com");

            given(ownerRepository.findByEmail("test@test.com")).willReturn(Optional.of(owner));
            given(passwordEncoder.matches("password123", "encoded")).willReturn(true);
            given(jwtProvider.generateAccessToken(1L)).willReturn("jwt-access-token");
            given(jwtProvider.getRefreshExpirationMs()).willReturn(604800000L);
            given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));

            TokenResponse result = authService.login(req);

            assertThat(result.accessToken()).isEqualTo("jwt-access-token");
            assertThat(result.refreshToken()).isNotBlank();
        }

        @Test
        @DisplayName("이메일이 존재하지 않으면 UnauthorizedException이 발생한다")
        void emailNotFound() {
            LoginRequest req = new LoginRequest("wrong@test.com", "password123");
            given(ownerRepository.findByEmail("wrong@test.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 UnauthorizedException이 발생한다")
        void wrongPassword() {
            LoginRequest req = new LoginRequest("test@test.com", "wrong");
            Owner owner = new Owner("test@test.com", "encoded");

            given(ownerRepository.findByEmail("test@test.com")).willReturn(Optional.of(owner));
            given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("토큰 갱신")
    class Refresh {

        @Test
        @DisplayName("유효한 리프레시 토큰이면 새 토큰 쌍을 반환한다")
        void success() {
            Owner owner = ownerWithId(1L, "test@test.com");
            RefreshToken rt = new RefreshToken(owner, "valid-refresh", Instant.now().plusSeconds(3600));

            given(refreshTokenRepository.findByToken("valid-refresh")).willReturn(Optional.of(rt));
            given(jwtProvider.generateAccessToken(1L)).willReturn("new-access");
            given(jwtProvider.getRefreshExpirationMs()).willReturn(604800000L);
            given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));

            TokenResponse result = authService.refresh(new RefreshRequest("valid-refresh"));

            assertThat(result.accessToken()).isEqualTo("new-access");
            verify(refreshTokenRepository).delete(rt);
        }

        @Test
        @DisplayName("만료된 리프레시 토큰이면 UnauthorizedException이 발생한다")
        void expired() {
            Owner owner = ownerWithId(1L, "test@test.com");
            RefreshToken rt = new RefreshToken(owner, "expired-refresh", Instant.now().minusSeconds(3600));

            given(refreshTokenRepository.findByToken("expired-refresh")).willReturn(Optional.of(rt));

            assertThatThrownBy(() -> authService.refresh(new RefreshRequest("expired-refresh")))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("존재하지 않는 리프레시 토큰이면 UnauthorizedException이 발생한다")
        void notFound() {
            given(refreshTokenRepository.findByToken("unknown")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(new RefreshRequest("unknown")))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("해당 Owner의 모든 리프레시 토큰을 삭제한다")
        void success() {
            authService.logout(1L);

            verify(refreshTokenRepository).deleteByOwnerId(1L);
        }
    }
}
