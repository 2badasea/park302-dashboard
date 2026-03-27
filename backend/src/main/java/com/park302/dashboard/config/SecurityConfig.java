package com.park302.dashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 *
 * [현재 상태 - 임시]
 * JWT 인증이 미구현된 상태로, 모든 요청을 허용(permitAll)한다.
 * CLAUDE.md "추후 구현 예정 (필수) 작업" 참고 — JWT 구현 완료 시 이 설정을 교체해야 한다.
 *
 * [JWT 구현 후 변경 사항]
 * - JwtAuthenticationFilter 등록
 * - 로그인 endpoint(/api/auth/login)만 허용, 나머지는 인증 필요
 * - CORS 설정 (프론트엔드 origin 허용)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF: REST API + JWT 방식에서는 불필요
            .csrf(AbstractHttpConfigurer::disable)
            // 세션 미사용 (JWT stateless)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // TODO: JWT 구현 후 인증 필요 경로로 교체
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
