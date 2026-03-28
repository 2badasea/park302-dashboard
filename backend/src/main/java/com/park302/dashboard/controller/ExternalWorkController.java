package com.park302.dashboard.controller;

import com.park302.dashboard.common.PageData;
import com.park302.dashboard.common.ResMessage;
import com.park302.dashboard.config.AgentPrincipal;
import com.park302.dashboard.dto.WorkCommentDTO;
import com.park302.dashboard.dto.WorkDTO;
import com.park302.dashboard.entity.FileInfo;
import com.park302.dashboard.service.NcpStorageService;
import com.park302.dashboard.service.WorkCommentService;
import com.park302.dashboard.service.WorkService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.util.List;

/**
 * 외부 API 컨트롤러 — cali 등 연동 업체 전용
 * X-Api-Key 인증 후 ApiKeyAuthFilter가 AgentPrincipal을 SecurityContext에 주입한다.
 * 모든 작업은 인증된 업체(agentId) 범위로 스코핑된다.
 */
@RestController
@RequestMapping("/api/external")
@RequiredArgsConstructor
public class ExternalWorkController {

    private final WorkService workService;
    private final WorkCommentService workCommentService;
    private final NcpStorageService ncpStorageService;

    /**
     * GET /api/external/works
     * 내 업체의 문의 목록 조회 (페이징, 최신순)
     */
    @GetMapping("/works")
    public ResponseEntity<ResMessage<PageData<WorkDTO.ListItem>>> getList(
        @AuthenticationPrincipal AgentPrincipal principal,
        @ModelAttribute WorkDTO.ExternalListRequest req
    ) {
        return ResponseEntity.ok(
            workService.getExternalList(principal.agentId(), req)
        );
    }

    /**
     * POST /api/external/works
     * 문의 등록 (multipart/form-data)
     *
     * 파트 구성:
     * - 'data' : JSON Blob (ExternalCreateRequest)
     * - 'files': 첨부파일 목록 (선택)
     */
    @PostMapping(value = "/works", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResMessage<WorkDTO.DetailResponse>> create(
        @AuthenticationPrincipal AgentPrincipal principal,
        @RequestPart("data") @Valid WorkDTO.ExternalCreateRequest data,
        @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws IOException {
        return ResponseEntity.status(201).body(
            workService.createByExternal(principal.agentId(), data, files)
        );
    }

    /**
     * GET /api/external/works/{id}
     * 문의 상세 조회 (첨부파일 + 댓글 트리 포함)
     */
    @GetMapping("/works/{id}")
    public ResponseEntity<ResMessage<WorkDTO.DetailResponse>> getDetail(
        @AuthenticationPrincipal AgentPrincipal principal,
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(
            workService.getExternalDetail(principal.agentId(), id)
        );
    }

    /**
     * GET /api/external/works/{id}/files/{fileId}
     * 첨부파일 다운로드 (NCP 스트리밍)
     * Content-Disposition: attachment — 브라우저 다운로드 유도
     */
    @GetMapping("/works/{id}/files/{fileId}")
    public void downloadFile(
        @AuthenticationPrincipal AgentPrincipal principal,
        @PathVariable Long id,
        @PathVariable Long fileId,
        HttpServletResponse response
    ) throws IOException {
        // agentId 스코핑 + 파일 소속 검증
        FileInfo fileInfo = workService.getFileForDownload(principal.agentId(), id, fileId);

        response.setContentType(
            fileInfo.getMimeType() != null ? fileInfo.getMimeType() : "application/octet-stream"
        );
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + fileInfo.getOriginalName() + "\"");
        if (fileInfo.getFileSize() != null) {
            response.setContentLengthLong(fileInfo.getFileSize());
        }

        // NCP에서 스트리밍 — try-with-resources로 S3 스트림 반드시 닫음
        try (ResponseInputStream<GetObjectResponse> stream =
                 ncpStorageService.download(fileInfo.getStoragePath())) {
            StreamUtils.copy(stream, response.getOutputStream());
        }
    }

    /**
     * POST /api/external/works/{id}/comments
     * 업체 측(AGENT) 댓글 등록
     */
    @PostMapping("/works/{id}/comments")
    public ResponseEntity<ResMessage<Void>> createComment(
        @AuthenticationPrincipal AgentPrincipal principal,
        @PathVariable Long id,
        @RequestBody @Valid WorkCommentDTO.ExternalCreateRequest req
    ) {
        return ResponseEntity.status(201).body(
            workCommentService.createByAgent(principal.agentId(), id, req)
        );
    }
}
