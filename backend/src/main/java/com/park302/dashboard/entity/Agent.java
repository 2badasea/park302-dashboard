package com.park302.dashboard.entity;

import com.park302.dashboard.common.enums.IsVisible;
import com.park302.dashboard.dto.AgentDTO;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 업체 엔티티
 * ERP 연동 업체 및 단순 관리 업체를 모두 포함한다.
 * client_code가 있는 업체만 ERP 연동 대상이며, 없는 업체는 대시보드 내부 관리 전용이다.
 */
@Entity
@Table(name = "agent")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ERP 연동 식별 코드 (예: cali-dev). 연동 없는 업체는 NULL */
    @Column(unique = true)
    private String clientCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String businessNumber;

    @Column(length = 20)
    private String contactTel;

    @Column(length = 100)
    private String contactEmail;

    @Column(length = 200)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String memo;

    /**
     * 노출 여부. Y = 활성, N = 숨김(soft delete)
     * DB CHAR(1)과 EnumType.STRING으로 매핑
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private IsVisible isVisible = IsVisible.Y;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
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

    /**
     * 등록용 팩토리 메서드
     * client_code는 빈 문자열이면 NULL로 저장 (UNIQUE 제약 상 빈 문자열 중복 방지)
     */
    public static Agent create(AgentDTO.CreateRequest req) {
        Agent agent = new Agent();
        agent.name = req.getName();
        agent.clientCode = StringUtils.hasText(req.getClientCode()) ? req.getClientCode().trim() : null;
        agent.businessNumber = req.getBusinessNumber();
        agent.contactTel = req.getContactTel();
        agent.contactEmail = req.getContactEmail();
        agent.address = req.getAddress();
        agent.memo = req.getMemo();
        return agent;
    }

    /**
     * 수정 메서드
     * client_code는 빈 문자열이면 NULL로 저장
     */
    public void update(AgentDTO.UpdateRequest req) {
        this.name = req.getName();
        this.clientCode = StringUtils.hasText(req.getClientCode()) ? req.getClientCode().trim() : null;
        this.businessNumber = req.getBusinessNumber();
        this.contactTel = req.getContactTel();
        this.contactEmail = req.getContactEmail();
        this.address = req.getAddress();
        this.memo = req.getMemo();
    }

    /** Soft delete: isVisible을 N으로 변경 */
    public void hide() {
        this.isVisible = IsVisible.N;
    }
}
