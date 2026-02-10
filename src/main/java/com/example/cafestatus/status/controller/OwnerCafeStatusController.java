package com.example.cafestatus.status.controller;

import com.example.cafestatus.cafe.service.CafeService;
import com.example.cafestatus.status.dto.CafeStatusResponse;
import com.example.cafestatus.status.dto.UpdateCafeStatusRequest;
import com.example.cafestatus.status.service.CafeStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Tag(name = "Owner - Status", description = "카페 사장 전용 상태 관리 API")
@RestController
@RequestMapping("/api/owner/cafes")
public class OwnerCafeStatusController {

    private final CafeService cafeService;
    private final CafeStatusService statusService;

    public OwnerCafeStatusController(CafeService cafeService, CafeStatusService statusService) {
        this.cafeService = cafeService;
        this.statusService = statusService;
    }

    @Operation(summary = "카페 실시간 상태 업데이트")
    @PutMapping("/{cafeId}/status")
    public CafeStatusResponse update(@PathVariable Long cafeId,
                                     @AuthenticationPrincipal Long ownerId,
                                     @Valid @RequestBody UpdateCafeStatusRequest req) {
        cafeService.verifyOwnership(cafeId, ownerId);
        var saved = statusService.upsert(cafeId, req);
        return CafeStatusResponse.from(saved, Instant.now());
    }
}
