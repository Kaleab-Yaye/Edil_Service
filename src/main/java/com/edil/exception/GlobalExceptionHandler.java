package com.edil.exception;

import com.edil.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .errorCode("EMAIL_ALREADY_EXISTS")
                        .timestamp(LocalDateTime.now())
                        .message(ex.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(PhoneAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlePhoneAlreadyExistsException(PhoneAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .errorCode("PHONE_ALREADY_EXISTS")
                        .timestamp(LocalDateTime.now())
                        .message(ex.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .errorCode("INVALID_CREDENTIALS")
                        .timestamp(LocalDateTime.now())
                        .message(ex.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFoundException(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder()
                        .errorCode("ACCOUNT_NOT_FOUND")
                        .timestamp(LocalDateTime.now())
                        .message(ex.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(AccountDeactivatedException.class)
    public ResponseEntity<ErrorResponse> handleAccountDeactivatedException(AccountDeactivatedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.builder()
                        .errorCode("ACCOUNT_DEACTIVATED")
                        .timestamp(LocalDateTime.now())
                        .message(ex.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("Validation error");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .errorCode("VALIDATION_ERROR")
                        .timestamp(LocalDateTime.now())
                        .message(message)
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .errorCode("INTERNAL_ERROR")
                        .timestamp(LocalDateTime.now())
                        .message("An unexpected error occurred")
                        .build()
        );
    }
}
