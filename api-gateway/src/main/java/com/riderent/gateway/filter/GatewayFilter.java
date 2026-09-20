package com.riderent.gateway.filter;

import java.time.Instant;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.atomic.AtomicInteger;

 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.cloud.gateway.filter.GlobalFilter;
 import org.springframework.core.Ordered;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
 import org.springframework.stereotype.Component;
 import org.springframework.web.server.ServerWebExchange;

 import com.riderent.gateway.security.JwtService;

import reactor.core.publisher.Mono;
@Component public class GatewayFilter implements GlobalFilter, Ordered { private record Window(Instant started,AtomicInteger count){} private final ConcurrentHashMap<String,Window> windows=new ConcurrentHashMap<>(); private final JwtService jwt; private final int max; public GatewayFilter(JwtService jwt,@Value("${rate-limit.requests-per-minute}")int max){this.jwt=jwt;this.max=max;}
  @Override public Mono<Void> filter(ServerWebExchange ex,org.springframework.cloud.gateway.filter.GatewayFilterChain chain){String ip=ex.getRequest().getRemoteAddress()==null?"unknown":ex.getRequest().getRemoteAddress().getAddress().getHostAddress(); Window current=windows.compute(ip,(k,w)->{if(w==null||w.started().plusSeconds(60).isBefore(Instant.now()))return new Window(Instant.now(),new AtomicInteger(1)); w.count().incrementAndGet(); return w;}); if(current.count().get()>max)return finish(ex,HttpStatus.TOO_MANY_REQUESTS); String path=ex.getRequest().getURI().getPath(); if(!path.startsWith("/api/auth/")){String h=ex.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION); if(h==null||!h.startsWith("Bearer ")||!jwt.valid(h.substring(7)))return finish(ex,HttpStatus.UNAUTHORIZED);} return chain.filter(ex); }
  private Mono<Void> finish(ServerWebExchange ex,HttpStatus status){ex.getResponse().setStatusCode(status);return ex.getResponse().setComplete();} @Override public int getOrder(){return -100;}
}
