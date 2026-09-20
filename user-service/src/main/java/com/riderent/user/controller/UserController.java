package com.riderent.user.controller;

import com.riderent.user.dto.UserDtos.*; import com.riderent.user.service.UserAccountService; import jakarta.validation.Valid; import org.springframework.http.HttpStatus; import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class UserController {
  private final UserAccountService service; public UserController(UserAccountService service) { this.service = service; }
  @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED) public MessageResponse register(@Valid @RequestBody RegisterRequest request) { return service.register(request); }
  @PostMapping("/auth/login") public LoginResponse login(@Valid @RequestBody LoginRequest request) { return service.login(request); }
  @GetMapping("/users/{id}") public UserResponse get(@PathVariable Long id) { return service.get(id); }
}
