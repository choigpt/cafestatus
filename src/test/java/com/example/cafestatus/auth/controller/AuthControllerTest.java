package com.example.cafestatus.auth.controller;

import com.example.cafestatus.support.TestAuthHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Auth API 통합 테스트")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    TestAuthHelper authHelper;

    @BeforeEach
    void setUp() {
        authHelper = new TestAuthHelper(mockMvc, objectMapper);
    }

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        @Test
        @DisplayName("성공 시 201과 accessToken + refreshToken을 반환한다")
        void success() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"test@test.com\",\"password\":\"password123\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("중복 이메일이면 400을 반환한다")
        void duplicateEmail() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"dup@test.com\",\"password\":\"password123\"}"))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"dup@test.com\",\"password\":\"password123\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("유효하지 않은 이메일 형식이면 400을 반환한다")
        void invalidEmail() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"not-an-email\",\"password\":\"password123\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("비밀번호가 8자 미만이면 400을 반환한다")
        void shortPassword() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"test@test.com\",\"password\":\"short\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("성공 시 200과 accessToken + refreshToken을 반환한다")
        void success() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"login@test.com\",\"password\":\"password123\"}"))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"login@test.com\",\"password\":\"password123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("잘못된 비밀번호이면 401을 반환한다")
        void wrongPassword() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"wrong@test.com\",\"password\":\"password123\"}"))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"wrong@test.com\",\"password\":\"wrongpassword\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 401을 반환한다")
        void emailNotFound() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"notfound@test.com\",\"password\":\"password123\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("토큰 갱신")
    class Refresh {

        @Test
        @DisplayName("유효한 refreshToken이면 새 토큰 쌍을 반환한다")
        void success() throws Exception {
            String signUpJson = mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"refresh@test.com\",\"password\":\"password123\"}"))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            String refreshToken = objectMapper.readTree(signUpJson).get("refreshToken").asText();

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("존재하지 않는 refreshToken이면 401을 반환한다")
        void invalidToken() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"nonexistent-token\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("사용된 refreshToken은 재사용할 수 없다")
        void usedTokenCannotBeReused() throws Exception {
            String signUpJson = mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"reuse@test.com\",\"password\":\"password123\"}"))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            String refreshToken = objectMapper.readTree(signUpJson).get("refreshToken").asText();

            // First refresh succeeds
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                    .andExpect(status().isOk());

            // Second refresh with same token fails (token rotation)
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("인증된 사용자가 로그아웃하면 204를 반환한다")
        void success() throws Exception {
            String accessToken = authHelper.signUpAndGetToken();

            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("인증 없이 로그아웃하면 401을 반환한다")
        void unauthenticated() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
