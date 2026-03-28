package com.park302.dashboard.service;

import com.park302.dashboard.common.ResMessage;
import com.park302.dashboard.dto.WorkCommentDTO;
import com.park302.dashboard.entity.Work;
import com.park302.dashboard.entity.WorkComment;
import com.park302.dashboard.repository.WorkCommentRepository;
import com.park302.dashboard.repository.WorkRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 문의 댓글 서비스
 *
 * 댓글 등록 흐름:
 * 1. work 조회 (agentId 또는 전체 스코핑)
 * 2. 2depth 제한 체크 (대댓글의 대댓글 방지)
 * 3. WorkComment 저장
 * 4. DEV 댓글인 경우 업체 callbackUrl로 webhook 발송 (callbackUrl == null이면 스킵)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkCommentService {

    private final WorkRepository workRepository;
    private final WorkCommentRepository workCommentRepository;
    private final RestTemplate restTemplate;

    /**
     * 업체 측(AGENT) 댓글 등록 (외부 API: POST /api/external/works/{id}/comments)
     * agentId 스코핑: 다른 업체의 문의에는 댓글 불가
     */
    @Transactional
    public ResMessage<Void> createByAgent(Long agentId, Long workId,
                                           WorkCommentDTO.ExternalCreateRequest req) {
        Work work = getWorkByAgent(workId, agentId);
        WorkComment parent = resolveParent(req.getParentId(), workId);

        WorkComment comment = WorkComment.createByAgent(
            work, parent, req.getContent(), req.getCreateMemberId(), req.getCreateMemberName()
        );
        workCommentRepository.save(comment);

        return new ResMessage<>(1, "댓글이 등록되었습니다.", null);
    }

    /**
     * 개발팀(DEV) 댓글 등록 (내부 API: POST /api/works/{id}/comments, JWT 인증)
     * 등록 완료 후 업체의 callbackUrl로 webhook 발송
     */
    @Transactional
    public ResMessage<Void> createByDev(Long workId, WorkCommentDTO.DevCreateRequest req,
                                         String authorName) {
        Work work = workRepository.findById(workId)
            .filter(w -> w.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다. id=" + workId));

        WorkComment parent = resolveParent(req.getParentId(), workId);
        WorkComment comment = WorkComment.createByDev(work, parent, req.getContent(), authorName);
        workCommentRepository.save(comment);

        // DEV 댓글 등록 후 webhook 발송 (트랜잭션 커밋 후 실행되지 않으므로 실패해도 롤백 안 됨)
        sendWebhook(work, comment);

        return new ResMessage<>(1, "댓글이 등록되었습니다.", null);
    }

    // -------------------------------------------------------------------------
    // 내부 헬퍼
    // -------------------------------------------------------------------------

    /**
     * agentId로 스코핑된 work 조회
     * 다른 업체의 work에 접근하면 EntityNotFoundException(→ 404) 처리
     */
    private Work getWorkByAgent(Long workId, Long agentId) {
        return workRepository.findById(workId)
            .filter(w -> w.getAgent().getId().equals(agentId) && w.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다. id=" + workId));
    }

    /**
     * parentId 검증 및 2depth 제한 체크
     * - parentId가 null: 최상위 댓글 → null 반환
     * - parentId의 parent가 이미 존재: 3depth 시도 → IllegalArgumentException(→ 400)
     * - parentId의 work가 다른 문의: cross-work 댓글 시도 → IllegalArgumentException(→ 400)
     */
    private WorkComment resolveParent(Long parentId, Long workId) {
        if (parentId == null) return null;

        WorkComment parent = workCommentRepository.findById(parentId)
            .orElseThrow(() -> new EntityNotFoundException("부모 댓글을 찾을 수 없습니다. id=" + parentId));

        // 부모의 부모가 존재하면 이미 2depth(대댓글)이므로 3depth 불가
        if (parent.getParent() != null) {
            throw new IllegalArgumentException("댓글은 최대 2depth까지만 허용됩니다.");
        }

        // 부모 댓글이 같은 work에 속하는지 확인
        if (!parent.getWork().getId().equals(workId)) {
            throw new IllegalArgumentException("부모 댓글이 해당 문의에 속하지 않습니다.");
        }

        return parent;
    }

    /**
     * DEV 댓글 등록 시 업체(cali)에 webhook 발송
     * callbackUrl이 NULL인 업체는 발송 스킵.
     * 발송 실패 시 예외를 삼켜 댓글 등록 자체는 성공 처리.
     * (webhook 실패가 메인 트랜잭션에 영향 주지 않음)
     *
     * TODO: 트래픽 증가 시 비동기 처리(@Async + ThreadPoolTaskExecutor) 또는
     *       메시지 큐(Redis pub/sub, Kafka) 방식으로 전환 권장.
     *       현재는 동기 RestTemplate 사용으로 callbackUrl 응답 지연이 API 응답 지연으로 이어질 수 있음.
     *       전환 시 spring-boot-starter-webflux 추가 후 WebClient.post()...subscribe() 사용.
     */
    private void sendWebhook(Work work, WorkComment comment) {
        String callbackUrl = work.getAgent().getCallbackUrl();
        String callbackKey = work.getAgent().getCallbackKey();

        if (callbackUrl == null || callbackUrl.isBlank()) {
            return;
        }

        // 알림 내용: 댓글 앞 50자 미리보기
        String contentPreview = comment.getContent().length() > 50
            ? comment.getContent().substring(0, 50) + "..."
            : comment.getContent();

        Map<String, Object> body = Map.of(
            "refType",        "WORK",
            "refId",          work.getId(),
            "content",        "[개발팀 댓글] " + work.getTitle() + ": " + contentPreview,
            "senderName",     comment.getAuthorName(),
            "targetMemberId", work.getCreateMemberId()
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (callbackKey != null && !callbackKey.isBlank()) {
                headers.set("X-Api-Key", callbackKey);
            }

            restTemplate.postForEntity(callbackUrl, new HttpEntity<>(body, headers), Void.class);
            log.info("Webhook sent to {} for work.id={}", callbackUrl, work.getId());

        } catch (Exception e) {
            // webhook 실패는 로그만 남기고 댓글 등록 성공은 유지
            log.warn("Webhook failed to {} for work.id={}: {}", callbackUrl, work.getId(), e.getMessage());
        }
    }
}
