-- =============================================================================
-- v_260328 : cali 연동 — 외부 API 인증 및 문의 댓글 구조 추가
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. agent 테이블: 외부 연동 인증/webhook 컬럼 추가
--    - api_key      : 외부 ERP가 대시보드 API 호출 시 X-Api-Key 헤더로 전달하는 키
--    - callback_url : 개발팀 댓글 등록 시 webhook을 보낼 ERP 수신 URL. NULL이면 미발송
--    - callback_key : webhook 호출 시 X-Api-Key 헤더에 담을 키
-- -----------------------------------------------------------------------------
ALTER TABLE agent
    ADD COLUMN api_key      VARCHAR(100)    DEFAULT NULL    COMMENT '외부 연동 인증 키 (X-Api-Key 헤더). 연동 없는 업체는 NULL'
        AFTER client_code,
    ADD COLUMN callback_url VARCHAR(500)    DEFAULT NULL    COMMENT 'webhook 수신 URL. NULL이면 이벤트 발송 안 함'
        AFTER api_key,
    ADD COLUMN callback_key VARCHAR(100)    DEFAULT NULL    COMMENT 'webhook 호출 시 인증 키 (X-Api-Key 헤더)'
        AFTER callback_url,
    ADD UNIQUE KEY uq_agent_api_key (api_key);


-- -----------------------------------------------------------------------------
-- 2. file_info 테이블 신규 생성
--    첨부파일 메타데이터. 실제 파일은 NCP Object Storage에 저장.
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
-- 3. work_comment 테이블 신규 생성
--    AGENT(업체 측) / DEV(개발팀) 댓글·대댓글 구조. 최대 2depth 앱 레벨 강제.
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
