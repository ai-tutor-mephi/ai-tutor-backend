package com.VLmb.ai_tutor_backend.feature.quiz.api.dto;

import jakarta.validation.constraints.NotNull;

public record QuizAnswerRequest(
        @NotNull(message = "Идентификатор вопроса квиза должен быть передан.")
        Long questionId,
        String answer
) {
}
