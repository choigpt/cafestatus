package com.example.cafestatus.cafe.controller;

import com.example.cafestatus.cafe.dto.CafeCreateRequest;
import com.example.cafestatus.status.entity.Availability;
import com.example.cafestatus.status.entity.CrowdLevel;
import com.example.cafestatus.status.dto.UpdateCafeStatusRequest;
import com.example.cafestatus.support.CreatedCafe;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("카페 API 테스트")
class CafeControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("카페를 등록하면 201과 함께 cafeId와 ownerToken을 반환한다")
    void createCafe_returnsOwnerTokenAndCafeId() throws Exception {
        CafeCreateRequest req = new CafeCreateRequest(
                "카페A", 37.5665, 126.9780, "서울 어딘가"
        );

        mockMvc.perform(post("/api/cafes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cafeId").isNumber())
                .andExpect(jsonPath("$.ownerToken", notNullValue()))
                .andExpect(jsonPath("$.ownerToken", matchesPattern("^.{32,}$")));
    }

    @Test
    @DisplayName("카페 ID로 조회하면 카페 상세 정보를 반환한다")
    void getCafe_returnsCafeResponse() throws Exception {
        CreatedCafe created = createCafeWithToken("카페B", 37.5665, 126.9780);

        mockMvc.perform(get("/api/cafes/{id}", created.cafeId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) created.cafeId()))
                .andExpect(jsonPath("$.name").value("카페B"))
                .andExpect(jsonPath("$.latitude").value(37.5665))
                .andExpect(jsonPath("$.longitude").value(126.9780))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("존재하지 않는 카페 조회 시 404를 반환한다")
    void getCafe_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/cafes/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("근처 검색 시 반경 내 카페만 조회된다")
    void near_returnsCafesWithinRadiusBoundingBox() throws Exception {
        createCafeWithToken("카페근처1", 37.5665, 126.9780);
        createCafeWithToken("카페근처2", 37.5667, 126.9782);
        createCafeWithToken("카페멀리", 35.1796, 129.0756);

        mockMvc.perform(get("/api/cafes/near")
                        .param("lat", "37.5665")
                        .param("lng", "126.9780")
                        .param("radiusMeters", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItems("카페근처1", "카페근처2")))
                .andExpect(jsonPath("$[*].name", not(hasItem("카페멀리"))));
    }

    @Test
    @DisplayName("status 없는 카페는 UNKNOWN + ageMinutes -1 이다")
    void near_withoutStatus_returnsUnknown() throws Exception {
        CreatedCafe created = createCafeWithToken("카페무상태", 37.5665, 126.9780);

        mockMvc.perform(get("/api/cafes/near")
                        .param("lat", "37.5665")
                        .param("lng", "126.9780")
                        .param("radiusMeters", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value((int) created.cafeId()))
                .andExpect(jsonPath("$[0].status.crowdLevel").value("UNKNOWN"))
                .andExpect(jsonPath("$[0].status.ageMinutes").value(-1))
                .andExpect(jsonPath("$[0].status.stale").value(true));
    }

    @Test
    @DisplayName("status 있는 카페는 마지막 입력값 유지 + ageMinutes >= 0")
    void near_withStatus_returnsLastValue() throws Exception {
        CreatedCafe created = createCafeWithToken("카페상태있음", 37.5665, 126.9780);

        UpdateCafeStatusRequest req = new UpdateCafeStatusRequest(
                CrowdLevel.NORMAL,
                Availability.YES,
                Availability.MAYBE,
                Availability.NO
        );

        mockMvc.perform(put("/api/owner/cafes/{id}/status", created.cafeId())
                        .header("X-OWNER-TOKEN", created.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cafes/near")
                        .param("lat", "37.5665")
                        .param("lng", "126.9780")
                        .param("radiusMeters", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status.crowdLevel").value("NORMAL"))
                .andExpect(jsonPath("$[0].status.party2").value("YES"))
                .andExpect(jsonPath("$[0].status.ageMinutes", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$[0].status.stale").isBoolean());
    }

    @Test
    @DisplayName("반경이 0 이하이면 근처 검색 요청은 400 에러로 실패한다")
    void near_invalidRadius_rejected() throws Exception {
        mockMvc.perform(get("/api/cafes/near")
                        .param("lat", "37.5665")
                        .param("lng", "126.9780")
                        .param("radiusMeters", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("radiusMeters must be between 1 and 10000"));
    }

    @Test
    @DisplayName("near 조회에서 limit을 주면 결과 개수가 제한된다")
    void near_limit_applies() throws Exception {
        createCafeWithToken("카페1", 37.5665, 126.9780);
        createCafeWithToken("카페2", 37.5666, 126.9781);

        mockMvc.perform(get("/api/cafes/near")
                        .param("lat", "37.5665")
                        .param("lng", "126.9780")
                        .param("radiusMeters", "2000")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("페이지네이션이 적용된 카페 목록을 반환한다")
    void list_withPagination() throws Exception {
        createCafeWithToken("페이지카페1", 37.5665, 126.9780);
        createCafeWithToken("페이지카페2", 37.5666, 126.9781);

        mockMvc.perform(get("/api/cafes")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    private CreatedCafe createCafeWithToken(String name, double lat, double lng) throws Exception {
        CafeCreateRequest req = new CafeCreateRequest(name, lat, lng, null);

        String json = mockMvc.perform(post("/api/cafes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(json).get("cafeId").asLong();
        String token = objectMapper.readTree(json).get("ownerToken").asText();
        return new CreatedCafe(id, token);
    }
}
