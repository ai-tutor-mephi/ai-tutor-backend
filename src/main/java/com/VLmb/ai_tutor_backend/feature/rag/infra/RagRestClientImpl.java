package com.VLmb.ai_tutor_backend.feature.rag.infra;

import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagFileRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagLoadFilesRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQueryRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class RagRestClientImpl implements RagRestClient {

    private final RestClient client;
    private final TaskExecutor ragExecutor;
    private static final String SEND_MESSAGE_PATH = "/query";
    private static final String LOAD_FILES_PATH = "/load";

    public RagRestClientImpl(RestClient ragRestClient, @Qualifier("ragExecutor") TaskExecutor ragExecutor) {
        this.client = ragRestClient;
        this.ragExecutor = ragExecutor;
    }

    @Override
    public SendMessageResponse sendMessage(RagQueryRequest ragRequest) {

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

    // TODO: Add retries and richer error handling for RAG file upload if needed.
    @Override
    public void loadFile(RagLoadFilesRequest request) {
        if (request.content() != null) {
            for (RagFileRequest file : request.content()) {
                log.info("Sending file to RAG: fileId={}, fileName={}, content={}", file.fileId(), file.fileName(), file.text());
            }
        } else {
            log.info("Sending file to RAG with empty content for dialog {}", request.dialogId());
        }

        client.post()
                .uri(LOAD_FILES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
