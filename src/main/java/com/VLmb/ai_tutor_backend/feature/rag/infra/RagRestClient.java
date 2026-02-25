package com.VLmb.ai_tutor_backend.feature.rag.infra;

import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagLoadFilesRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQueryRequest;

public interface RagRestClient {
    SendMessageResponse sendMessage(RagQueryRequest ragRequest);
    void loadFile(RagLoadFilesRequest request);
}
