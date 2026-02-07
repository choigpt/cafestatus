package com.example.cafestatus.cafe.controller;

import com.example.cafestatus.cafe.dto.CafeOwnerTokenResponse;
import com.example.cafestatus.cafe.dto.CafeResponse;
import com.example.cafestatus.cafe.dto.CafeUpdateRequest;
import com.example.cafestatus.cafe.service.CafeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Owner - Cafe", description = "카페 사장 전용 카페 관리 API")
@RestController
@RequestMapping("/api/owner/cafes")
public class OwnerCafeController {

    private static final String OWNER_TOKEN_HEADER = "X-OWNER-TOKEN";

    private final CafeService cafeService;

    public OwnerCafeController(CafeService cafeService) {
        this.cafeService = cafeService;
    }

    @Operation(summary = "카페 정보 수정")
    @PatchMapping("/{cafeId}")
    public CafeResponse update(@PathVariable Long cafeId,
                               @RequestHeader(name = OWNER_TOKEN_HEADER) String ownerToken,
                               @RequestBody CafeUpdateRequest req) {
        return CafeResponse.from(cafeService.update(cafeId, ownerToken, req));
    }

    @Operation(summary = "카페 삭제")
    @DeleteMapping("/{cafeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long cafeId,
                       @RequestHeader(name = OWNER_TOKEN_HEADER) String ownerToken) {
        cafeService.delete(cafeId, ownerToken);
    }

    @Operation(summary = "Owner 토큰 갱신")
    @PostMapping("/{cafeId}/token/rotate")
    public CafeOwnerTokenResponse rotateToken(@PathVariable Long cafeId,
                                              @RequestHeader(name = OWNER_TOKEN_HEADER) String ownerToken) {
        return cafeService.rotateOwnerToken(cafeId, ownerToken);
    }
}
