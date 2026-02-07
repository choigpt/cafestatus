package com.example.cafestatus.status.controller;

import com.example.cafestatus.status.service.StatusSseRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.List;

@Tag(name = "SSE", description = "실시간 상태 스트리밍")
@RestController
@RequestMapping("/api/cafes/status")
public class StatusSseController {

    private static final Logger log = LoggerFactory.getLogger(StatusSseController.class);

    private final StatusSseRegistry registry;

    public StatusSseController(StatusSseRegistry registry) {
        this.registry = registry;
    }

    @Operation(summary = "카페 상태 SSE 스트림 구독")
    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam String cafeIds) {
        log.info("SSE 스트림 요청: cafeIds={}", cafeIds);

        List<Long> ids;
        try {
            ids = Arrays.stream(cafeIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(Long::valueOf)
                    .distinct()
                    .toList();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("cafeIds must be comma-separated numbers");
        }

        if (ids.isEmpty()) {
            throw new IllegalArgumentException("cafeIds is required");
        }

        return registry.subscribe(ids);
    }
}
