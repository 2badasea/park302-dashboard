package com.park302.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 업체담당자 DTO 모음
 */
public class AgentManagerDTO {

    /**
     * 담당자 단건 항목
     * 목록 조회 응답 및 저장 요청(upsert)에 공용으로 사용한다.
     * id가 있으면 update, 없으면 insert로 처리된다.
     */
    @Getter
    @Setter
    public static class Item {
        /** DB PK. 신규 등록 시 null */
        private Long id;
        private String name;
        private String department;
        private String position;
        private String tel;
        private String email;
    }

    /**
     * 담당자 일괄 저장 요청
     * 프론트엔드 그리드 상태를 그대로 전달한다.
     */
    @Getter
    @Setter
    public static class SaveRequest {
        /** upsert 대상 목록 (id 있으면 update, id 없으면 insert) */
        private List<Item> managers;
        /** soft delete 대상 id 목록 */
        private List<Long> deleteIds;
    }
}
