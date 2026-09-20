package com.riderent.gateway.security;

import java.nio.charset.StandardCharsets;

 import javax.crypto.SecretKey;

 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.stereotype.Service;

 import io.jsonwebtoken.JwtException;
 import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
@Service public class JwtService { private final SecretKey key; public JwtService(@Value("${jwt.secret}")String secret){key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));} public boolean valid(String token){try{Jwts.parser().verifyWith(key).build().parseSignedClaims(token);return true;}catch(JwtException|IllegalArgumentException e){return false;}} }
