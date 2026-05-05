package com.VLmb.ai_tutor_backend.feature.rag.api.dto;

import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.DialogMessageResponse;

import java.util.List;

public record RagQuizRequest(
        String dialogId,
        List<DialogMessageResponse> dialogMessages
) {
}
