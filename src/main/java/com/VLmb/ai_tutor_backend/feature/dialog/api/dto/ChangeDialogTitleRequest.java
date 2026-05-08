package com.VLmb.ai_tutor_backend.feature.dialog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeDialogTitleRequest(
        @NotBlank(message = "Название диалога не должно быть пустым.")
        @Size(max = 255, message = "Название диалога должно быть не длиннее 255 символов.")
        String title
) {
}
