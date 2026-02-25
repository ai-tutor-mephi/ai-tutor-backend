package com.VLmb.ai_tutor_backend.feature.dialog.api.dto;

import java.time.OffsetDateTime;

public record DialogSummaryResponse(
        Long dialogId,
        String title,
        OffsetDateTime createdAt
) {
}
