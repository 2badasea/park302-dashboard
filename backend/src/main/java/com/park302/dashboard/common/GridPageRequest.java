package com.park302.dashboard.common;

import lombok.Getter;
import lombok.Setter;

/**
 * TUI Grid 서버사이드 페이지네이션 요청 베이스 클래스
 * 각 기능의 ListRequest DTO가 이 클래스를 extends하여 검색 조건을 추가한다.
 *
 * 주의: Spring Data의 PageRequest(org.springframework.data.domain.PageRequest)와
 * 이름이 충돌하지 않도록 GridPageRequest로 명명한다.
 *
 * @ModelAttribute로 바인딩되므로 getter/setter 모두 필요
 */
@Getter
@Setter
public class GridPageRequest {
    /** 현재 페이지 (1-based). TUI Grid 페이지네이션 기준 */
    private int page = 1;
    /** 페이지당 행 수 */
    private int perPage = 20;
}
