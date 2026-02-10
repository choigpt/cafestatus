package com.example.cafestatus.status.controller;

import com.example.cafestatus.cafe.dto.CafeCreateRequest;
import com.example.cafestatus.status.dto.UpdateCafeStatusRequest;
import com.example.cafestatus.status.entity.Availability;
import com.example.cafestatus.status.entity.CrowdLevel;
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
@DisplayName("카페 상태(매장입력) 플로우 테스트")
public class CafeStatusFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    TestAuthHelper authHelper;

    @BeforeEach
    void setUp() {
        authHelper = new TestAuthHelper(mockMvc, objectMapper);
    }

    @Test
    @DisplayName("매장이 JWT로 상태를 업데이트하면 손님 조회에서 동일한 값이 나온다")
    void ownerUpdates_thenCustomerGets() throws Exception {
        String token = authHelper.signUpAndGetToken();
        long cafeId = createCafe(token, "카페상태", 37.5665, 126.9780);

        UpdateCafeStatusRequest req = new UpdateCafeStatusRequest(
                CrowdLevel.NORMAL,
                Availability.YES,
                Availability.MAYBE,
                Availability.NO
        );

        mockMvc.perform(put("/api/owner/cafes/{id}/status", cafeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cafeId").value((int) cafeId))
                .andExpect(jsonPath("$.crowdLevel").value("NORMAL"))
                .andExpect(jsonPath("$.party2").value("YES"))
                .andExpect(jsonPath("$.party3").value("MAYBE"))
                .andExpect(jsonPath("$.party4").value("NO"))
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.expiresAt").exists());

        mockMvc.perform(get("/api/cafes/{id}/status", cafeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.crowdLevel").value("NORMAL"))
                .andExpect(jsonPath("$.party2").value("YES"))
                .andExpect(jsonPath("$.party3").value("MAYBE"))
                .andExpect(jsonPath("$.party4").value("NO"))
                .andExpect(jsonPath("$.stale").isBoolean());
    }

    @Test
    @DisplayName("다른 사장 JWT로 남의 카페 상태 업데이트는 403이다")
    void differentOwner_forbidden() throws Exception {
        String token1 = authHelper.signUpAndGetToken();
        long cafeId = createCafe(token1, "카페토큰", 37.5665, 126.9780);

        String token2 = authHelper.signUpAndGetToken();

        UpdateCafeStatusRequest req = new UpdateCafeStatusRequest(
                CrowdLevel.FULL,
                Availability.NO,
                Availability.NO,
                Availability.NO
        );

        mockMvc.perform(put("/api/owner/cafes/{id}/status", cafeId)
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("JWT 없이 상태 업데이트는 401이다")
    void missingAuth_unauthorized() throws Exception {
        String token = authHelper.signUpAndGetToken();
        long cafeId = createCafe(token, "카페토큰없음", 37.5665, 126.9780);

        UpdateCafeStatusRequest req = new UpdateCafeStatusRequest(
                CrowdLevel.FULL,
                Availability.NO,
                Availability.NO,
                Availability.NO
        );

        mockMvc.perform(put("/api/owner/cafes/{id}/status", cafeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("존재하지 않는 카페의 상태 조회는 404이다")
    void statusNotFound_notFound() throws Exception {
        mockMvc.perform(get("/api/cafes/{id}/status", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private long createCafe(String token, String name, double lat, double lng) throws Exception {
        CafeCreateRequest req = new CafeCreateRequest(name, lat, lng, null);

        String json = mockMvc.perform(post("/api/owner/cafes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(json).get("id").asLong();
    }
}
