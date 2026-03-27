package com.park302.dashboard.controller;

import com.park302.dashboard.common.ResMessage;
import com.park302.dashboard.dto.AgentManagerDTO;
import com.park302.dashboard.service.AgentManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 업체담당자 REST API 컨트롤러
 * Base URI: /api/agents/{agentId}/managers
 */
@RestController
@RequestMapping("/api/agents/{agentId}/managers")
@RequiredArgsConstructor
public class AgentManagerController {

    private final AgentManagerService agentManagerService;

    /**
     * 업체담당자 목록 조회
     * GET /api/agents/{agentId}/managers
     */
    @GetMapping
    public ResponseEntity<ResMessage<List<AgentManagerDTO.Item>>> getManagers(
            @PathVariable Long agentId) {
        return ResponseEntity.ok(agentManagerService.getManagers(agentId));
    }

    /**
     * 업체담당자 일괄 저장 (upsert + soft delete)
     * PUT /api/agents/{agentId}/managers
     * body: { "managers": [...], "deleteIds": [1, 2] }
     */
    @PutMapping
    public ResponseEntity<ResMessage<List<AgentManagerDTO.Item>>> saveManagers(
            @PathVariable Long agentId,
            @RequestBody AgentManagerDTO.SaveRequest req) {
        return ResponseEntity.ok(agentManagerService.saveManagers(agentId, req));
    }
}
