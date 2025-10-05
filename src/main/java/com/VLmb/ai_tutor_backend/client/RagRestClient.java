package com.VLmb.ai_tutor_backend.client;

import com.VLmb.ai_tutor_backend.dto.MessageResponse;

public interface RagRestClient {
    MessageResponse current(String message);
}
