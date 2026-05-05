package com.VLmb.ai_tutor_backend.feature.quiz.api.dto;

public record QuizQuestionScoreResponse(
        Long questionId,
        String selectedAnswer,
        String correctAnswer,
        boolean correct
) {
}
