package com.VLmb.ai_tutor_backend.feature.user.api.dto;

public record ChangeUsernameResponse(
        Long userId,
        String userName
) {
}
