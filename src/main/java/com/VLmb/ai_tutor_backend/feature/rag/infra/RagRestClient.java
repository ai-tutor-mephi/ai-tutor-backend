package com.VLmb.ai_tutor_backend.feature.rag.infra;

import com.VLmb.ai_tutor_backend.feature.rag.api.dto.LoadFileToRagDto;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.MessageResponse;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.MessageRequestDto;

public interface RagRestClient {
    MessageResponse sendMessage(MessageRequestDto ragRequest);
    void loadFile(LoadFileToRagDto request);
}
