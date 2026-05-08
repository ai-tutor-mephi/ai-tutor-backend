package com.VLmb.ai_tutor_backend.shared.error.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s с %s '%s' уже существует.", translateResourceName(resourceName), translateFieldName(fieldName), fieldValue));
    }

    private static String translateResourceName(String resourceName) {
        return switch (resourceName) {
            case "User" -> "Пользователь";
            default -> resourceName;
        };
    }

    private static String translateFieldName(String fieldName) {
        return switch (fieldName) {
            case "userName" -> "именем";
            case "email" -> "email";
            default -> fieldName;
        };
    }
}
