-- =============================================================================
-- Park302 Dashboard — Full Schema
-- 이 파일은 항상 최신 전체 스키마 상태를 유지한다.
-- 새 환경 셋업 시 이 파일 하나만 실행하면 전체 스키마가 구성되어야 한다.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- agent : 연동 업체
-- client_code는 각 업체 ERP의 application.properties에 저장된 식별 코드.
-- 문의 등록 시 client_code로 agent를 조회하여 agent_id를 work에 바인딩한다.
-- -----------------------------------------------------------------------------
CREATE TABLE agent (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    client_code         VARCHAR(50)     NULL        UNIQUE      COMMENT 'ERP 연동 식별 코드 (예: cali-dev). 연동 없는 업체는 NULL',
    api_key             VARCHAR(100)    NULL        UNIQUE      COMMENT '외부 연동 인증 키 (X-Api-Key 헤더). 연동 없는 업체는 NULL',
    callback_url        VARCHAR(500)    NULL                    COMMENT 'webhook 수신 URL. NULL이면 이벤트 발송 안 함',
    callback_key        VARCHAR(100)    NULL                    COMMENT 'webhook 호출 시 인증 키 (X-Api-Key 헤더)',
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


-- -----------------------------------------------------------------------------
-- agent_manager : 업체 담당자
-- 주담당자(is_primary=Y) 는 업체당 1명. 제약은 앱 레벨에서 관리한다.
-- (Y→N, N→Y 전환 시 트랜잭션 처리 필요)
-- -----------------------------------------------------------------------------
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


-- -----------------------------------------------------------------------------
-- work : 문의사항
-- 업체 ERP에서 등록한 문의글을 저장한다.
-- -----------------------------------------------------------------------------
CREATE TABLE work (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    agent_id            BIGINT          NOT NULL,                                       -- 요청 업체 ID
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
    INDEX idx_work_created_at (created_at),
    CONSTRAINT fk_work_agent FOREIGN KEY (agent_id) REFERENCES agent(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- -----------------------------------------------------------------------------
-- file_info : 첨부파일 및 정적 리소스 메타데이터
-- 실제 파일은 NCP Object Storage에 저장하고, 이 테이블은 메타데이터만 관리한다.
-- ref_table + ref_id 조합으로 어느 도메인 레코드에 속한 파일인지 식별한다.
-- -----------------------------------------------------------------------------
CREATE TABLE file_info (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    ref_table           VARCHAR(50)     NOT NULL                COMMENT '참조 테이블명 (예: work)',
    ref_id              BIGINT          NOT NULL                COMMENT '참조 테이블 PK',
    agent_id            BIGINT          NULL                    COMMENT '소속 업체 ID (업체별 경로 분리 및 접근 제어용, NULL이면 공통)',
    original_name       VARCHAR(255)    NOT NULL                COMMENT '원본 파일명',
    stored_name         VARCHAR(255)    NOT NULL                COMMENT '스토리지 저장 파일명 (uuid_원본명)',
    storage_path        VARCHAR(500)    NOT NULL                COMMENT 'NCP 스토리지 내 전체 경로 (예: park302/cali/work/uuid_name.pdf)',
    file_ext            VARCHAR(20)     NULL                    COMMENT '확장자 소문자, 점 제외 (예: pdf)',
    file_size           BIGINT          NULL                    COMMENT '파일 크기 (bytes)',
    mime_type           VARCHAR(100)    NULL                    COMMENT 'MIME 타입 (예: image/png)',
    create_member_id    BIGINT          NULL                    COMMENT '등록자 ID',
    create_member_name  VARCHAR(50)     NULL                    COMMENT '등록자 이름 (스냅샷)',
    created_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_file_info_ref     (ref_table, ref_id),
    INDEX idx_file_info_agent   (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='첨부파일 및 정적 리소스 메타데이터';


-- -----------------------------------------------------------------------------
-- work_comment : 문의 댓글
-- AGENT(업체 측)와 DEV(개발팀)가 주고받는 댓글/대댓글 구조.
-- parent_id = NULL: 최상위 댓글 / parent_id NOT NULL: 대댓글 (최대 2depth 앱 레벨 강제)
-- -----------------------------------------------------------------------------
CREATE TABLE work_comment (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    work_id             BIGINT          NOT NULL                COMMENT '참조 문의 ID',
    parent_id           BIGINT          NULL                    COMMENT '부모 댓글 ID. NULL=최상위 댓글, NOT NULL=대댓글. 최대 2depth 앱 레벨 강제',
    content             TEXT            NOT NULL                COMMENT '댓글 내용',
    author_type         VARCHAR(10)     NOT NULL                COMMENT 'AGENT(업체 측) / DEV(개발팀)',
    author_name         VARCHAR(50)     NOT NULL                COMMENT '작성자 이름',
    create_member_id    BIGINT          NULL                    COMMENT '작성자 ID (ERP에서 전달)',
    created_at          DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_work_comment_work_id      (work_id),
    INDEX idx_work_comment_parent_id    (parent_id),
    CONSTRAINT fk_work_comment_work     FOREIGN KEY (work_id)   REFERENCES work(id),
    CONSTRAINT fk_work_comment_parent   FOREIGN KEY (parent_id) REFERENCES work_comment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='문의 댓글 (최대 2depth: 댓글 + 대댓글)';
