package com.park302.dashboard.entity;

import com.park302.dashboard.service.NcpStorageService;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 첨부파일 메타데이터 엔티티
 * 실제 파일은 NCP Object Storage에 저장하고, 이 테이블은 경로·이름·크기 등 메타데이터만 관리한다.
 * ref_table + ref_id 조합으로 어느 도메인 레코드에 속한 파일인지 식별한다.
 */
@Entity
@Table(name = "file_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 참조 테이블명 (예: "work") */
    @Column(nullable = false, length = 50)
    private String refTable;

    /** 참조 테이블 PK */
    @Column(nullable = false)
    private Long refId;

    /** 소속 업체 ID — 경로 분리 및 접근 제어용. NULL이면 공통 */
    private Long agentId;

    /** 원본 파일명 */
    @Column(nullable = false, length = 255)
    private String originalName;

    /** 스토리지 저장 파일명 (uuid_원본명) */
    @Column(nullable = false, length = 255)
    private String storedName;

    /** NCP 스토리지 내 전체 경로 */
    @Column(nullable = false, length = 500)
    private String storagePath;

    /** 확장자 소문자, 점 제외 (예: pdf) */
    @Column(length = 20)
    private String fileExt;

    /** 파일 크기 (bytes) */
    private Long fileSize;

    /** MIME 타입 (예: image/png) */
    @Column(length = 100)
    private String mimeType;

    private Long createMemberId;

    @Column(length = 50)
    private String createMemberName;

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
     * NCP 업로드 결과로 FileInfo 생성
     * @param refTable         참조 테이블명
     * @param refId            참조 테이블 PK
     * @param agentId          소속 업체 ID
     * @param result           NcpStorageService.UploadResult
     * @param createMemberId   등록자 ID (없으면 null)
     * @param createMemberName 등록자 이름 스냅샷 (없으면 null)
     */
    public static FileInfo of(String refTable, Long refId, Long agentId,
                               NcpStorageService.UploadResult result,
                               Long createMemberId, String createMemberName) {
        FileInfo fi = new FileInfo();
        fi.refTable = refTable;
        fi.refId = refId;
        fi.agentId = agentId;
        fi.originalName = result.originalName();
        fi.storedName = result.storedName();
        fi.storagePath = result.storagePath();
        fi.fileSize = result.fileSize();
        fi.mimeType = result.mimeType();
        // 확장자 추출: 마지막 '.' 이후 소문자
        int dotIdx = result.originalName().lastIndexOf('.');
        fi.fileExt = dotIdx >= 0
            ? result.originalName().substring(dotIdx + 1).toLowerCase()
            : null;
        fi.createMemberId = createMemberId;
        fi.createMemberName = createMemberName;
        return fi;
    }
}
