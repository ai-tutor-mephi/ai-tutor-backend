package com.VLmb.ai_tutor_backend.feature.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Имя пользователя не должно быть пустым.")
        @Size(min = 3, max = 50, message = "Имя пользователя должно быть от 3 до 50 символов.")
        String userName,

        @NotBlank(message = "Пароль не должен быть пустым.")
        @Size(min = 8, max = 100, message = "Пароль должен быть от 8 до 100 символов.")
        String password
) {}
