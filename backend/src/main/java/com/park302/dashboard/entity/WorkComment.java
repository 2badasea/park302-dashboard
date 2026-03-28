package com.park302.dashboard.entity;

import com.park302.dashboard.common.enums.AuthorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 문의 댓글 엔티티
 * AGENT(업체 측) / DEV(개발팀)가 주고받는 댓글·대댓글 구조.
 * parent가 NULL이면 최상위 댓글, NOT NULL이면 대댓글.
 * 최대 2depth는 WorkCommentService에서 강제한다 (parent.parent != null이면 예외).
 */
@Entity
@Table(name = "work_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 문의. 지연 로딩 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id", nullable = false)
    private Work work;

    /**
     * 부모 댓글 (자기 참조).
     * NULL = 최상위 댓글, NOT NULL = 대댓글.
     * 최대 2depth — parent.parent가 존재하면 서비스 레이어에서 예외 발생.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private WorkComment parent;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuthorType authorType;

    @Column(nullable = false, length = 50)
    private String authorName;

    /** 작성자 ID (ERP 사용자 ID. 개발팀 댓글은 null) */
    private Long createMemberId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // 팩토리 메서드
    // -------------------------------------------------------------------------

    /**
     * 업체 측(AGENT) 댓글/대댓글 생성
     * @param work       소속 문의
     * @param parent     부모 댓글 (최상위이면 null)
     * @param content    댓글 내용
     * @param memberId   ERP 사용자 ID
     * @param memberName ERP 사용자 이름
     */
    public static WorkComment createByAgent(Work work, WorkComment parent,
                                             String content, Long memberId, String memberName) {
        WorkComment c = new WorkComment();
        c.work = work;
        c.parent = parent;
        c.content = content;
        c.authorType = AuthorType.AGENT;
        c.authorName = memberName;
        c.createMemberId = memberId;
        return c;
    }

    /**
     * 개발팀(DEV) 댓글/대댓글 생성
     * @param work       소속 문의
     * @param parent     부모 댓글 (최상위이면 null)
     * @param content    댓글 내용
     * @param authorName 관리자 이름 (JWT username)
     */
    public static WorkComment createByDev(Work work, WorkComment parent,
                                           String content, String authorName) {
        WorkComment c = new WorkComment();
        c.work = work;
        c.parent = parent;
        c.content = content;
        c.authorType = AuthorType.DEV;
        c.authorName = authorName;
        c.createMemberId = null;
        return c;
    }
}
