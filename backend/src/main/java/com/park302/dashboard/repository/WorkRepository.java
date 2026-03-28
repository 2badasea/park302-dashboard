package com.park302.dashboard.repository;

import com.park302.dashboard.entity.Work;
import com.park302.dashboard.repository.projection.WorkListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkRepository extends JpaRepository<Work, Long> {

    /**
     * 업체별 문의 목록 조회 (외부 API용, 서버사이드 페이징)
     *
     * - agentId 스코핑: 해당 업체의 문의만 조회
     * - soft delete 제외: deletedAt IS NULL
     * - 정렬: Pageable로 처리 (기본 createdAt DESC)
     * - commentCount: work_comment 서브쿼리로 계산
     */
    @Query(
        value = """
            SELECT w.id                                                             AS id,
                   w.title                                                          AS title,
                   w.category                                                       AS category,
                   w.priorityByAgent                                                AS priorityByAgent,
                   w.workStatus                                                     AS workStatus,
                   w.createMemberName                                               AS createMemberName,
                   w.createdAt                                                      AS createdAt,
                   (SELECT COUNT(c) FROM WorkComment c WHERE c.work = w)            AS commentCount
            FROM Work w
            WHERE w.agent.id = :agentId
              AND w.deletedAt IS NULL
            """,
        countQuery = """
            SELECT COUNT(w) FROM Work w
            WHERE w.agent.id = :agentId AND w.deletedAt IS NULL
            """
    )
    Page<WorkListProjection> findByAgentId(@Param("agentId") Long agentId, Pageable pageable);
}
