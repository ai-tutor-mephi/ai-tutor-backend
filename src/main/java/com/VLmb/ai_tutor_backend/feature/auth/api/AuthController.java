package com.VLmb.ai_tutor_backend.feature.auth.api;

import com.VLmb.ai_tutor_backend.feature.auth.api.dto.LoginResponse;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.LoginRequest;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.RefreshTokenResponse;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.RegisterRequest;
import com.VLmb.ai_tutor_backend.feature.auth.api.dto.RefreshTokenRequest;
import com.VLmb.ai_tutor_backend.feature.auth.application.AuthService;
import com.VLmb.ai_tutor_backend.feature.auth.application.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        service.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = service.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = service.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(@AuthenticationPrincipal CustomUserDetails principal) {
        service.logout(principal.getUser().getId());

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok("User logged out successfully!");
    }
}
