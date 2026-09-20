package com.riderent.user.repository;

import com.riderent.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> { Optional<User> findByEmail(String email); boolean existsByEmail(String email); }
