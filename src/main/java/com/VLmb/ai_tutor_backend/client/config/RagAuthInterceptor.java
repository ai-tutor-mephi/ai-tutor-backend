package com.VLmb.ai_tutor_backend.client.config;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class RagAuthInterceptor implements ClientHttpRequestInterceptor {

    private final RagProperties ragProperties;

    public RagAuthInterceptor(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
//        if (ragProperties.apiKey() != null && !ragProperties.apiKey().isBlank()) {
//            request.getHeaders().add("X-API-Key", ragProperties.apiKey());
//        }
        return execution.execute(request, body);
    }

}
