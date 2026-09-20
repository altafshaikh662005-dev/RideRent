package com.riderent.booking.security;

import java.io.IOException;

 import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
 import org.springframework.security.core.authority.AuthorityUtils;
 import org.springframework.security.core.context.SecurityContextHolder;
 import org.springframework.stereotype.Component;
 import org.springframework.web.filter.OncePerRequestFilter;

 import io.jsonwebtoken.JwtException;
 import jakarta.servlet.FilterChain;
 import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@Component public class JwtAuthFilter extends OncePerRequestFilter { private final JwtService jwt; public JwtAuthFilter(JwtService jwt){this.jwt=jwt;} @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{String h=req.getHeader("Authorization"); if(h!=null&&h.startsWith("Bearer "))try{var c=jwt.parse(h.substring(7)); SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(c.getSubject(),null,AuthorityUtils.NO_AUTHORITIES));}catch(JwtException|IllegalArgumentException ignored){} chain.doFilter(req,res);} }
