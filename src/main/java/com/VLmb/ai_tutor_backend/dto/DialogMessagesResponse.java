package com.VLmb.ai_tutor_backend.dto;

import java.util.List;

public record DialogMessagesResponse(
        Long dialogId,
        List<DialogMessagesDto> dialogMessages
) {
}
