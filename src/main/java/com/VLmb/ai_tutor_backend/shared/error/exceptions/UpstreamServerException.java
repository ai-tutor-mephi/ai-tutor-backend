package com.VLmb.ai_tutor_backend.shared.error.exceptions;

import org.springframework.http.HttpStatusCode;

public class UpstreamServerException extends RuntimeException {

    private final HttpStatusCode status;

    public UpstreamServerException(HttpStatusCode status, String body) {
        super("RAG-сервис вернул серверную ошибку " + status + ", body=" + body);
        this.status = status;
    }

    public HttpStatusCode status() {
        return status;
    }
}
