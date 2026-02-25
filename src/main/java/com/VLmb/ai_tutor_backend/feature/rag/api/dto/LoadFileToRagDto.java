package com.VLmb.ai_tutor_backend.feature.rag.api.dto;

import java.util.List;

public record LoadFileToRagDto(
        List<FileInf> content,
        String dialogId
) {
}
