package com.VLmb.ai_tutor_backend.dto;

import java.util.List;

public record RagRequestDto(
        List<ContentDto> content,
        Long dialogId
) {}
