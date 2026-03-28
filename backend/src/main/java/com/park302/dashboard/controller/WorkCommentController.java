package com.park302.dashboard.controller;

import com.park302.dashboard.common.ResMessage;
import com.park302.dashboard.dto.WorkCommentDTO;
import com.park302.dashboard.service.WorkCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자(DEV) 댓글 컨트롤러 — JWT 인증 필요
 * 개발팀이 문의에 댓글을 남기면 업체 측 callbackUrl로 webhook 알림을 발송한다.
 */
@RestController
@RequestMapping("/api/works")
@RequiredArgsConstructor
public class WorkCommentController {

    private final WorkCommentService workCommentService;

    /**
     * POST /api/works/{id}/comments
     * 개발팀(DEV) 댓글 등록
     * JWT에서 추출한 username을 authorName으로 사용한다.
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<ResMessage<Void>> createDevComment(
        @PathVariable Long id,
        @RequestBody @Valid WorkCommentDTO.DevCreateRequest req,
        Authentication authentication   // JwtAuthenticationFilter에서 설정된 username
    ) {
        String authorName = authentication.getName();
        return ResponseEntity.status(201).body(
            workCommentService.createByDev(id, req, authorName)
        );
    }
}
