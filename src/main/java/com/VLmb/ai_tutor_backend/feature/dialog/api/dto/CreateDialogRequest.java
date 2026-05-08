package com.VLmb.ai_tutor_backend.feature.dialog.api.dto;

import jakarta.validation.constraints.Size;

public record CreateDialogRequest(
        @Size(max = 255, message = "Название диалога должно быть не длиннее 255 символов.")
        String title
) {
}
