package com.example.cafestatus.common.exception;

import com.example.cafestatus.cafe.dto.CafeCreateRequest;
import com.example.cafestatus.support.TestAuthHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    TestAuthHelper authHelper;

    @BeforeEach
    void setUp() {
        authHelper = new TestAuthHelper(mockMvc, objectMapper);
    }

    @Test
    @DisplayName("유효성 검증 실패 시 VALIDATION_ERROR를 반환한다")
    void validationError() throws Exception {
        String token = authHelper.signUpAndGetToken();
        CafeCreateRequest req = new CafeCreateRequest("", null, null, null);

        mockMvc.perform(post("/api/owner/cafes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("잘못된 인자 시 BAD_REQUEST를 반환한다")
    void illegalArgumentError() throws Exception {
        mockMvc.perform(get("/api/cafes/near")
                        .param("lat", "37.5665")
                        .param("lng", "126.9780")
                        .param("radiusMeters", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("필수 파라미터 누락 시 MISSING_PARAMETER를 반환한다")
    void missingParameterError() throws Exception {
        mockMvc.perform(get("/api/cafes/near")
                        .param("lat", "37.5665"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
    }

    @Test
    @DisplayName("파라미터 타입 불일치 시 TYPE_MISMATCH를 반환한다")
    void typeMismatchError() throws Exception {
        mockMvc.perform(get("/api/cafes/near")
                        .param("lat", "not-a-number")
                        .param("lng", "126.9780")
                        .param("radiusMeters", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TYPE_MISMATCH"));
    }

    @Test
    @DisplayName("JSON 파싱 실패 시 INVALID_REQUEST_BODY를 반환한다")
    void invalidJsonError() throws Exception {
        String token = authHelper.signUpAndGetToken();

        mockMvc.perform(post("/api/owner/cafes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
    }

    @Test
    @DisplayName("존재하지 않는 카페 조회 시 NOT_FOUND를 반환한다")
    void cafeNotFoundError() throws Exception {
        mockMvc.perform(get("/api/cafes/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Cafe not found: 999999"));
    }

    @Test
    @DisplayName("인증 없이 보호 엔드포인트 접근 시 401 JSON을 반환한다")
    void unauthorizedError() throws Exception {
        mockMvc.perform(put("/api/owner/cafes/{cafeId}/status", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"crowdLevel\":\"NORMAL\",\"party2\":\"YES\",\"party3\":\"YES\",\"party4\":\"YES\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
