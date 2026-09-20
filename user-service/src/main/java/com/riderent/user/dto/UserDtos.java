package com.riderent.user.dto;

import jakarta.validation.constraints.*;

public final class UserDtos {
  private UserDtos() {}
  public record RegisterRequest(@NotBlank String name, @NotBlank @Email String email, @NotBlank String password, @NotBlank String phone) {}
  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
  public record UserResponse(Long id, String name, String email, String phone) {}
  public record LoginResponse(String token, String tokenType) {}
  public record MessageResponse(String message) {}
}
