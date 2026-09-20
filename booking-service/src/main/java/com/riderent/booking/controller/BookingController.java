package com.riderent.booking.controller;

import java.util.List;

 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
 import org.springframework.web.bind.annotation.GetMapping;
 import org.springframework.web.bind.annotation.PathVariable;
 import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.riderent.booking.dto.BookingDtos.*;
import com.riderent.booking.dto.BookingDtos.BookingRequest;
import com.riderent.booking.dto.BookingDtos.BookingResponse;
import com.riderent.booking.service.BookingService;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
@RestController @RequestMapping("/api/bookings") public class BookingController { private final BookingService service; public BookingController(BookingService service){this.service=service;} @PostMapping @ResponseStatus(HttpStatus.CREATED) public BookingResponse create(@Valid @RequestBody BookingRequest r,@Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION)String auth){return service.create(r,auth);} @GetMapping("/{id}") public BookingResponse get(@PathVariable Long id){return service.get(id);} @GetMapping("/user/{userId}") public List<BookingResponse> byUser(@PathVariable Long userId){return service.byUser(userId);} @PutMapping("/{id}/cancel") public BookingResponse cancel(@PathVariable Long id){return service.cancel(id);} }
