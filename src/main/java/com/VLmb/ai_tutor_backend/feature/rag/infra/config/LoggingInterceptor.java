package com.VLmb.ai_tutor_backend.feature.rag.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

@Slf4j
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    private final boolean logPayloads;

    public LoggingInterceptor(boolean logPayloads) {

        this.logPayloads = logPayloads;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {

        long start = System.currentTimeMillis();

        log.debug("event=rag_http_request method={} uri={}", request.getMethod(), request.getURI());

//        var headers = new HttpHeaders();
//        request.getHeaders().forEach((k, v) -> {
//            if ("authorization".equalsIgnoreCase(k) || "cookie".equalsIgnoreCase(k)) {
//                headers.add(k, "***");
//            } else {
//                headers.put(k, v);
//            }
//        });

        if (logPayloads && body.length > 0) {
            log.trace("event=rag_http_request_body bytes={}", body.length);
        }

        ClientHttpResponse response = execution.execute(request, body);

        long duration = System.currentTimeMillis() - start;
        log.debug(
                "event=rag_http_response status={} uri={} duration_ms={}",
                response.getStatusCode(),
                request.getURI(),
                duration
        );

        return response;
    }
}
