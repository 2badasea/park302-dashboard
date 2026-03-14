# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## 프로젝트 개요

**Park302**는 1인 관리자 전용 운영 대시보드다.
업체별 문의·공지·인프라·정산·작업 현황을 하나의 화면 구조에서 통합 관리한다.

- 내부 프로젝트명: `park302-dashboard` (JAR명, GitHub 레포명 등 모든 식별자 통일)
- 대상 사용자: 단일 관리자(본인) 전용. 외부 고객 로그인 없음
- UI 레이아웃 레퍼런스: `docs/plan_reference/layout.png`, `docs/plan_reference/login_reference.png`

---

## 아키텍처

두 개의 독립된 애플리케이션으로 구성한다. 서버 템플릿 엔진(Thymeleaf 등)은 사용하지 않는다.

### 백엔드
- **언어**: Java 17
- **빌드**: Gradle
- **프레임워크**: Spring Boot 3.x
- **보안**: Spring Security + **JWT 인증** (세션 방식 사용 안 함)
- **ORM**: Spring Data JPA (복잡한 조회는 Querydsl 추후 검토)
- **DB**: MySQL 8.0
- **계층 구조**: `Controller → Service → Repository`

### 프론트엔드
- **구성**: React SPA
- **빌드 도구**: Vite
- **스타일**: Bootstrap 5
- **언어**: TypeScript 사용 권장

### 배포
- **환경**: Linux 홈서버
- **컨테이너**: Docker + Nginx 리버스 프록시
- **배포 방식**: 수동 배포 (CI/CD는 추후 검토)
- **환경변수 관리**: 서버에 `.env` 파일을 두고 Docker Compose `env_file`로 참조. `.env`는 git에 커밋하지 않으며, `.env.example`만 커밋하여 필요 변수를 문서화. Spring Boot `application.yml`에서는 `${VAR_NAME}` 형태로 읽음

---

## 커맨드

> 프로젝트 스캐폴딩 완료 후 추가 예정

---

## 프론트엔드 라이브러리

| 용도 | 라이브러리 | 비고 |
|---|---|---|
| 리스트/그리드 | tui-grid (코어) | 아래 래퍼 방식 참고 |
| 캘린더 | FullCalendar React | |
| 스타일 | Bootstrap 5 | |
| 모달 / confirm / alert | SweetAlert2 | |
| 토스트 알림 | React-Toastify | |
| 리치 텍스트 에디터 | Toast UI Editor | |
| 파일 저장 | AWS S3 | 모든 첨부파일 S3 관리 |

### TUI Grid 사용 방식

`@toast-ui/react-grid`는 React 18과 호환성 문제(내부적으로 deprecated된 `ReactDOM.render` 사용)가 있어 **채택하지 않는다.**
대신 `tui-grid` 코어를 직접 사용하고, 프로젝트 내부에 `ToastGridWrapper.tsx` 공통 컴포넌트를 만들어 표준화한다.

- `useEffect`에서 tui-grid 인스턴스를 초기화하고 `gridInstance.resetData(newData)` 등 명령형 API를 래퍼 내부에서 처리
- 데이터/옵션은 React props로 받아서 내부에서 tui-grid에 전달
- 페이지네이션: **서버사이드 페이징** 사용, 그리드 헤더 sort 활용

---

## 공통 유틸 함수

`src/utils/` 또는 `src/lib/` 에 위치시키고 import해서 사용한다.
아래 함수들은 확정된 공통 유틸이며 임의로 변경하지 않는다. 수정이 필요하면 사용자에게 먼저 제안한다.

> **주의**: `gFetch`는 현재 인증 헤더 처리가 없다. JWT 방식 확정에 따라 `Authorization: Bearer <token>` 헤더 자동 추가 로직을 추가해야 한다.

```javascript
/**
 * fetch 래퍼
 * - JSON Content-Type 기본 설정 (multipart 전송 시 headers에서 Content-Type 제외할 것)
 * - HTTP 에러(response.ok=false) 시 Error throw
 * - 응답 JSON 자동 파싱 (204 No Content는 null 반환)
 */
export const gFetch = async (url, options = {}) => {
  const { headers, ...rest } = options;
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...headers },
    ...rest,
  });
  if (!res.ok) {
    const err = new Error(`HTTP ${res.status}`);
    err.status = res.status;
    try { err.data = await res.json(); } catch { err.data = null; }
    throw err;
  }
  if (res.status === 204) return null;
  return res.json();
};

/** 우측 상단 토스트. 앱에 <ToastContainer> 마운트 필요 */
export const gToast = (msg, type = 'info') => toast[type](msg);

/** 로딩 다이얼로그 표시. gCloseLoading()으로 반드시 닫을 것 */
export const gLoading = (title = '처리 중...') => {
  Swal.fire({ title, allowOutsideClick: false, showConfirmButton: false, didOpen: () => Swal.showLoading() });
};

/** 로딩 닫기 */
export const gCloseLoading = () => Swal.close();

/** 성공 메시지 — 중앙, 확인 버튼 + 3초 자동 닫힘 */
export const gSuccess = (title, html = '') =>
  Swal.fire({ title, html: html || undefined, icon: 'success', timer: 3000, timerProgressBar: true, showConfirmButton: true, confirmButtonText: '확인' });

/** 오류/안내 메시지 — 중앙, 확인 클릭 시 닫힘 */
export const gAlert = (title, html = '', icon = 'error') =>
  Swal.fire({ title, html: html || undefined, icon, confirmButtonText: '확인' });

/** Confirm 다이얼로그 — Promise<boolean> 반환 */
export const gConfirm = (title, html = '') =>
  Swal.fire({
    title, html: html || undefined, icon: 'question',
    showCancelButton: true, confirmButtonText: '확인', cancelButtonText: '취소',
  }).then((r) => r.isConfirmed);
```

---

## UI / UX 규칙

### 레이아웃 구조
```
┌─────────────┬──────────────────────────┐
│             │  헤더 (Header)            │
│  사이드바    ├──────────────────────────┤
│  (고정)     │                          │
│             │  본문 콘텐츠 영역         │
│             │                          │
└─────────────┴──────────────────────────┘
```
- 데스크톱 우선(Desktop First) 설계
- 태블릿: 사이드바 collapse 지원
- 모바일: 조회 중심 UI. 복잡한 편집/운영 기능은 PC 전용

### Alert / Confirm UX
- **SweetAlert2** (`gSuccess`, `gAlert`, `gConfirm`): confirm 다이얼로그, alert, 모달형 피드백
- **React-Toastify** (`gToast`): 폼 검증 메시지, 가벼운 안내/성공/실패 토스트

`gConfirm` 필수 적용 대상:
- 삭제 작업
- 대량 변경 작업
- 외부 DB 수정 등 민감 작업
- 외부 전파(ERP 공지 전달 등)가 있는 작업

### 상세/등록/수정 UI
- 상세 조회, 등록, 수정은 **모달**로 처리한다 (별도 페이지 이동 없음)

---

## 메뉴 구조

```
대시보드
  ├─ 캘린더 (기본 화면)
  └─ 리스트 보기 (전환 버튼)
업체관리
  └─ 목록 / 등록·수정·조회(모달)
문의관리
  └─ 목록 / 등록·수정·조회(모달)
공지관리
  └─ 목록 / 등록·수정·조회(모달)
게시판
  └─ 목록 / 등록·수정·조회(모달)
설정관리  ← 1차: UI 메뉴만. 스키마/기획 확정 후 구현
```

---

## 도메인 엔티티

| 엔티티 | 설명 | 비고 |
|---|---|---|
| `agent` | 업체 | |
| `work` | 문의사항 | 명칭 추후 확정 |
| `reply` | 댓글 / 대댓글 | `work` 참조. **최대 2depth** (댓글 + 대댓글, 그 이상 불허) |
| `file_info` | 첨부파일 및 정적 리소스 | 실제 파일은 S3 저장 |
| `env` | 앱 설정 | 스키마 기획 완료 후 구현 |
| `board` | 게시판 | |
| `notice` | 공지/알림 | 읽음 여부 별도 관리 안 함 |
| `log` | 이력 (감사 로그) | 민감 작업 변경 이력 필수 기록 |

### 문의 상태값
`대기 / 진행중 / 보류 / 완료 / 취소`
상태 전이 규칙은 스키마/기획 확정 후 정의 예정

### 캘린더 데이터
`work` 데이터를 캘린더에 표시. 등록일·예상시작일·예상종료일·완료일 등으로 필터링할 수 있도록 추후 구현. 구체적인 표시 기준은 기획 확정 후 결정.

---

## 코드 스타일

### 백엔드 패키지 구조

```
src/main/java/.../
  entity/                  # JPA 엔티티 (도메인 모델)
  repository/              # Spring Data JPA Repository
    projection/            # 목록/조회용 Projection 인터페이스
  service/                 # 비즈니스 로직 (*ServiceImpl.java, 별도 인터페이스 없음)
  dto/                     # 요청/응답 DTO
  mapper/                  # MapStruct 매퍼
  config/                  # Security / 스토리지 / 예외 처리 등 설정
  common/
    enums/                 # 업무 Enum
```

### 프론트엔드 폴더 구조

```
src/
  components/   # UI 컴포넌트
  hooks/        # 커스텀 훅
  services/     # API 호출 로직
  utils/        # 공통 유틸 (gFetch 등)
  lib/          # 외부 라이브러리 래퍼 (ToastGridWrapper 등)
  styles/       # 공통 CSS (common.css 포함)
```

---

## REST API 규칙

### HTTP 메서드
- 조회: `GET`
- 등록: `POST`
- 수정: `PATCH` (부분 수정 우선) / `PUT` (전체 교체일 때만)
- 삭제: `DELETE`
- 등록/수정을 하나의 엔드포인트에서 분기 처리하고 있다면 REST 규칙에 맞게 **분리를 제안**한다 (임의 분리 금지)

### URI 규칙
- 복수형 자원: `/api/.../members`
- 단건: `/api/.../members/{id}`
- 하위 자원: `/api/.../members/{id}/roles`

### HTTP 상태코드
- `POST` 생성 성공: `201` (필요 시 `Location` 헤더 포함)
- `DELETE` 성공: `204`
- 검증 실패: `400`
- 인증 실패: `401`
- 인가 실패: `403`

---

### 코드 작성 원칙
- 컴포넌트 책임을 명확히 분리한다
- 매직넘버와 과도한 하드코딩을 최소화한다
- 과도한 한 줄 체이닝, 복잡한 삼항연산, 불필요한 추상화를 지양한다
- 명확한 네이밍을 사용하고 지나친 축약어를 피한다

### 주석 정책
주석이 **필수**인 구간:
- 복잡한 비즈니스 로직
- 보안 관련 처리
- 외부 연동 지점 (ERP API 호출 등)
- 상태 전이 로직

자명한 코드에 과도한 주석을 달지 않는다. 코드의 의도는 이름과 구조로 먼저 드러나야 한다.

---

## 외부 ERP 연동 (2차 이후)

- 각 업체 ERP에서 Park302 API를 호출하여 문의 목록 조회, 글/댓글 등록·수정·삭제 처리
- 문의/댓글 원본 데이터는 모두 Park302 DB에 저장
- 실시간 자동 갱신 없음 — 새로고침/재조회 시 반영되는 **준실시간 구조**
- 공지 대상 직원: 각 업체 DB의 `member` 테이블에서 `agent_id = 0`인 직원

## 외부 DB 연동 (2차 이후)

- 업체별 외부 DB의 `menu` 테이블 조회/관리 기능
- 외부 DB 접속 정보는 암호화 저장
- 수정 전 사용자 confirm 필수, 변경 이력 저장 필수
- 런타임 동적 DataSource 전환 전략이 필요 — 구현 전 별도 설계 논의 필요
- `menu` 테이블 스키마는 업체 간 공통 스키마임을 확인함
- **1차에서는 더미데이터 기반 UI만 구현**

---

## 2차 이후 확장 예정 기능

| 기능 | 조건 |
|---|---|
| Slack 알림 (문의 등록, 댓글, 공지, 외부 DB 수정 이벤트) | 2차 |
| 내부 알림 시스템 | 2차 |
| Redis (캐시, 세션, 알림 큐) | 필요 시 검토 |
| WebSocket (실시간 알림) | 필요 시 검토 |
| Cloudflare (외부 공개 또는 보안 강화) | 필요 시 검토 |
| CI/CD 자동화 | 인프라 여건 확정 후 검토 |

---

## 구현 전 확인 원칙

- 기능 구현 전 이 문서와 범위를 먼저 확인한다
- 1차 범위를 벗어나는 기능은 임의로 추가하지 않는다
- 미확정 사항(상태 전이 규칙, 캘린더 표시 기준, 설정관리 스키마 등)은 사용자에게 먼저 확인한다
- 공통 유틸 함수(`gFetch`, `gToast` 등)를 수정해야 할 경우 먼저 제안하고 확정 후 수정한다

---

## 작업 범위 및 수정 원칙

- **요청받은 범위만 수정**한다. 같이 고치면 좋아 보이는 코드는 **제안만** 하고 임의 리팩토링은 금지한다.
- 변경은 가능한 작은 단위로 쪼개어 **diff가 과도하게 커지지 않게** 한다.
- **"UI만" 요청**이면 API/비즈니스/DB/연동 로직은 구현하지 않는다.
  → UI 구조·레이아웃·TUI Grid 정의·이벤트 핸들러 골격까지만 구현한다.

---

## 페이지/기능 검토 시 전체 스택 점검 원칙

특정 페이지 또는 기능에 대한 **검토/점검**을 지시받으면, 해당 기능과 관련된 프론트엔드 및 백엔드 로직 전체를 **직접 읽고** 점검한다. 탐색 에이전트 요약만 참고하는 것은 점검으로 인정하지 않는다.

점검 대상:
- **프론트엔드**: React 컴포넌트
- **백엔드**: Entity, DTO, REST API Controller, Service, Repository, Security(인증/인가 핸들러 포함)
- **런타임 설정**: `application.properties` — multipart 크기 제한, DB 설정 등 런타임 동작에 영향을 주는 설정값
- **예외처리**: `GlobalApiExceptionHandler` — 해당 기능에서 발생 가능한 예외가 모두 핸들링되는지 확인. 컨트롤러 실행 이전(예: multipart 파싱 단계)에 던져지는 예외는 `@RestControllerAdvice`가 잡지 못할 수 있으므로 특히 주의

점검 후 수정 사항은 **"즉시 수정 항목"** 과 **"제안 사항(승인 필요)"** 으로 구분하여 보고한다.

---

## 사전 승인 없이 절대 금지

다음 항목은 반드시 사용자 승인 후 진행한다:
- DB 스키마/DDL 변경 (테이블/컬럼/인덱스/제약)
- 코어 동작/공통 계약 변경 (전역 라우팅, 공통 레이아웃 규칙, 공통 유틸 함수 계약 변경 등)

필요해 보이면 아래 3가지를 요약해 승인을 먼저 받는다:
1. 필요 이유
2. 대안
3. 영향 범위 (백엔드/프론트엔드/DB/보안 포함)

---

## Git 규칙

- `git commit`, `git push`, merge/rebase는 **절대 자동 실행 금지**
- 작업 종료 시 항상 아래 항목을 제공한다:
  - **Changed files**: 변경 파일 목록 (경로)
  - **Summary**: 핵심 변경 요약 (3~12줄)
  - **How to verify**: 실행/화면/간단 점검 방법
  - **Notes/Risks**: 주의점/추가 확인 포인트
  - **권장 커밋 메시지**: 제안만, 한글로 작성

---

## 네이밍 / 코드 컨벤션

- 신규 코드/함수/변수는 **camelCase** 사용
- 신규 스네이크 케이스(언더바 포함) 도입 금지
- 기존 스네이크 케이스 정리는 **단계적 마이그레이션** 원칙 — 전역 치환 금지
- JavaScript/TypeScript 변수 선언은 **`const` 우선**, 필요 시 `let` 사용

---

## 보안 / 품질

- 비밀정보(키/토큰/비밀번호/내부 URL/IP)는 코드·로그·문서에 절대 하드코딩 금지
- 사용자에게 스택트레이스/내부 정보 노출 금지
- 변경 후 가능한 범위에서 **검증 방법**을 함께 제시한다
- CI/CD 경로, 버킷명, 서버 정보 등 환경별로 달라지는 민감값은 일반화하여 작성한다
