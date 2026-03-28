package com.park302.dashboard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.park302.dashboard.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 *
 * 두 가지 인증 레이어:
 * 1. /api/external/** — X-Api-Key 헤더로 업체 인증 (ApiKeyAuthFilter)
 * 2. /api/** (그 외) — JWT Bearer 토큰으로 관리자 인증 (JwtAuthenticationFilter)
 * 3. /api/auth/login — 인증 불필요 (로그인 엔드포인트)
 *
 * 필터 등록 순서: ApiKeyAuthFilter → JwtAuthenticationFilter → UsernamePasswordAuthenticationFilter
 * ApiKeyAuthFilter는 /api/external/** 외 경로는 shouldNotFilter로 건너뜀.
 * JwtAuthenticationFilter는 /api/external/** 경로는 shouldNotFilter로 건너뜀.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final AgentService agentService;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF: REST API + JWT/ApiKey 방식에서 불필요
            .csrf(AbstractHttpConfigurer::disable)
            // 세션 미사용 (stateless)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 로그인 엔드포인트는 인증 불필요
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                // 외부 연동 API는 ApiKeyAuthFilter에서 인증 처리 — Spring Security 인가는 통과
                .requestMatchers("/api/external/**").permitAll()
                // 나머지 API는 JWT 인증 필요
                .requestMatchers("/api/**").authenticated()
                // 그 외 (프론트엔드 정적 리소스 등)
                .anyRequest().permitAll()
            )
            // JWT 필터: UsernamePasswordAuthenticationFilter 앞에 삽입
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil),
                UsernamePasswordAuthenticationFilter.class)
            // ApiKey 필터: JWT 필터 앞에 삽입 (/api/external/** 만 동작)
            .addFilterBefore(new ApiKeyAuthFilter(agentService, objectMapper),
                JwtAuthenticationFilter.class);

        return http.build();
    }
}
