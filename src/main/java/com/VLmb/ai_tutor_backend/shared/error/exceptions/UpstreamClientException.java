package com.VLmb.ai_tutor_backend.shared.error.exceptions;

import org.springframework.http.HttpStatusCode;

public class UpstreamClientException extends RuntimeException {

    private final HttpStatusCode status;

    public UpstreamClientException(HttpStatusCode status, String body) {
        super("RAG-сервис вернул клиентскую ошибку " + status + ", body=" + body);
        this.status = status;
    }

    public HttpStatusCode status() {
        return status;
    }
}
