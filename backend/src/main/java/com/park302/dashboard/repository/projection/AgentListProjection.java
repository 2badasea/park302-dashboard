package com.park302.dashboard.repository.projection;

/**
 * 업체 목록 조회용 Projection
 * AgentRepository의 JPQL 쿼리 결과를 이 인터페이스로 매핑한다.
 * 목록에 필요한 컬럼만 선택하여 불필요한 데이터 전송을 줄인다.
 */
public interface AgentListProjection {
    Long getId();
    String getName();
    String getClientCode();
    String getAddress();
    String getContactTel();
    String getContactEmail();
}
