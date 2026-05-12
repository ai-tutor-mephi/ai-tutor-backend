package com.VLmb.ai_tutor_backend.shared.error;

import com.VLmb.ai_tutor_backend.shared.error.exceptions.DuplicateResourceException;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.FileUploadException;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.InvalidQuizQuestionsCountException;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.ResourceNotFoundException;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.TextExtractionException;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.TokenRefreshException;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.UnsupportedFileExtension;
import com.VLmb.ai_tutor_backend.shared.error.exceptions.UpstreamServerException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SUPPORT_CONTACT_MESSAGE = "Наша поддержка в tg: @WocherZ";

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
        logException(HttpStatus.NOT_FOUND, ex, request);
        return createErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(DuplicateResourceException ex, HttpServletRequest request) {
        logException(HttpStatus.CONFLICT, ex, request);
        return createErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefreshException(TokenRefreshException ex, HttpServletRequest request) {
        logException(HttpStatus.FORBIDDEN, ex, request);
        return createErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUploadException(FileUploadException ex, HttpServletRequest request) {
        logException(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UpstreamServerException.class)
    public ResponseEntity<ErrorResponse> handleUpstreamServerException(
            UpstreamServerException ex,
            HttpServletRequest request
    ) {
        logException(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
        return createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage() + " " + SUPPORT_CONTACT_MESSAGE,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(TextExtractionException.class)
    public ResponseEntity<ErrorResponse> handleTextExtractionException(TextExtractionException ex, HttpServletRequest request) {
        logException(HttpStatus.UNPROCESSABLE_ENTITY, ex, request);
        return createErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UnsupportedFileExtension.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedFileExtension(UnsupportedFileExtension ex, HttpServletRequest request) {
        logException(HttpStatus.UNPROCESSABLE_ENTITY, ex, request);
        return createErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidQuizQuestionsCountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidQuizQuestionsCountException(
            InvalidQuizQuestionsCountException ex,
            HttpServletRequest request
    ) {
        logException(HttpStatus.BAD_REQUEST, ex, request);
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage() == null
                        ? "Запрос содержит некорректные данные."
                        : error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Запрос содержит некорректные данные.";
        }

        logException(HttpStatus.BAD_REQUEST, ex, request);
        return createErrorResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        logException(HttpStatus.BAD_REQUEST, ex, request);
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex, HttpServletRequest request) {
        logException(HttpStatus.FORBIDDEN, ex, request);
        return createErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
    }

    private void logException(HttpStatus status, Exception ex, HttpServletRequest request) {
        if (status.is5xxServerError()) {
            log.error(
                    "event=exception_handled status={} type={} path={} message={}",
                    status.value(),
                    ex.getClass().getSimpleName(),
                    request.getRequestURI(),
                    ex.getMessage(),
                    ex
            );
        } else {
            log.warn(
                    "event=exception_handled status={} type={} path={} message={}",
                    status.value(),
                    ex.getClass().getSimpleName(),
                    request.getRequestURI(),
                    ex.getMessage()
            );
        }
    }

}
