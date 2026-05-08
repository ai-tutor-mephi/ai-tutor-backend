package com.VLmb.ai_tutor_backend.feature.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeUsernameRequest(
        @NotBlank(message = "Имя пользователя не должно быть пустым.")
        @Size(min = 3, max = 50, message = "Имя пользователя должно быть от 3 до 50 символов.")
        String userName
) {
}
