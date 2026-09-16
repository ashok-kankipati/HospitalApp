package com.hospital.app.validation;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestControllerAdvice
public class ValidationErrors {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> invalid(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("message", "Please correct the highlighted fields. " + errors.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(java.util.stream.Collectors.joining(" ")), "fieldErrors", errors));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> unreadable() { return ResponseEntity.badRequest().body(Map.of("message", "Check the submitted dates, numbers and required values.")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict() { return ResponseEntity.status(409).body(Map.of("message", "A unique value is already in use, or a selected record is no longer available. Check your entries and try again.")); }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException exception) { return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", exception.getReason() == null ? "The request could not be completed." : exception.getReason())); }
}
