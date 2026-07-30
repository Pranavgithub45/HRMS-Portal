package com.billdesk.hrmsportal.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Extends ResponseEntityExceptionHandler so Spring MVC's own exceptions (unreadable body,
 * unsupported Content-Type, unknown path, bad path variable type) keep their real 4xx status
 * instead of being swallowed by the catch-all below and reported as 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Field-level bean validation failures — one entry per rejected field. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    /** Renders every framework-handled exception in our {"error": "..."} shape, status preserved. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        log.warn("Request rejected [{}]: {}", statusCode, ex.getMessage());

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(statusCode);
        if (headers != null) {
            builder.headers(headers);   // preserve Allow / Accept hints on 405 and 415
        }
        return builder.body(Map.of("error", reasonFor(ex)));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason() != null ? ex.getReason() : "Error"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Something went wrong"));
    }

    /** Actionable messages for the two mistakes that used to surface as a bare 500. */
    private String reasonFor(Exception ex) {
        if (ex instanceof HttpMessageNotReadableException) {
            return "Malformed or missing JSON request body";
        }
        if (ex instanceof HttpMediaTypeNotSupportedException) {
            return "Unsupported Content-Type. Send Content-Type: application/json";
        }
        return ex.getMessage() != null ? ex.getMessage() : "Request could not be processed";
    }
}
