package com.medibook.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseGlobalExceptionHandler<T> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(BaseGlobalExceptionHandler.class);

    private final ErrorResponseFactory<T> errorResponseFactory;

    protected BaseGlobalExceptionHandler(
            ErrorResponseFactory<T> errorResponseFactory) {
        this.errorResponseFactory = errorResponseFactory;
    }

    @ExceptionHandler(BaseResourceNotFoundException.class)
    public ResponseEntity<T> handleNotFound(
            BaseResourceNotFoundException ex,
            HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Not Found",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(BaseDuplicateResourceException.class)
    public ResponseEntity<T> handleDuplicate(
            BaseDuplicateResourceException ex,
            HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Conflict",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(BaseBadRequestException.class)
    public ResponseEntity<T> handleBadRequest(
            BaseBadRequestException ex,
            HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(BaseUnauthorizedException.class)
    public ResponseEntity<T> handleUnauthorized(
            BaseUnauthorizedException ex,
            HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(BaseForbiddenException.class)
    public ResponseEntity<T> handleForbidden(
            BaseForbiddenException ex,
            HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("errors", fieldErrors);
        response.put("path", request.getRequestURI());
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<T> handleGeneral(
            Exception ex,
            HttpServletRequest request) {

        LOGGER.error("Unexpected error: {}", ex.getMessage(), ex);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Something went wrong. Please try again later.",
                request
        );
    }

    private ResponseEntity<T> buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request) {

        T errorResponse = errorResponseFactory.create(
                status.value(),
                error,
                message,
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }
}
