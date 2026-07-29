package com.gayuth.hephaestus.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Turns bad-trace conditions into 400s with a readable message instead of 500s.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({ InvalidTraceException.class, IllegalArgumentException.class })
    public ResponseEntity<Map<String, String>> badTrace(RuntimeException ex) {
        String msg = ex.getMessage() == null ? "invalid trace" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
    }
}
