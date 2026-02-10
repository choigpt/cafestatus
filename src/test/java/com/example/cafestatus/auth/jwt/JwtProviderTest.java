package com.example.cafestatus.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtProvider 단위 테스트")
class JwtProviderTest {

    JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
                "testSecretKeyThatIsLongEnoughForHS256AlgorithmTesting1234567890",
                900000L,
                604800000L
        );
    }

    @Test
    @DisplayName("Access 토큰을 생성하면 유효한 토큰이 반환된다")
    void generateAccessToken() {
        String token = jwtProvider.generateAccessToken(1L);

        assertThat(token).isNotBlank();
        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("토큰에서 ownerId를 추출할 수 있다")
    void getOwnerIdFromToken() {
        String token = jwtProvider.generateAccessToken(42L);

        Long ownerId = jwtProvider.getOwnerIdFromToken(token);

        assertThat(ownerId).isEqualTo(42L);
    }

    @Test
    @DisplayName("잘못된 토큰은 유효하지 않다")
    void invalidToken() {
        assertThat(jwtProvider.validateToken("invalid.token.here")).isFalse();
    }

    @Test
    @DisplayName("null 토큰은 유효하지 않다")
    void nullToken() {
        assertThat(jwtProvider.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 유효하지 않다")
    void expiredToken() {
        JwtProvider shortLivedProvider = new JwtProvider(
                "testSecretKeyThatIsLongEnoughForHS256AlgorithmTesting1234567890",
                -1000L,
                -1000L
        );

        String token = shortLivedProvider.generateAccessToken(1L);

        assertThat(shortLivedProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("refreshExpirationMs를 반환한다")
    void getRefreshExpirationMs() {
        assertThat(jwtProvider.getRefreshExpirationMs()).isEqualTo(604800000L);
    }
}
