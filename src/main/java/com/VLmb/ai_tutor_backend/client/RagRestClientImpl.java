package com.VLmb.ai_tutor_backend.client;

import com.VLmb.ai_tutor_backend.dto.LoadFileToRagDto;
import com.VLmb.ai_tutor_backend.dto.MessageResponse;
import com.VLmb.ai_tutor_backend.dto.MessageRequestDto;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class RagRestClientImpl implements RagRestClient{

    private final RestClient client;
    private final static String SEND_MESSAGE_PATH = "/query";
    private final static String LOAD_FILES_PATH = "/load";

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

    //Todo: Ошибки не обрабатываются, разобраться с повторными запросами
    @Override
    public void loadFile(LoadFileToRagDto request) {
        client.post()
            .uri(LOAD_FILES_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toBodilessEntity();
    }
}
