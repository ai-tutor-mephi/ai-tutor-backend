package com.VLmb.ai_tutor_backend.feature.rag.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RagQuizResponse(
        @JsonProperty("test_name")
        String testName,
        List<RagQuizQuestionResponse> questions
) {
}
