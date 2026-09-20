package com.riderent.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
  @Column(nullable = false) private String name;
  @Column(nullable = false, unique = true) private String email;
  @Column(nullable = false) private String password;
  @Column(nullable = false) private String phone;
  @PrePersist void setCreatedAt() { if (createdAt == null) createdAt = LocalDateTime.now(); }
  public Long getId() { return id; } public String getName() { return name; } public void setName(String v) { name = v; }
  public String getEmail() { return email; } public void setEmail(String v) { email = v; } public String getPassword() { return password; } public void setPassword(String v) { password = v; }
  public String getPhone() { return phone; } public void setPhone(String v) { phone = v; }
}
