package com.riderent.booking.config;

import java.util.Map; import org.springframework.http.*; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice public class ErrorHandler { @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",e.getReason()==null?"Request failed":e.getReason()));} @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(){return ResponseEntity.badRequest().body(Map.of("error","Invalid request"));} }
