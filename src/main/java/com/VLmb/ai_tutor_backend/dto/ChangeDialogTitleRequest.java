package com.VLmb.ai_tutor_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeDialogTitleRequest(
        @NotBlank
        @Size(max = 255)
        String title
) {
}
