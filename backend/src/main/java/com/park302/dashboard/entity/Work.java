package com.park302.dashboard.entity;

import com.park302.dashboard.common.enums.IsVisible;
import com.park302.dashboard.common.enums.WorkCategory;
import com.park302.dashboard.common.enums.WorkPriority;
import com.park302.dashboard.common.enums.WorkStatus;
import com.park302.dashboard.dto.WorkDTO;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 문의사항 엔티티
 * 업체 ERP(cali 등)에서 등록한 문의글을 저장한다.
 * create_member_* 필드는 ERP에서 전달한 스냅샷으로 저장.
 * 외부 DB의 사용자 정보가 변경되어도 등록 당시 정보가 보존된다.
 */
@Entity
@Table(name = "work")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Work {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 요청 업체. 지연 로딩 — 목록 조회 시 불필요한 JOIN 방지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    /** 업체 기준 중요도 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkPriority priorityByAgent = WorkPriority.NORMAL;

    /** 개발팀 내부 중요도 (업체에게 비노출) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkPriority priorityByDev = WorkPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkStatus workStatus = WorkStatus.READY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "CHAR(1)")
    private IsVisible isPinned = IsVisible.N;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "CHAR(1)")
    private IsVisible isVisible = IsVisible.Y;

    private LocalDate expectStartDay;
    private LocalDate expectFinishDay;
    private LocalDate startDay;
    private LocalDate finishDay;

    /** 작성자 ID (ERP에서 전달) */
    @Column(nullable = false)
    private Long createMemberId;

    /** 작성자 이름 스냅샷 (ERP에서 전달) */
    @Column(nullable = false, length = 50)
    private String createMemberName;

    /** 작성자 연락처 스냅샷 (없을 수 있음) */
    @Column(length = 20)
    private String createMemberTel;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private Long updateMemberId;

    /** soft delete 일시 */
    private LocalDateTime deletedAt;
    private Long deleteMemberId;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // 팩토리 메서드
    // -------------------------------------------------------------------------

    /**
     * 외부(ERP) 등록 팩토리 메서드
     * agent는 ApiKeyAuthFilter에서 주입된 agentId로 조회한 값을 사용한다.
     * priorityByAgent가 null이면 NORMAL로 기본 설정.
     */
    public static Work createByExternal(Agent agent, WorkDTO.ExternalCreateRequest req) {
        Work work = new Work();
        work.agent = agent;
        work.category = req.getCategory();
        work.title = req.getTitle();
        work.content = req.getContent();
        work.priorityByAgent = req.getPriorityByAgent() != null
            ? req.getPriorityByAgent() : WorkPriority.NORMAL;
        work.createMemberId = req.getCreateMemberId();
        work.createMemberName = req.getCreateMemberName();
        work.createMemberTel = req.getCreateMemberTel();
        return work;
    }
}
