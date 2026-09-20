package com.riderent.gateway.config;

import org.springframework.context.annotation.*; import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity; import org.springframework.security.config.web.server.ServerHttpSecurity; import org.springframework.security.web.server.SecurityWebFilterChain;
@Configuration @EnableWebFluxSecurity public class SecurityConfig { @Bean SecurityWebFilterChain chain(ServerHttpSecurity http){return http.csrf(ServerHttpSecurity.CsrfSpec::disable).authorizeExchange(a->a.anyExchange().permitAll()).build();} }
