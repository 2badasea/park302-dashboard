package com.park302.dashboard.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 유틸리티
 * HS256 알고리즘 사용. secret은 최소 32자 이상 환경변수(JWT_SECRET)로 주입.
 * JJWT 0.12.x API 사용.
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        // JJWT 0.12.x: Keys.hmacShaKeyFor으로 SecretKey 생성
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * JWT 토큰 생성
     * @param username 관리자 username (subject에 저장)
     */
    public String generate(String username) {
        Date now = new Date();
        return Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirationMs))
            .signWith(secretKey)
            .compact();
    }

    /**
     * JWT 토큰 검증 및 username 추출
     * @return username (subject)
     * @throws JwtException 토큰이 유효하지 않거나 만료된 경우
     */
    public String validate(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claims.getSubject();
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     * @return 토큰 문자열. 헤더가 없거나 "Bearer "로 시작하지 않으면 null
     */
    public String extractFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}
