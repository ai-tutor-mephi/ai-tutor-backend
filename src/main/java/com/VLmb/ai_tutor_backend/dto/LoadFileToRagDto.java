package com.VLmb.ai_tutor_backend.dto;

import java.util.List;

public record LoadFileToRagDto(
        List<FileInf> content,
        Long dialogId
) {
}
