package com.VLmb.ai_tutor_backend.feature.dialog.api.dto;

import com.VLmb.ai_tutor_backend.feature.dialog.domain.Message;

public record DialogMessagesDto(
        String message,
        Message.MessageRole role
) {
}
