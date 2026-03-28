package com.park302.dashboard.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache 설정 (@EnableCaching 활성화)
 * 기본 provider: ConcurrentHashMap 기반 인메모리 캐시 (별도 의존성 불필요)
 *
 * 현재 캐시 목록:
 * - agentByApiKey: api_key → Agent 변환 결과 캐싱 (매 외부 API 요청마다 발생하는 DB 조회 방지)
 *   evict: agent 수정/삭제 시 allEntries=true로 전체 무효화
 *
 * 추후 Redis 전환 시: spring-boot-starter-data-redis 추가 후
 * RedisCacheManager Bean을 선언하면 자동으로 Redis가 캐시 provider로 사용됨
 */
@EnableCaching
@Configuration
public class CacheConfig {
    // 기본 ConcurrentHashMap provider 사용 — 추가 설정 불필요
}
