package com.VLmb.ai_tutor_backend.feature.dialog.api.dto;

import com.VLmb.ai_tutor_backend.feature.dialog.domain.Message;

public record DialogMessageResponse(
        String message,
        Message.MessageRole role
) {
}
