package com.riderent.user.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key; private final long expiration;
  public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expiration) { key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.expiration = expiration; }
  public String generate(Long userId, String email) { Date now = new Date(); return Jwts.builder().subject(String.valueOf(userId)).claim("email", email).issuedAt(now).expiration(new Date(now.getTime() + expiration)).signWith(key).compact(); }
  public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
