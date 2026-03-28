package com.park302.dashboard.config;

/**
 * 외부 API (X-Api-Key 인증) 요청에서 SecurityContext에 담기는 업체 주체 정보.
 * ApiKeyAuthFilter에서 생성하여 UsernamePasswordAuthenticationToken의 principal로 설정한다.
 * 컨트롤러에서 @AuthenticationPrincipal AgentPrincipal 로 꺼낸다.
 */
public record AgentPrincipal(Long agentId, String clientCode) {}
