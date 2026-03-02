package com.VLmb.ai_tutor_backend.shared.error.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class TextExtractionException extends RuntimeException {
    public TextExtractionException(String message) {
        super(message);
    }
}
