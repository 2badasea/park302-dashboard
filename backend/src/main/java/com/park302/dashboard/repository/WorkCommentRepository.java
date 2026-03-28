package com.park302.dashboard.repository;

import com.park302.dashboard.entity.WorkComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkCommentRepository extends JpaRepository<WorkComment, Long> {

    /**
     * 특정 문의의 전체 댓글 조회 (등록 시간 오름차순)
     * flat 목록으로 조회 후 서비스에서 2depth 트리 구조로 변환한다.
     */
    @Query("""
        SELECT c FROM WorkComment c
        WHERE c.work.id = :workId
        ORDER BY c.createdAt ASC
        """)
    List<WorkComment> findByWorkIdOrderByCreatedAt(@Param("workId") Long workId);
}
