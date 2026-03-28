package com.park302.dashboard.repository;

import com.park302.dashboard.common.enums.IsVisible;
import com.park302.dashboard.entity.Agent;
import com.park302.dashboard.repository.projection.AgentListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    /**
     * 업체 목록 조회 (서버사이드 페이지네이션)
     *
     * 검색 조건:
     * - keyword가 빈 문자열이면 전체 조회
     * - keyword가 있으면 searchType에 따라 해당 필드에 LIKE 검색
     *   ALL: 업체명, clientCode, 주소, 연락처, 이메일 전체
     *   NAME / CLIENT_CODE / ADDRESS / TEL / EMAIL: 해당 필드만
     *
     * isVisible = Y인 업체만 조회한다.
     * NULL 컬럼에 LIKE를 적용하면 NULL을 반환하므로 자연스럽게 매칭에서 제외된다.
     */
    @Query(
        value = """
            SELECT a.id        AS id,
                   a.name      AS name,
                   a.clientCode AS clientCode,
                   a.address   AS address,
                   a.contactTel AS contactTel,
                   a.contactEmail AS contactEmail
            FROM Agent a
            WHERE a.isVisible = :visible
              AND (:keyword = '' OR (
                    (:searchType = 'ALL'         AND (a.name LIKE %:keyword% OR a.clientCode LIKE %:keyword% OR a.address LIKE %:keyword% OR a.contactTel LIKE %:keyword% OR a.contactEmail LIKE %:keyword%))
                 OR (:searchType = 'NAME'        AND a.name LIKE %:keyword%)
                 OR (:searchType = 'CLIENT_CODE' AND a.clientCode LIKE %:keyword%)
                 OR (:searchType = 'ADDRESS'     AND a.address LIKE %:keyword%)
                 OR (:searchType = 'TEL'         AND a.contactTel LIKE %:keyword%)
                 OR (:searchType = 'EMAIL'       AND a.contactEmail LIKE %:keyword%)
              ))
            """,
        countQuery = """
            SELECT COUNT(a)
            FROM Agent a
            WHERE a.isVisible = :visible
              AND (:keyword = '' OR (
                    (:searchType = 'ALL'         AND (a.name LIKE %:keyword% OR a.clientCode LIKE %:keyword% OR a.address LIKE %:keyword% OR a.contactTel LIKE %:keyword% OR a.contactEmail LIKE %:keyword%))
                 OR (:searchType = 'NAME'        AND a.name LIKE %:keyword%)
                 OR (:searchType = 'CLIENT_CODE' AND a.clientCode LIKE %:keyword%)
                 OR (:searchType = 'ADDRESS'     AND a.address LIKE %:keyword%)
                 OR (:searchType = 'TEL'         AND a.contactTel LIKE %:keyword%)
                 OR (:searchType = 'EMAIL'       AND a.contactEmail LIKE %:keyword%)
              ))
            """
    )
    Page<AgentListProjection> findAgentList(
        @Param("visible") IsVisible visible,
        @Param("searchType") String searchType,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    /**
     * api_key로 업체 조회 — ApiKeyAuthFilter에서 호출.
     * AgentService.findByApiKey()에서 @Cacheable로 캐싱됨.
     */
    Optional<Agent> findByApiKey(String apiKey);
}
