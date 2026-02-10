package com.example.cafestatus.status.controller;

import com.example.cafestatus.status.dto.StatusSummary;
import com.example.cafestatus.status.service.CafeStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Status", description = "카페 상태 공개 조회 API")
@RestController
@RequestMapping("/api/cafes")
public class CafeStatusController {

    private final CafeStatusService statusService;

    public CafeStatusController(CafeStatusService statusService) {
        this.statusService = statusService;
    }

    @Operation(summary = "카페 실시간 상태 조회")
    @GetMapping("/{cafeId}/status")
    public StatusSummary get(@PathVariable Long cafeId) {
        return statusService.getStatusSummary(cafeId);
    }
}
