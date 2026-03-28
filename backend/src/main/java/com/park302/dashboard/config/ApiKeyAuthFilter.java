package com.park302.dashboard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.park302.dashboard.common.ResMessage;
import com.park302.dashboard.common.exception.UnauthorizedException;
import com.park302.dashboard.entity.Agent;
import com.park302.dashboard.service.AgentService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 외부 API (cali 등 연동 업체) 인증 필터
 * X-Api-Key 헤더로 업체를 식별하고 AgentPrincipal을 SecurityContext에 주입한다.
 *
 * 인증 흐름:
 * 1. X-Api-Key 헤더 추출
 * 2. AgentService.findByApiKey() 로 Agent 조회 (agentByApiKey 캐시 활용)
 * 3. 성공 → AgentPrincipal을 SecurityContext에 설정
 * 4. 실패 → 401 JSON 응답 즉시 반환 (필터 체인 중단)
 *
 * 적용 경로: /api/external/** (shouldNotFilter로 다른 경로는 건너뜀)
 */
@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Api-Key";

    private final AgentService agentService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
        throws ServletException, IOException {

        String apiKey = request.getHeader(HEADER_NAME);

        if (apiKey == null || apiKey.isBlank()) {
            sendUnauthorized(response, "X-Api-Key 헤더가 필요합니다.");
            return;
        }

        try {
            Agent agent = agentService.findByApiKey(apiKey);
            // 인증 성공: AgentPrincipal을 SecurityContext에 설정
            AgentPrincipal principal = new AgentPrincipal(agent.getId(), agent.getClientCode());
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (UnauthorizedException e) {
            sendUnauthorized(response, "유효하지 않은 API 키입니다.");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // /api/external/** 경로만 이 필터 적용
        return !request.getServletPath().startsWith("/api/external/");
    }

    /** 401 JSON 응답 전송 (필터 체인 중단) */
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ResMessage<>(-1, message, null));
    }
}
