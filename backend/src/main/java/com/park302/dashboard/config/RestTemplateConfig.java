package com.park302.dashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate Bean 설정
 * webhook 발송(WorkCommentService.sendWebhook)에 사용.
 *
 * TODO: 비동기 처리가 필요해지면 spring-boot-starter-webflux 의존성 추가 후
 *       WebClient Bean으로 교체. RestTemplate은 Spring 유지보수 모드이므로
 *       장기적으로는 WebClient 전환 권장.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
