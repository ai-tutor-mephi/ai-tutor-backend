package com.VLmb.ai_tutor_backend.client;

import com.VLmb.ai_tutor_backend.dto.LoadFileToRagDto;
import com.VLmb.ai_tutor_backend.dto.MessageResponse;
import com.VLmb.ai_tutor_backend.dto.MessageRequestDto;

public interface RagRestClient {
    MessageResponse sendMessage(MessageRequestDto ragRequest);
    void loadFile(LoadFileToRagDto request);
}
