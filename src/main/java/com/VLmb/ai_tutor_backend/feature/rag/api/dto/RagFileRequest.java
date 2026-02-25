package com.VLmb.ai_tutor_backend.feature.rag.api.dto;

public record RagFileRequest(
        String fileId,
        String fileName,
        String text
) {
}
