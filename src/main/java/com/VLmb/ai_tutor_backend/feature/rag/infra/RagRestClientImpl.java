package com.VLmb.ai_tutor_backend.feature.rag.infra;

import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagFileRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagLoadFilesRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQuizRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQueryRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import com.VLmb.ai_tutor_backend.feature.quiz.api.dto.QuizResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RagRestClientImpl implements RagRestClient {

    public static final int LOAD_FILE_TIMEOUT = 30;
    private final RestClient client;
    private final TaskExecutor ragExecutor;
    private static final String SEND_MESSAGE_PATH = "/query";
    private static final String GENERATE_TEST_PATH = "/tests";
    private static final String LOAD_FILES_PATH = "/load";

    public RagRestClientImpl(RestClient ragRestClient, @Qualifier("ragExecutor") TaskExecutor ragExecutor) {
        this.client = ragRestClient;
        this.ragExecutor = ragExecutor;
    }

    @Override
    public SendMessageResponse sendMessage(RagQueryRequest ragRequest) {
        log.debug(
                "event=rag_query_request dialog_id={} message_count={} question_len={}",
                ragRequest.dialogId(),
                ragRequest.dialogMessages() == null ? 0 : ragRequest.dialogMessages().size(),
                ragRequest.question() == null ? 0 : ragRequest.question().length()
        );

        return client.post()
                .uri(SEND_MESSAGE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(ragRequest)
                .retrieve()
                .body(SendMessageResponse.class);
    }

    @Override
    public CompletableFuture<SendMessageResponse> sendMessageAsync(RagQueryRequest ragRequest) {
        return CompletableFuture.supplyAsync(() -> sendMessage(ragRequest), ragExecutor);
    }

    @Override
    public QuizResponse generateQuiz(RagQuizRequest request) {
        log.info(
                "event=rag_generate_quiz_request dialog_id={} message_count={}",
                request.dialogId(),
                request.dialogMessages() == null ? 0 : request.dialogMessages().size()
        );

        return client.post()
                .uri(GENERATE_TEST_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(QuizResponse.class);
    }

    @Override
    public CompletableFuture<QuizResponse> generateQuizAsync(RagQuizRequest request) {
        return CompletableFuture.supplyAsync(() -> generateQuiz(request), ragExecutor);
    }

    // TODO: Add retries and richer error handling for RAG file upload if needed.
    @Override
    public void loadFile(RagLoadFilesRequest request) {
        int fileCount = request.content() == null ? 0 : request.content().size();
        int totalTextLen = 0;
        if (request.content() != null) {
            for (RagFileRequest file : request.content()) {
                if (file.text() != null) {
                    totalTextLen += file.text().length();
                }
            }
        }
        log.info(
                "event=rag_load_files dialog_id={} file_count={} total_text_len={}",
                request.dialogId(),
                fileCount,
                totalTextLen
        );

        client.post()
                .uri(LOAD_FILES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public CompletableFuture<Void> loadFileAsync(RagLoadFilesRequest request) {
        return CompletableFuture.runAsync(() -> loadFile(request), ragExecutor)
                .orTimeout(LOAD_FILE_TIMEOUT, TimeUnit.SECONDS);
    }
}
