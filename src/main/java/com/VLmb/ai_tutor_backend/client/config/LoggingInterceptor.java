package com.VLmb.ai_tutor_backend.client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

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

        log.debug("→ {} {}", request.getMethod(), request.getURI());

//        var headers = new HttpHeaders();
//        request.getHeaders().forEach((k, v) -> {
//            if ("authorization".equalsIgnoreCase(k) || "cookie".equalsIgnoreCase(k)) {
//                headers.add(k, "***");
//            } else {
//                headers.put(k, v);
//            }
//        });

        if (logPayloads && body.length > 0) {
            log.trace("Request headers: {}", request.getHeaders());
            log.trace("Request body ({} bytes)", body.length);
        }

        ClientHttpResponse response = execution.execute(request, body);

        long duration = System.currentTimeMillis() - start;
        log.debug("← {} {} ({} ms)", response.getStatusCode(), request.getURI(), duration);

        return response;
    }
}
