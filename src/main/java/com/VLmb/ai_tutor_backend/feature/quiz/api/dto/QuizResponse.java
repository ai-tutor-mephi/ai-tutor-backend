package com.VLmb.ai_tutor_backend.feature.quiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record QuizResponse(
        Long id,
        @JsonProperty("test_name")
        String testName,
        List<QuizQuestionResponse> questions
) {
}
