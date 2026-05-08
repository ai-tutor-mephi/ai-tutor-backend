package com.VLmb.ai_tutor_backend.feature.dialog.api.dto;

import jakarta.validation.constraints.Size;

public record CreateDialogRequest(
        @Size(max = 255)
        String title
) {
}
