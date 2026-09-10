package com.edil.exception;

import com.edil.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        log.warn("EmailAlreadyExistsException: {}", ex.getMessage());
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
        log.warn("PhoneAlreadyExistsException: {}", ex.getMessage());
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
        log.warn("InvalidCredentialsException: {}", ex.getMessage());
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
        // remove this log  ( as it exposes stack trace just debagging now
        log.warn("AccountNotFoundException: {} with stack trace of {}", ex.getMessage(), ex.getStackTrace());
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
        log.warn("AccountDeactivatedException: {}, {}", ex.getMessage(), ex.getStackTrace());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.builder()
                        .errorCode("ACCOUNT_DEACTIVATED")
                        .timestamp(LocalDateTime.now())
                        .message(ex.getMessage())
                        .build()
        );
    }


    @ExceptionHandler(CampaignNotFoundException.class)
    public ResponseEntity<String> handelCampaignNotFoundException(CampaignNotFoundException campaignNotFoundException){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                campaignNotFoundException.getMessage()
        );
    }



    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Throwable ex) {
        log.warn("Unhandled Exception captured in GlobalExceptionHandler: [{}] - {}", ex.getClass().getName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .errorCode("INTERNAL_ERROR")
                        .timestamp(LocalDateTime.now())
                        .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred")
                        .build()
        );
    }


}
