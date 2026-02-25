package com.VLmb.ai_tutor_backend.feature.rag.api.dto;

import java.util.List;

public record RagLoadFilesRequest(
        List<RagFileRequest> content,
        String dialogId
) {
}
