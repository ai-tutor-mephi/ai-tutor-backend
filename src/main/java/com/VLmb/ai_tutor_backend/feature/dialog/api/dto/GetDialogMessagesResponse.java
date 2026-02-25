package com.VLmb.ai_tutor_backend.feature.dialog.api.dto;

import java.util.List;

public record GetDialogMessagesResponse(
        Long dialogId,
        List<DialogMessageResponse> dialogMessages
) {
}
