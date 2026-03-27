package com.park302.dashboard.service;

import com.park302.dashboard.common.PageData;
import com.park302.dashboard.common.ResMessage;
import com.park302.dashboard.common.enums.IsVisible;
import com.park302.dashboard.dto.AgentDTO;
import com.park302.dashboard.entity.Agent;
import com.park302.dashboard.mapper.AgentMapper;
import com.park302.dashboard.repository.AgentRepository;
import com.park302.dashboard.repository.projection.AgentListProjection;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 업체관리 서비스
 * 모든 조회는 readOnly 트랜잭션으로 처리하여 불필요한 dirty-checking을 방지한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentService {

    private final AgentRepository agentRepository;
    private final AgentMapper agentMapper;

    /**
     * 업체 목록 조회 (서버사이드 페이지네이션)
     * isVisible = Y인 업체만 조회한다.
     * keyword가 없으면 전체 조회, 있으면 searchType에 따라 해당 필드 LIKE 검색
     */
    public ResMessage<PageData<AgentDTO.ListItem>> getList(AgentDTO.ListRequest req) {
        // keyword 정규화: null 또는 공백은 빈 문자열로 처리 (JPQL에서 '' 조건으로 전체 조회)
        String keyword = StringUtils.hasText(req.getKeyword()) ? req.getKeyword().trim() : "";

        // Spring Data의 PageRequest (1-based → 0-based 변환)
        PageRequest pageable = PageRequest.of(
            req.getPage() - 1,
            req.getPerPage(),
            Sort.by(Sort.Direction.DESC, "id")
        );

        Page<AgentListProjection> page = agentRepository.findAgentList(
            IsVisible.Y,
            req.getSearchType(),
            keyword,
            pageable
        );

        List<AgentDTO.ListItem> content = page.getContent().stream()
            .map(agentMapper::toListItem)
            .toList();

        PageData<AgentDTO.ListItem> pageData = PageData.<AgentDTO.ListItem>builder()
            .content(content)
            .totalCount(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .currentPage(req.getPage())
            .perPage(req.getPerPage())
            .build();

        return new ResMessage<>(1, "", pageData);
    }

    /**
     * 업체 상세 조회 (수정 모달 진입 시 사용)
     * isVisible 무관하게 id로 조회한다 (숨김 처리된 업체도 직접 조회 가능).
     */
    public ResMessage<AgentDTO.DetailResponse> getDetail(Long id) {
        Agent agent = agentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("업체를 찾을 수 없습니다. id=" + id));
        return new ResMessage<>(1, "", agentMapper.toDetailResponse(agent));
    }

    /**
     * 업체 등록
     * client_code 중복 시 DB UNIQUE 제약이 발생하며 GlobalApiExceptionHandler가 처리한다.
     */
    @Transactional
    public ResMessage<AgentDTO.DetailResponse> create(AgentDTO.CreateRequest req) {
        Agent agent = Agent.create(req);
        agentRepository.save(agent);
        return new ResMessage<>(1, "업체가 등록되었습니다.", agentMapper.toDetailResponse(agent));
    }

    /**
     * 업체 수정
     */
    @Transactional
    public ResMessage<AgentDTO.DetailResponse> update(Long id, AgentDTO.UpdateRequest req) {
        Agent agent = agentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("업체를 찾을 수 없습니다. id=" + id));
        agent.update(req);
        return new ResMessage<>(1, "업체 정보가 수정되었습니다.", agentMapper.toDetailResponse(agent));
    }

    /**
     * 업체 삭제 (soft delete, 복수 처리)
     * isVisible을 N으로 변경한다. 실제 데이터는 삭제되지 않는다.
     * 존재하지 않는 id는 findAllById에서 조용히 무시된다.
     */
    @Transactional
    public void delete(AgentDTO.DeleteRequest req) {
        List<Agent> agents = agentRepository.findAllById(req.getIds());
        agents.forEach(Agent::hide);
    }
}
