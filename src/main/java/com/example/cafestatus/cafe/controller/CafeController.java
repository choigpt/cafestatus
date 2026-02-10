package com.example.cafestatus.cafe.controller;

import com.example.cafestatus.cafe.dto.*;
import com.example.cafestatus.cafe.service.CafeSearchService;
import com.example.cafestatus.cafe.service.CafeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Cafe", description = "카페 공개 API")
@RestController
@RequestMapping("/api/cafes")
public class CafeController {

    private final CafeService cafeService;
    private final CafeSearchService cafeSearchService;

    public CafeController(CafeService cafeService, CafeSearchService cafeSearchService) {
        this.cafeService = cafeService;
        this.cafeSearchService = cafeSearchService;
    }

    @Operation(summary = "카페 단건 조회")
    @GetMapping("/{id}")
    public CafeResponse get(@PathVariable Long id) {
        return CafeResponse.from(cafeService.get(id));
    }

    @Operation(summary = "카페 목록 조회 (페이지네이션)")
    @GetMapping
    public Page<CafeResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return cafeService.list(pageable).map(CafeResponse::from);
    }

    @Operation(summary = "카페 이름 검색")
    @GetMapping("/search")
    public Page<CafeResponse> search(@RequestParam String name,
                                     @PageableDefault(size = 20) Pageable pageable) {
        return cafeService.searchByName(name, pageable).map(CafeResponse::from);
    }

    @Operation(summary = "근처 카페 검색 (상태 포함)")
    @GetMapping("/near")
    public List<CafeMapItemResponse> near(@RequestParam double lat,
                                          @RequestParam double lng,
                                          @RequestParam double radiusMeters,
                                          @RequestParam(defaultValue = "50") int limit) {
        return cafeSearchService.findNearbyWithStatus(lat, lng, radiusMeters, limit);
    }
}
