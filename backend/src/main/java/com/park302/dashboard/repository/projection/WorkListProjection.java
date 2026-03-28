package com.park302.dashboard.repository.projection;

import java.time.LocalDateTime;

/**
 * 문의 목록 조회용 Projection (외부 API 응답)
 * 본문(content)은 제외하여 응답 크기를 최소화한다.
 * commentCount는 JPQL 서브쿼리로 계산된다.
 */
public interface WorkListProjection {
    Long getId();
    String getTitle();
    String getCategory();
    String getPriorityByAgent();
    String getWorkStatus();
    String getCreateMemberName();
    LocalDateTime getCreatedAt();
    Long getCommentCount();
}
