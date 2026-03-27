package com.park302.dashboard.common;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * TUI Grid 서버사이드 페이지네이션 응답 래퍼
 * ResMessage의 data 필드에 담겨 반환된다.
 * 예) ResMessage<PageData<AgentDTO.ListItem>>
 *
 * 응답 전용 — builder로 생성 후 수정하지 않으므로 setter 불필요
 */
@Getter
@Builder
public class PageData<T> {
    private List<T> content;
    /** 전체 행 수 (TUI Grid 페이지네이션 totalCount에 사용) */
    private long totalCount;
    /** 전체 페이지 수 */
    private int totalPages;
    /** 현재 페이지 (1-based) */
    private int currentPage;
    /** 페이지당 행 수 */
    private int perPage;
}
