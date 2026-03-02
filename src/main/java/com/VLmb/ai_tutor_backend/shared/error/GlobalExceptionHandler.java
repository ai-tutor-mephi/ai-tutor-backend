package com.VLmb.ai_tutor_backend.shared.error;

import com.VLmb.ai_tutor_backend.shared.error.ErrorResponse;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.DuplicateResourceException;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.FileUploadException;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.ResourceNotFoundException;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.TextExtractionException;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.TokenRefreshException;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.UnsupportedFileExtension;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> createErrorResponse(HttpStatus status, String message, String path) {
        ErrorResponse errorResponse = new ErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path);
        return new ResponseEntity<ErrorResponse>(errorResponse, status);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(DuplicateResourceException ex, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefreshException(TokenRefreshException ex, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUploadException(FileUploadException ex, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(TextExtractionException.class)
    public ResponseEntity<ErrorResponse> handleTextExtractionException(TextExtractionException ex, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UnsupportedFileExtension.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedFileExtension(UnsupportedFileExtension ex, HttpServletRequest request) {
        return createErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI());
    }

}
