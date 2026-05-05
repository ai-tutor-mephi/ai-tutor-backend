package com.VLmb.ai_tutor_backend.feature.quiz.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuizScoreRequest(
        @NotNull
        List<@Valid QuizAnswerRequest> answers
) {
}
