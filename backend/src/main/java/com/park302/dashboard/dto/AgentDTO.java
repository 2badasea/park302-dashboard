package com.park302.dashboard.dto;

import com.park302.dashboard.common.GridPageRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 업체관리 DTO 모음
 * 요청 DTO: @Getter @Setter (바인딩 필요)
 * 응답 DTO: record (불변, MapStruct 1.5+ 지원)
 */
public class AgentDTO {

    // -------------------------------------------------------------------------
    // 요청 DTO
    // -------------------------------------------------------------------------

    /**
     * 목록 조회 요청
     * @ModelAttribute로 바인딩. GridPageRequest(page, perPage)를 상속해 검색 조건 추가
     */
    @Getter
    @Setter
    public static class ListRequest extends GridPageRequest {
        /** 검색 대상 필드. ALL / NAME / CLIENT_CODE / ADDRESS / TEL / EMAIL */
        private String searchType = "ALL";
        /** 검색 키워드. 빈 문자열이면 전체 조회 */
        private String keyword = "";
    }

    /**
     * 등록 요청
     * name은 필수, 나머지는 선택
     */
    @Getter
    @Setter
    public static class CreateRequest {
        @NotBlank(message = "업체명은 필수입니다.")
        @Size(max = 100, message = "업체명은 100자 이내로 입력해 주세요.")
        private String name;

        @Size(max = 50, message = "Client Code는 50자 이내로 입력해 주세요.")
        private String clientCode;

        @Size(max = 20)
        private String businessNumber;

        @Size(max = 20)
        private String contactTel;

        @Size(max = 100)
        private String contactEmail;

        @Size(max = 200)
        private String address;

        private String memo;
    }

    /**
     * 수정 요청 — 등록 요청과 동일한 필드 구조
     */
    @Getter
    @Setter
    public static class UpdateRequest {
        @NotBlank(message = "업체명은 필수입니다.")
        @Size(max = 100, message = "업체명은 100자 이내로 입력해 주세요.")
        private String name;

        @Size(max = 50, message = "Client Code는 50자 이내로 입력해 주세요.")
        private String clientCode;

        @Size(max = 20)
        private String businessNumber;

        @Size(max = 20)
        private String contactTel;

        @Size(max = 100)
        private String contactEmail;

        @Size(max = 200)
        private String address;

        private String memo;
    }

    /**
     * 벌크 삭제 요청
     * 체크박스로 선택된 업체 id 목록을 받는다.
     */
    @Getter
    @Setter
    public static class DeleteRequest {
        private List<Long> ids;
    }

    // -------------------------------------------------------------------------
    // 응답 DTO (record — 불변, 응답 전용)
    // -------------------------------------------------------------------------

    /**
     * 목록 항목 — AgentListProjection과 MapStruct로 매핑
     * TUI Grid 컬럼과 1:1 대응
     */
    public record ListItem(
        Long id,
        String name,
        String clientCode,
        String address,
        String contactTel,
        String contactEmail
    ) {}

    /**
     * 상세 조회 응답 — 수정 모달 폼에 데이터를 채울 때 사용
     */
    public record DetailResponse(
        Long id,
        String name,
        String clientCode,
        String businessNumber,
        String contactTel,
        String contactEmail,
        String address,
        String memo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {}
}
