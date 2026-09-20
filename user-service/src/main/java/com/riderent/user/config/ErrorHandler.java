package com.riderent.user.config;

import java.util.Map; import org.springframework.http.HttpStatus; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.ExceptionHandler; import org.springframework.web.bind.annotation.RestControllerAdvice; import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice public class ErrorHandler { @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason() == null ? "Request failed" : e.getReason())); } @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation() { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid request")); } }
