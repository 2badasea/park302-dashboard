package com.park302.dashboard.service;

import com.park302.dashboard.common.PageData;
import com.park302.dashboard.common.ResMessage;
import com.park302.dashboard.dto.FileInfoDTO;
import com.park302.dashboard.dto.WorkCommentDTO;
import com.park302.dashboard.dto.WorkDTO;
import com.park302.dashboard.entity.Agent;
import com.park302.dashboard.entity.FileInfo;
import com.park302.dashboard.entity.Work;
import com.park302.dashboard.entity.WorkComment;
import com.park302.dashboard.repository.AgentRepository;
import com.park302.dashboard.repository.FileInfoRepository;
import com.park302.dashboard.repository.WorkCommentRepository;
import com.park302.dashboard.repository.WorkRepository;
import com.park302.dashboard.repository.projection.WorkListProjection;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 문의사항 서비스
 * 외부 API(/api/external/works)에서 호출된다.
 * 모든 조회/변경은 agentId로 스코핑되어 다른 업체의 데이터에 접근할 수 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkService {

    private final WorkRepository workRepository;
    private final AgentRepository agentRepository;
    private final FileInfoRepository fileInfoRepository;
    private final WorkCommentRepository workCommentRepository;
    private final NcpStorageService ncpStorageService;

    /**
     * 문의 목록 조회 (agentId 스코핑, 최신순 페이징)
     */
    public ResMessage<PageData<WorkDTO.ListItem>> getExternalList(Long agentId,
                                                                   WorkDTO.ExternalListRequest req) {
        PageRequest pageable = PageRequest.of(
            req.getPage() - 1, req.getPerPage(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<WorkListProjection> page = workRepository.findByAgentId(agentId, pageable);

        List<WorkDTO.ListItem> content = page.getContent().stream()
            .map(p -> new WorkDTO.ListItem(
                p.getId(), p.getTitle(), p.getCategory(),
                p.getPriorityByAgent(), p.getWorkStatus(),
                p.getCreateMemberName(), p.getCreatedAt(), p.getCommentCount()
            ))
            .toList();

        return new ResMessage<>(1, "", PageData.<WorkDTO.ListItem>builder()
            .content(content)
            .totalCount(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .currentPage(req.getPage())
            .perPage(req.getPerPage())
            .build());
    }

    /**
     * 문의 등록 (외부 API, multipart)
     * agent는 ApiKeyAuthFilter 주입 agentId로 조회한다.
     * 첨부파일이 있으면 NCP에 업로드 후 file_info에 저장한다.
     */
    @Transactional
    public ResMessage<WorkDTO.DetailResponse> createByExternal(Long agentId,
                                                                WorkDTO.ExternalCreateRequest req,
                                                                List<MultipartFile> files)
        throws IOException {

        Agent agent = agentRepository.findById(agentId)
            .orElseThrow(() -> new EntityNotFoundException("업체를 찾을 수 없습니다."));

        Work work = Work.createByExternal(agent, req);
        workRepository.save(work);

        // 첨부파일 업로드 처리
        List<FileInfo> savedFiles = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                // clientCode가 없는 업체는 "common" 폴더 사용
                String clientCode = agent.getClientCode() != null ? agent.getClientCode() : "common";
                NcpStorageService.UploadResult result = ncpStorageService.upload(clientCode, "work", file);
                FileInfo fi = FileInfo.of("work", work.getId(), agentId,
                    result, req.getCreateMemberId(), req.getCreateMemberName());
                fileInfoRepository.save(fi);
                savedFiles.add(fi);
            }
        }

        return new ResMessage<>(1, "등록되었습니다.", toDetailResponse(work, savedFiles, List.of()));
    }

    /**
     * 문의 상세 조회 (외부 API, 첨부파일 + 댓글 포함)
     * agentId 스코핑: 다른 업체의 문의에 접근하면 404 처리
     */
    public ResMessage<WorkDTO.DetailResponse> getExternalDetail(Long agentId, Long workId) {
        Work work = workRepository.findById(workId)
            .filter(w -> w.getAgent().getId().equals(agentId) && w.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다. id=" + workId));

        List<FileInfo> files = fileInfoRepository.findByRefTableAndRefId("work", workId);
        List<WorkComment> comments = workCommentRepository.findByWorkIdOrderByCreatedAt(workId);

        return new ResMessage<>(1, "", toDetailResponse(work, files, comments));
    }

    /**
     * 파일 다운로드를 위한 FileInfo 조회 (agentId 스코핑)
     * work 소유권 + file이 해당 work에 속하는지 검증한다.
     */
    public FileInfo getFileForDownload(Long agentId, Long workId, Long fileId) {
        // work 소유권 확인
        workRepository.findById(workId)
            .filter(w -> w.getAgent().getId().equals(agentId) && w.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다. id=" + workId));

        return fileInfoRepository.findByIdAndRefTableAndRefId(fileId, "work", workId)
            .orElseThrow(() -> new EntityNotFoundException("파일을 찾을 수 없습니다. id=" + fileId));
    }

    // -------------------------------------------------------------------------
    // 내부 변환 헬퍼
    // -------------------------------------------------------------------------

    private WorkDTO.DetailResponse toDetailResponse(Work work,
                                                     List<FileInfo> files,
                                                     List<WorkComment> comments) {
        List<FileInfoDTO.Item> fileItems = files.stream()
            .map(f -> new FileInfoDTO.Item(
                f.getId(), f.getOriginalName(), f.getFileExt(),
                f.getFileSize(), f.getMimeType(), f.getCreatedAt()
            ))
            .toList();

        return new WorkDTO.DetailResponse(
            work.getId(), work.getTitle(),
            work.getCategory().name(),
            work.getPriorityByAgent().name(),
            work.getWorkStatus().name(),
            work.getContent(),
            work.getCreateMemberName(),
            work.getCreateMemberTel(),
            work.getCreatedAt(),
            fileItems,
            buildCommentTree(comments)
        );
    }

    /**
     * flat 댓글 목록 → 2depth 트리 구조 변환
     * 최상위 댓글(parent==null)을 수집하고, 각 댓글의 replies에 대댓글을 추가한다.
     */
    private List<WorkCommentDTO.CommentItem> buildCommentTree(List<WorkComment> comments) {
        // 최상위 댓글 (parent == null)
        List<WorkComment> roots = comments.stream()
            .filter(c -> c.getParent() == null)
            .toList();

        // 부모 ID별 대댓글 그룹핑
        Map<Long, List<WorkComment>> repliesMap = comments.stream()
            .filter(c -> c.getParent() != null)
            .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        return roots.stream()
            .map(root -> {
                List<WorkCommentDTO.CommentItem> replies = repliesMap
                    .getOrDefault(root.getId(), List.of())
                    .stream()
                    .map(reply -> new WorkCommentDTO.CommentItem(
                        reply.getId(),
                        root.getId(),
                        reply.getContent(),
                        reply.getAuthorType().name(),
                        reply.getAuthorName(),
                        reply.getCreatedAt(),
                        List.of()   // 대댓글의 하위는 2depth 제한으로 항상 빈 리스트
                    ))
                    .toList();

                return new WorkCommentDTO.CommentItem(
                    root.getId(),
                    null,
                    root.getContent(),
                    root.getAuthorType().name(),
                    root.getAuthorName(),
                    root.getCreatedAt(),
                    replies
                );
            })
            .toList();
    }
}
