package com.VLmb.ai_tutor_backend.client;

import com.VLmb.ai_tutor_backend.dto.MessageRequest;
import com.VLmb.ai_tutor_backend.dto.MessageResponse;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class RagRestClientImpl implements RagRestClient{

    private final RestClient client;
    private final static String URL_PATH = "/v1/current";

    public RagRestClientImpl(RestClient ragRestClient) {
        this.client = ragRestClient;
    }

    @Override
    public MessageResponse current(String message) {
        var request = new MessageRequest(message);

        return client.post()
                .uri(URL_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(MessageResponse.class);
    }
}
