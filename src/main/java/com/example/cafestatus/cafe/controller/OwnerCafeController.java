package com.example.cafestatus.cafe.controller;

import com.example.cafestatus.cafe.dto.CafeCreateRequest;
import com.example.cafestatus.cafe.dto.CafeResponse;
import com.example.cafestatus.cafe.dto.CafeUpdateRequest;
import com.example.cafestatus.cafe.entity.Cafe;
import com.example.cafestatus.cafe.service.CafeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Owner - Cafe", description = "카페 사장 전용 카페 관리 API")
@RestController
@RequestMapping("/api/owner/cafes")
public class OwnerCafeController {

    private final CafeService cafeService;

    public OwnerCafeController(CafeService cafeService) {
        this.cafeService = cafeService;
    }

    @Operation(summary = "카페 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CafeResponse create(@AuthenticationPrincipal Long ownerId,
                               @Valid @RequestBody CafeCreateRequest req) {
        Cafe cafe = cafeService.create(req, ownerId);
        return CafeResponse.from(cafe);
    }

    @Operation(summary = "내 카페 목록 조회 (페이지네이션)")
    @GetMapping
    public Page<CafeResponse> myCafes(@AuthenticationPrincipal Long ownerId,
                                      @PageableDefault(size = 20) Pageable pageable) {
        return cafeService.findByOwner(ownerId, pageable).map(CafeResponse::from);
    }

    @Operation(summary = "카페 정보 수정")
    @PatchMapping("/{cafeId}")
    public CafeResponse update(@PathVariable Long cafeId,
                               @AuthenticationPrincipal Long ownerId,
                               @RequestBody CafeUpdateRequest req) {
        return CafeResponse.from(cafeService.update(cafeId, ownerId, req));
    }

    @Operation(summary = "카페 삭제")
    @DeleteMapping("/{cafeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long cafeId,
                       @AuthenticationPrincipal Long ownerId) {
        cafeService.delete(cafeId, ownerId);
    }
}
