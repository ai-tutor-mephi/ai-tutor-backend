package com.VLmb.ai_tutor_backend.feature.quiz.api.dto;

import jakarta.validation.constraints.NotNull;

public record QuizAnswerRequest(
        @NotNull
        Long questionId,
        String answer
) {
}
