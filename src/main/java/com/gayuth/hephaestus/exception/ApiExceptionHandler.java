package com.gayuth.hephaestus.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns bad input into 400s with a readable message, and everything else into a
 * logged, generic 500.
 *
 * <p>
 * This used to catch {@code IllegalArgumentException} wholesale, which meant
 * ANY internal IAE thrown anywhere in Spring, Jackson, or the JDK came back to
 * the caller as a 400 with its raw internal message in the body. Only
 * {@link InvalidTraceException} - which the parser and builder now throw
 * deliberately for caller-fixable problems - is treated as a client error. The
 * catch-all is the other half: without it, an unhandled exception produced a
 * whitelabel error page and nothing in the logs tied it to a request.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidTraceException.class)
    public ResponseEntity<Map<String, String>> badTrace(InvalidTraceException ex) {
        String msg = ex.getMessage() == null ? "invalid trace" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpected(Exception ex) {
        // Log the detail, return none of it.
        log.error("unhandled exception serving request", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Something went wrong handling that request."));
    }
}
