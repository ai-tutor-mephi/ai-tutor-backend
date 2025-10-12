package com.VLmb.ai_tutor_backend.client;

import com.VLmb.ai_tutor_backend.dto.MessageResponse;
import com.VLmb.ai_tutor_backend.dto.RagRequestDto;

public interface RagRestClient {
    MessageResponse current(RagRequestDto ragRequest);
}
