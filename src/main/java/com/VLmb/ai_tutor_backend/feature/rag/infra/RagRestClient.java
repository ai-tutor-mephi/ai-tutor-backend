package com.VLmb.ai_tutor_backend.feature.rag.infra;

import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizResponse;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagLoadFilesRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQuizRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQueryRequest;

import java.util.concurrent.CompletableFuture;

public interface RagRestClient {
    SendMessageResponse sendMessage(RagQueryRequest ragRequest);
    CompletableFuture<SendMessageResponse> sendMessageAsync(RagQueryRequest ragRequest);
    QuizResponse generateQuiz(RagQuizRequest request);
    CompletableFuture<QuizResponse> generateQuizAsync(RagQuizRequest request);
    void loadFile(RagLoadFilesRequest request);
    CompletableFuture<Void> loadFileAsync(RagLoadFilesRequest request);
}
