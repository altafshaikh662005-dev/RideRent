package com.riderent.user.config;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;

    public RateLimitFilter(@Value("${rate-limit.requests-per-minute:60}") int limit) {
        this.limit = limit;
    }

    @Override
        protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String client = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        Window window = windows.compute(client, (key, current) -> {
            Instant now = Instant.now();
            if (current == null || Duration.between(current.started(), now).toMinutes() >= 1) {
                return new Window(now, new AtomicInteger(1));
            }
            current.count().incrementAndGet();
            return current;
        });
        if (window.count().get() > limit) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Rate limit exceeded");
            return;
        }
        chain.doFilter(request, response);
    }

    private record Window(Instant started, AtomicInteger count) {
    }
}
