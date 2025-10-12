package com.VLmb.ai_tutor_backend.dto;

import java.util.List;
import java.util.Map;

public record RagRequestDto(
        Long dialogId,
        List<DialogMessagesDto> dialogMessages,
        String question
) {}
