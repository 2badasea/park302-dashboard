package com.park302.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/** 댓글 DTO 모음 */
public class WorkCommentDTO {

    // -------------------------------------------------------------------------
    // 요청 DTO
    // -------------------------------------------------------------------------

    /**
     * 외부 API 댓글 등록 요청 (업체 측 AGENT)
     * authorType은 AGENT로 고정, 작성자 정보는 ERP에서 전달
     */
    @Getter
    @Setter
    public static class ExternalCreateRequest {
        @NotBlank(message = "내용은 필수입니다.")
        private String content;

        /** 부모 댓글 ID. null이면 최상위 댓글, 값이 있으면 대댓글 */
        private Long parentId;

        @NotNull(message = "작성자 ID는 필수입니다.")
        private Long createMemberId;

        @NotBlank(message = "작성자 이름은 필수입니다.")
        private String createMemberName;
    }

    /**
     * 관리자(DEV) 댓글 등록 요청
     * JWT 인증 후 호출. authorName은 서버에서 JWT의 username을 사용
     */
    @Getter
    @Setter
    public static class DevCreateRequest {
        @NotBlank(message = "내용은 필수입니다.")
        private String content;

        /** 부모 댓글 ID. null이면 최상위 댓글, 값이 있으면 대댓글 */
        private Long parentId;
    }

    // -------------------------------------------------------------------------
    // 응답 DTO
    // -------------------------------------------------------------------------

    /**
     * 댓글 응답 (2depth 트리 구조)
     * depth 1 댓글(parent==null)은 replies 리스트에 대댓글을 포함한다.
     * depth 2 (대댓글)의 replies는 항상 빈 리스트 (최대 2depth 제한).
     */
    public record CommentItem(
        Long id,
        Long parentId,
        String content,
        String authorType,
        String authorName,
        LocalDateTime createdAt,
        List<CommentItem> replies
    ) {}
}
