package com.riderent.user.config;

import com.riderent.user.security.JwtAuthFilter;
import org.springframework.context.annotation.*; import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.config.http.SessionCreationPolicy; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.security.web.*; import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
  @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwt, RateLimitFilter rateLimit) throws Exception { return http.csrf(csrf -> csrf.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a -> a.requestMatchers("/api/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/health", "/error").permitAll().anyRequest().authenticated()).addFilterBefore(rateLimit, UsernamePasswordAuthenticationFilter.class).addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).build(); }
}
