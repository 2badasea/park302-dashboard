package com.park302.dashboard.service;

import com.park302.dashboard.common.ResMessage;
import com.park302.dashboard.common.enums.IsVisible;
import com.park302.dashboard.dto.AgentManagerDTO;
import com.park302.dashboard.entity.Agent;
import com.park302.dashboard.entity.AgentManager;
import com.park302.dashboard.repository.AgentManagerRepository;
import com.park302.dashboard.repository.AgentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 업체담당자 서비스
 * 담당자 조회 및 일괄 저장(upsert + soft delete)을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentManagerService {

    private final AgentManagerRepository agentManagerRepository;
    private final AgentRepository agentRepository;

    /**
     * 업체별 담당자 목록 조회 (is_visible = Y)
     */
    public ResMessage<List<AgentManagerDTO.Item>> getManagers(Long agentId) {
        List<AgentManager> managers = agentManagerRepository
            .findByAgent_IdAndIsVisibleOrderByIdAsc(agentId, IsVisible.Y);

        List<AgentManagerDTO.Item> items = managers.stream().map(m -> {
            AgentManagerDTO.Item item = new AgentManagerDTO.Item();
            item.setId(m.getId());
            item.setName(m.getName());
            item.setDepartment(m.getDepartment());
            item.setPosition(m.getPosition());
            item.setTel(m.getTel());
            item.setEmail(m.getEmail());
            return item;
        }).toList();

        return new ResMessage<>(1, "", items);
    }

    /**
     * 담당자 일괄 저장
     *
     * 처리 순서:
     * 1) deleteIds soft delete: is_visible = N
     * 2) managers upsert: id 있으면 update, id 없으면 insert
     * 3) 최신 담당자 목록 반환
     *
     * deleteIds와 managers에 동일 id가 들어오는 케이스는 프론트에서 방지한다.
     */
    @Transactional
    public ResMessage<List<AgentManagerDTO.Item>> saveManagers(
            Long agentId, AgentManagerDTO.SaveRequest req) {

        Agent agent = agentRepository.findById(agentId)
            .orElseThrow(() -> new EntityNotFoundException("업체를 찾을 수 없습니다. id=" + agentId));

        // 1) soft delete 처리
        if (req.getDeleteIds() != null && !req.getDeleteIds().isEmpty()) {
            List<AgentManager> toDelete = agentManagerRepository.findAllById(req.getDeleteIds());
            toDelete.forEach(AgentManager::hide);
        }

        // 2) upsert 처리 (id 있으면 update, 없으면 insert)
        if (req.getManagers() != null) {
            for (AgentManagerDTO.Item item : req.getManagers()) {
                if (item.getId() != null) {
                    agentManagerRepository.findById(item.getId())
                        .ifPresent(m -> m.update(item));
                } else {
                    agentManagerRepository.save(AgentManager.create(agent, item));
                }
            }
        }

        // 3) 저장 후 최신 목록 반환 (같은 트랜잭션 내)
        return getManagers(agentId);
    }
}
