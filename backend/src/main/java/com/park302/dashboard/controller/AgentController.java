package com.park302.dashboard.controller;

import com.park302.dashboard.common.PageData;
import com.park302.dashboard.common.ResMessage;
import com.park302.dashboard.dto.AgentDTO;
import com.park302.dashboard.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 업체관리 REST API 컨트롤러
 * Base URI: /api/agents
 */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * 업체 목록 조회 (서버사이드 페이지네이션)
     * GET /api/agents?page=1&perPage=20&searchType=ALL&keyword=
     */
    @GetMapping
    public ResponseEntity<ResMessage<PageData<AgentDTO.ListItem>>> getList(
            @ModelAttribute AgentDTO.ListRequest req) {
        return ResponseEntity.ok(agentService.getList(req));
    }

    /**
     * 업체 상세 조회 (수정 모달 진입 시)
     * GET /api/agents/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResMessage<AgentDTO.DetailResponse>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(agentService.getDetail(id));
    }

    /**
     * 업체 등록
     * POST /api/agents → 201 Created
     */
    @PostMapping
    public ResponseEntity<ResMessage<AgentDTO.DetailResponse>> create(
            @RequestBody @Valid AgentDTO.CreateRequest req) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(agentService.create(req));
    }

    /**
     * 업체 수정 (부분 수정)
     * PATCH /api/agents/{id}
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ResMessage<AgentDTO.DetailResponse>> update(
            @PathVariable Long id,
            @RequestBody @Valid AgentDTO.UpdateRequest req) {
        return ResponseEntity.ok(agentService.update(id, req));
    }

    /**
     * 업체 삭제 (soft delete, 복수)
     * DELETE /api/agents → 204 No Content
     * 요청 body: { "ids": [1, 2, 3] }
     */
    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestBody AgentDTO.DeleteRequest req) {
        agentService.delete(req);
        return ResponseEntity.noContent().build();
    }
}
