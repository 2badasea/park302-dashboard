package com.park302.dashboard.repository;

import com.park302.dashboard.common.enums.IsVisible;
import com.park302.dashboard.entity.AgentManager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentManagerRepository extends JpaRepository<AgentManager, Long> {

    /**
     * 업체별 노출 담당자 목록 조회 (is_visible = Y)
     * id 오름차순으로 등록 순서를 유지한다.
     */
    List<AgentManager> findByAgent_IdAndIsVisibleOrderByIdAsc(Long agentId, IsVisible isVisible);
}
