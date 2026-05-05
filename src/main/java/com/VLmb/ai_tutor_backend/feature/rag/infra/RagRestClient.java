package com.VLmb.ai_tutor_backend.feature.rag.infra;

import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagLoadFilesRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQuizRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQuizResponse;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQueryRequest;

public interface RagRestClient {
    SendMessageResponse sendMessage(RagQueryRequest ragRequest);
    RagQuizResponse generateQuiz(Integer questionsCount, RagQuizRequest request);
    void loadFile(RagLoadFilesRequest request);
}
