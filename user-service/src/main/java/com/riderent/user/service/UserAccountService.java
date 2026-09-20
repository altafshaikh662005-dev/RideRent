package com.riderent.user.service;

import com.riderent.user.dto.UserDtos.*; import com.riderent.user.entity.User; import com.riderent.user.repository.UserRepository; import com.riderent.user.security.JwtService; import org.springframework.http.HttpStatus; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service; import org.springframework.web.server.ResponseStatusException;

@Service
public class UserAccountService {
  private final UserRepository repository; private final PasswordEncoder encoder; private final JwtService jwt;
  public UserAccountService(UserRepository repository, PasswordEncoder encoder, JwtService jwt) { this.repository = repository; this.encoder = encoder; this.jwt = jwt; }
  public MessageResponse register(RegisterRequest request) { if (repository.existsByEmail(request.email())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"); User user = new User(); user.setName(request.name()); user.setEmail(request.email()); user.setPassword(encoder.encode(request.password())); user.setPhone(request.phone()); repository.save(user); return new MessageResponse("User registered successfully"); }
  public LoginResponse login(LoginRequest request) { User user = repository.findByEmail(request.email()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")); if (!encoder.matches(request.password(), user.getPassword())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"); return new LoginResponse(jwt.generate(user.getId(), user.getEmail()), "Bearer"); }
  public UserResponse get(Long id) { User user = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")); return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone()); }
}
