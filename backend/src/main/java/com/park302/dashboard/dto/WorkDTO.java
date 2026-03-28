package com.park302.dashboard.dto;

import com.park302.dashboard.common.GridPageRequest;
import com.park302.dashboard.common.enums.WorkCategory;
import com.park302.dashboard.common.enums.WorkPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/** 문의사항 DTO 모음 */
public class WorkDTO {

    // -------------------------------------------------------------------------
    // 요청 DTO
    // -------------------------------------------------------------------------

    /**
     * 외부 API 목록 조회 요청 (cali → 대시보드)
     * GridPageRequest(page, perPage) 상속. 추후 필터 조건 추가 가능.
     */
    @Getter
    @Setter
    public static class ExternalListRequest extends GridPageRequest {
        // 추후 status, category 필터 추가 예정
    }

    /**
     * 외부 API 등록 요청 (cali → 대시보드)
     * multipart/form-data의 JSON Blob 파트('data')로 전달됨.
     * agent_id는 ApiKeyAuthFilter에서 주입하므로 요청에 포함하지 않는다.
     */
    @Getter
    @Setter
    public static class ExternalCreateRequest {
        @NotNull(message = "문의 유형은 필수입니다.")
        private WorkCategory category;

        /** 업체 기준 중요도. null이면 NORMAL로 처리 */
        private WorkPriority priorityByAgent;

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자 이내로 입력해 주세요.")
        private String title;

        @NotBlank(message = "내용은 필수입니다.")
        private String content;

        @NotNull(message = "작성자 ID는 필수입니다.")
        private Long createMemberId;

        @NotBlank(message = "작성자 이름은 필수입니다.")
        @Size(max = 50, message = "작성자 이름은 50자 이내로 입력해 주세요.")
        private String createMemberName;

        @Size(max = 20)
        private String createMemberTel;
    }

    // -------------------------------------------------------------------------
    // 응답 DTO
    // -------------------------------------------------------------------------

    /**
     * 목록 항목 응답 (외부 API)
     * 본문(content)은 포함하지 않아 응답 크기를 최소화한다.
     */
    public record ListItem(
        Long id,
        String title,
        String category,
        String priorityByAgent,
        String workStatus,
        String createMemberName,
        LocalDateTime createdAt,
        Long commentCount
    ) {}

    /**
     * 상세 응답 (외부 API)
     * 첨부파일 목록과 댓글 트리를 포함한다.
     */
    public record DetailResponse(
        Long id,
        String title,
        String category,
        String priorityByAgent,
        String workStatus,
        String content,
        String createMemberName,
        String createMemberTel,
        LocalDateTime createdAt,
        List<FileInfoDTO.Item> files,
        List<WorkCommentDTO.CommentItem> comments
    ) {}
}
