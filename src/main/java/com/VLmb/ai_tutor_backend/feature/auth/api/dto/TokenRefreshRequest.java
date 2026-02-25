package com.VLmb.ai_tutor_backend.feature.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(@NotBlank String refreshToken) {
}
