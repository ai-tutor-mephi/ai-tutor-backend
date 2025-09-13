package com.VLmb.ai_tutor_backend.dto;

public record FileResponse(
        Long fileId,
        String originalFileName,
        Long dialogId
)
{}
