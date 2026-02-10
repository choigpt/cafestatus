package com.example.cafestatus.auth.controller;

import com.example.cafestatus.auth.dto.LoginRequest;
import com.example.cafestatus.auth.dto.RefreshRequest;
import com.example.cafestatus.auth.dto.SignUpRequest;
import com.example.cafestatus.auth.dto.TokenResponse;
import com.example.cafestatus.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "사장 인증 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "사장 회원가입")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    @Operation(summary = "사장 로그인")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "토큰 갱신")
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Long ownerId) {
        authService.logout(ownerId);
    }
}
