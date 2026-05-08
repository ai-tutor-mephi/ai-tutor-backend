package com.VLmb.ai_tutor_backend.feature.rag.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagFileRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagLoadFilesRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQuizRequest;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQuizResponse;
import com.VLmb.ai_tutor_backend.feature.rag.api.dto.RagQueryRequest;
import com.VLmb.ai_tutor_backend.feature.dialog.api.dto.SendMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Slf4j
public class RagRestClientImpl implements RagRestClient {

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private static final String SEND_MESSAGE_PATH = "/query";
    private static final String GENERATE_TEST_PATH = "/tests";
    private static final String LOAD_FILES_PATH = "/load";

    public RagRestClientImpl(RestClient ragRestClient, ObjectMapper objectMapper) {
        this.client = ragRestClient;
        this.objectMapper = objectMapper;
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
    public RagQuizResponse generateQuiz(Integer questionsCount, RagQuizRequest request) {
        log.info(
                "event=rag_generate_quiz_request dialog_id={} message_count={} questions_count={}",
                request.dialogId(),
                request.dialogMessages() == null ? 0 : request.dialogMessages().size(),
                questionsCount
        );

        return client.post()
                .uri(GENERATE_TEST_PATH + "/{questionsCount}", questionsCount)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(RagQuizResponse.class);
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
        log.info("event=rag_load_files_request_body body={}", toJson(request));

        client.post()
                .uri(LOAD_FILES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("event=rag_request_body_serialization_failed message={}", ex.getMessage());
            return "<unserializable>";
        }
    }
}
