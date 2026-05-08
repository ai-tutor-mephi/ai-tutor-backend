package com.VLmb.ai_tutor_backend.shared.error.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {


    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s с %s '%s' не найден.", translateResourceName(resourceName), translateFieldName(fieldName), fieldValue));
    }

    private static String translateResourceName(String resourceName) {
        return switch (resourceName) {
            case "Dialog" -> "Диалог";
            case "Quiz" -> "Квиз";
            case "User" -> "Пользователь";
            default -> resourceName;
        };
    }

    private static String translateFieldName(String fieldName) {
        return switch (fieldName) {
            case "id" -> "id";
            case "userName" -> "именем";
            default -> fieldName;
        };
    }
}
