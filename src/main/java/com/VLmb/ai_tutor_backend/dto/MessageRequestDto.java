package com.VLmb.ai_tutor_backend.dto;

import java.util.List;

public record MessageRequestDto(
        Long dialogId,
        List<DialogMessagesDto> dialogMessages,
        String question
) {}
