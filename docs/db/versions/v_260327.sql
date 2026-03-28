-- =============================================================================
-- Delta: v_260327
-- 작업일: 2026-03-27
-- 내용:
--   1. agent 테이블 생성 (연동 업체)
--   2. agent_manager 테이블 생성 (업체 담당자)
--   3. work 테이블 최초 생성 (문의관리)
--   4. work.agent_id FK 추가
-- =============================================================================

-- 1. agent
CREATE TABLE agent (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    client_code         VARCHAR(50)     NULL        UNIQUE      COMMENT 'ERP 연동 식별 코드 (예: cali-dev). 연동 없는 업체는 NULL',
    name                VARCHAR(100)    NOT NULL                COMMENT '업체명',
    business_number     VARCHAR(20)     NULL                    COMMENT '사업자등록번호',
    contact_tel         VARCHAR(20)     NULL                    COMMENT '대표 연락처',
    contact_email       VARCHAR(100)    NULL                    COMMENT '대표 이메일',
    address             VARCHAR(200)    NULL                    COMMENT '주소',
    memo                TEXT            NULL                    COMMENT '내부 메모',
    is_visible          CHAR(1)         NOT NULL    DEFAULT 'Y' COMMENT '노출 여부 (N: 숨김 처리)'
                            CHECK (is_visible IN ('Y', 'N')),
    created_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_agent_client_code (client_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='연동 업체';


-- 2. agent_manager
CREATE TABLE agent_manager (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    agent_id            BIGINT          NOT NULL                COMMENT '소속 업체 ID',
    name                VARCHAR(50)     NOT NULL                COMMENT '담당자 이름',
    department          VARCHAR(50)     NULL                    COMMENT '부서',
    position            VARCHAR(50)     NULL                    COMMENT '직책',
    tel                 VARCHAR(20)     NULL                    COMMENT '연락처',
    email               VARCHAR(100)    NULL                    COMMENT '이메일',
    is_primary          CHAR(1)         NOT NULL    DEFAULT 'N' COMMENT '주담당자 여부 (업체당 1명, 앱 레벨 관리)'
                            CHECK (is_primary IN ('Y', 'N')),
    is_visible          CHAR(1)         NOT NULL    DEFAULT 'Y' COMMENT '노출 여부 (N: 숨김 처리)'
                            CHECK (is_visible IN ('Y', 'N')),
    created_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_agent_manager_agent_id (agent_id),
    CONSTRAINT fk_agent_manager_agent FOREIGN KEY (agent_id) REFERENCES agent(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='업체 담당자';


-- 3. work

CREATE TABLE work (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    agent_id            BIGINT          NOT NULL,                                       -- 요청 업체 ID (추후 agent 테이블 생성 후 FK 추가 예정)
    category            VARCHAR(20)     NOT NULL                                        -- 문의 유형: BUG / NEW_FEATURE / INQUIRY / ERROR
                            CHECK (category IN ('BUG', 'NEW_FEATURE', 'INQUIRY', 'ERROR')),
    title               VARCHAR(200)    NOT NULL,                                       -- 문의 제목
    content             LONGTEXT        NOT NULL,                                       -- 문의 본문 (Toast UI Editor 리치텍스트, S3 이미지 URL 포함 가능)
    priority_by_agent   VARCHAR(20)     NOT NULL    DEFAULT 'NORMAL'                    -- 업체 기준 중요도
                            CHECK (priority_by_agent IN ('NORMAL', 'EMERGENCY')),
    priority_by_dev     VARCHAR(20)     NOT NULL    DEFAULT 'NORMAL'                    -- 개발팀 내부 중요도 (업체 비노출)
                            CHECK (priority_by_dev IN ('NORMAL', 'EMERGENCY')),
    work_status         VARCHAR(20)     NOT NULL    DEFAULT 'READY'                     -- 작업 상태
                            CHECK (work_status IN ('READY', 'IN_PROGRESS', 'ON_HOLD', 'DONE', 'CANCELLED')),
    is_pinned           CHAR(1)         NOT NULL    DEFAULT 'N'                         -- 상단 고정 여부
                            CHECK (is_pinned IN ('Y', 'N')),
    is_visible          CHAR(1)         NOT NULL    DEFAULT 'Y'                         -- 노출 여부 (N: 숨김 처리)
                            CHECK (is_visible IN ('Y', 'N')),
    expect_start_day    DATE            NULL,                                           -- 예상 시작일
    expect_finish_day   DATE            NULL,                                           -- 예상 종료일
    start_day           DATE            NULL,                                           -- 실제 시작일
    finish_day          DATE            NULL,                                           -- 실제 종료일
    create_member_id    BIGINT          NOT NULL,                                       -- 작성자 ID (ERP에서 전달)
    create_member_name  VARCHAR(50)     NOT NULL,                                       -- 작성자 이름 (ERP에서 전달, 스냅샷 저장)
    create_member_tel   VARCHAR(20)     NULL,                                           -- 작성자 연락처 (없을 수 있음)
    created_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,
    update_member_id    BIGINT          NULL,                                           -- 수정자 ID
    deleted_at          DATETIME        NULL,                                           -- soft delete 일시
    delete_member_id    BIGINT          NULL,                                           -- 삭제 처리자 ID

    PRIMARY KEY (id),
    INDEX idx_work_agent_id   (agent_id),
    INDEX idx_work_status     (work_status),
    INDEX idx_work_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 4. work FK 추가 (agent 생성 후)
ALTER TABLE work
    ADD CONSTRAINT fk_work_agent FOREIGN KEY (agent_id) REFERENCES agent(id);
