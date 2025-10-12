package com.VLmb.ai_tutor_backend.dto;

import com.VLmb.ai_tutor_backend.entity.Message;

public record DialogMessagesDto(
        String message,
        Message.MessageRole role
) {
}
