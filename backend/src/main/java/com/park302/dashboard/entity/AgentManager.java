package com.park302.dashboard.entity;

import com.park302.dashboard.common.enums.IsVisible;
import com.park302.dashboard.dto.AgentManagerDTO;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 업체담당자 엔티티
 * 업체(Agent)와 N:1 관계. is_visible = N으로 soft delete 처리한다.
 * 주담당자(is_primary=Y) 제약은 앱 레벨에서 관리한다.
 */
@Entity
@Table(name = "agent_manager")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentManager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 업체. 지연 로딩으로 불필요한 join 방지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 50)
    private String department;

    @Column(length = 50)
    private String position;

    @Column(length = 20)
    private String tel;

    @Column(length = 100)
    private String email;

    /**
     * 주담당자 여부 (Y/N).
     * 업체당 1명 제약은 앱 레벨에서 관리. DB CHAR(1)로 저장.
     */
    @Column(name = "is_primary", nullable = false, columnDefinition = "CHAR(1)")
    private String isPrimary = "N";

    @Enumerated(EnumType.STRING)
    @Column(name = "is_visible", nullable = false, columnDefinition = "CHAR(1)")
    private IsVisible isVisible = IsVisible.Y;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
    // 팩토리 메서드 / 도메인 로직
    // -------------------------------------------------------------------------

    /** 신규 담당자 생성 팩토리 메서드 */
    public static AgentManager create(Agent agent, AgentManagerDTO.Item req) {
        AgentManager m = new AgentManager();
        m.agent = agent;
        m.name = req.getName();
        m.department = req.getDepartment();
        m.position = req.getPosition();
        m.tel = req.getTel();
        m.email = req.getEmail();
        return m;
    }

    /** 담당자 정보 수정 */
    public void update(AgentManagerDTO.Item req) {
        this.name = req.getName();
        this.department = req.getDepartment();
        this.position = req.getPosition();
        this.tel = req.getTel();
        this.email = req.getEmail();
    }

    /** Soft delete: isVisible을 N으로 변경 */
    public void hide() {
        this.isVisible = IsVisible.N;
    }
}
