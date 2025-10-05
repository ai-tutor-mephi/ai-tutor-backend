package com.VLmb.ai_tutor_backend.exception;

import org.springframework.http.HttpStatusCode;

public class UpstreamServerException extends RuntimeException {

    private final HttpStatusCode status;

    public UpstreamServerException(HttpStatusCode status, String body) {
        super("Upstream 5xx " + status + " body=" + body);
        this.status = status;
    }

    public HttpStatusCode status() {
        return status;
    }
}
