package com.VLmb.ai_tutor_backend.feature.quiz.api.dto;

import java.util.List;

public record QuizQuestionResponse(
        Long id,
        String question,
        List<String> variants
) {
}
