package com.VLmb.ai_tutor_backend.client;

import com.VLmb.ai_tutor_backend.dto.FileInf;
import com.VLmb.ai_tutor_backend.dto.LoadFileToRagDto;
import com.VLmb.ai_tutor_backend.dto.MessageRequestDto;
import com.VLmb.ai_tutor_backend.dto.MessageResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Slf4j
public class RagRestClientImpl implements RagRestClient {

    private final RestClient client;
    private static final String SEND_MESSAGE_PATH = "/query";
    private static final String LOAD_FILES_PATH = "/load";

    public RagRestClientImpl(RestClient ragRestClient) {
        this.client = ragRestClient;
    }

    @Override
    public MessageResponse sendMessage(MessageRequestDto ragRequest) {

        return client.post()
                .uri(SEND_MESSAGE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(ragRequest)
                .retrieve()
                .body(MessageResponse.class);
    }

    // TODO: Add retries and richer error handling for RAG file upload if needed.
    @Override
    public void loadFile(LoadFileToRagDto request) {
        if (request.content() != null) {
            for (FileInf file : request.content()) {
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
