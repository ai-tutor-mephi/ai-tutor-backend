package com.VLmb.ai_tutor_backend.feature.quiz.api.dto;

import java.util.List;

public record QuizScoreResponse(
        Long quizId,
        int totalQuestions,
        int correctAnswers,
        double score,
        List<QuizQuestionScoreResponse> questions
) {
}
