package com.VLmb.ai_tutor_backend.client;

import com.VLmb.ai_tutor_backend.dto.MessageRequest;
import com.VLmb.ai_tutor_backend.dto.MessageResponse;
import com.VLmb.ai_tutor_backend.dto.RagRequestDto;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class RagRestClientImpl implements RagRestClient{

    private final RestClient client;
    private final static String URL_PATH = "/rag/user-question";

    public RagRestClientImpl(RestClient ragRestClient) {
        this.client = ragRestClient;
    }

    @Override
    public MessageResponse current(RagRequestDto ragRequest) {

        return client.post()
                .uri(URL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(ragRequest)
                .retrieve()
                .body(MessageResponse.class);
    }
}