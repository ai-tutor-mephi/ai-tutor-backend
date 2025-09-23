package com.VLmb.ai_tutor_backend.dto;

import java.time.OffsetDateTime;

public record DialogInfo(
        Long dialogId,
        String title,
        OffsetDateTime createdAt
) {
}
