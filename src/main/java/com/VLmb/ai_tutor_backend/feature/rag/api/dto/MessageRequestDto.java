package com.VLmb.ai_tutor_backend.feature.rag.api.dto;

import java.util.List;

public record MessageRequestDto(
        String dialogId,
        List<DialogMessagesDto> dialogMessages,
        String question
) {}
