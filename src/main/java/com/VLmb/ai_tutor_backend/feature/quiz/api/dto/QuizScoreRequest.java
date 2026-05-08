package com.VLmb.ai_tutor_backend.feature.quiz.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuizScoreRequest(
        @NotNull(message = "Ответы на вопросы квиза должны быть переданы.")
        List<@Valid QuizAnswerRequest> answers
) {
}
