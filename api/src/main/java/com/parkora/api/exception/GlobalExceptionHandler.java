package com.parkora.api.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.parkora.api.dto.common.ApiResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex) {
        logger.warn("Unsupported media type: {}", ex.getContentType());

        String message = String.format(
                "Content-Type '%s' is not supported. Please use 'application/json' for request body.",
                ex.getContentType()
        );

        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("contentType", ex.getContentType() != null ? ex.getContentType().toString() : "unknown");
        errorDetails.put("supportedTypes", ex.getSupportedMediaTypes().toString());

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .message(message)
                .errorCode(415)
                .status("UNSUPPORTED_MEDIA_TYPE")
                .data(errorDetails)
                .build();

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        logger.warn("Validation failed: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .message("Validation failed")
                .errorCode(400)
                .status("BAD_REQUEST")
                .data(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        logger.warn("Constraint violation: {}", ex.getMessage());

        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .message("Validation failed")
                .errorCode(400)
                .status("BAD_REQUEST")
                .data(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        logger.warn("Message not readable: {}", ex.getMessage());

        String message = "Invalid request body. Please ensure the request body is valid JSON.";
        if (ex.getMessage() != null && ex.getMessage().contains("JSON")) {
            message = "Invalid JSON format in request body.";
        }

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .message(message)
                .errorCode(400)
                .status("BAD_REQUEST")
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        logger.warn("Argument type mismatch: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .message(String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName()))
                .errorCode(400)
                .status("BAD_REQUEST")
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred", ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .message("An unexpected error occurred: " + ex.getMessage())
                .errorCode(500)
                .status("INTERNAL_ERROR")
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
