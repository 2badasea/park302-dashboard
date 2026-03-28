package com.park302.dashboard.config;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터
 * Authorization: Bearer {token} 헤더에서 토큰을 추출하여 검증하고 SecurityContext에 설정한다.
 * 토큰이 없거나 유효하지 않으면 인증 없이 통과 (403은 SecurityConfig 인가 단계에서 처리).
 *
 * /api/external/** 경로는 ApiKeyAuthFilter가 처리하므로 이 필터에서 건너뛴다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
        throws ServletException, IOException {

        String token = jwtUtil.extractFromHeader(request.getHeader("Authorization"));

        if (token != null) {
            try {
                String username = jwtUtil.validate(token);
                // 인증 성공: SecurityContext에 주체 설정 (단일 관리자이므로 별도 authorities 불필요)
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException e) {
                // 유효하지 않은 토큰: SecurityContext 미설정 → SecurityConfig 인가 단계에서 차단
                log.debug("Invalid JWT token: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // /api/external/** 는 ApiKeyAuthFilter 담당, 이 필터 건너뜀
        return request.getServletPath().startsWith("/api/external/");
    }
}
