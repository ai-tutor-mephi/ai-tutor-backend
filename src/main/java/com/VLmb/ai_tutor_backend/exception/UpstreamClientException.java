package com.VLmb.ai_tutor_backend.exception;

import org.springframework.http.HttpStatusCode;

public class UpstreamClientException extends RuntimeException {

    private final HttpStatusCode status;

    public UpstreamClientException(HttpStatusCode status, String body) {
        super("Upstream 4xx " + status + " body=" + body);
        this.status = status;
    }

    public HttpStatusCode status() {
        return status;
    }
}
