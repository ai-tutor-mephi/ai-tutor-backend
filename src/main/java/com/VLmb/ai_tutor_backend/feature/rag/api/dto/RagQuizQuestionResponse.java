package com.VLmb.ai_tutor_backend.feature.rag.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RagQuizQuestionResponse(
        String question,
        List<String> variants,
        @JsonProperty("gold_answer")
        String goldAnswer
) {
}
