# Park302 Dashboard

1인 관리자 전용 운영 대시보드.
업체별 문의·공지·인프라·정산·작업 현황을 하나의 화면에서 통합 관리한다.

---

## 기술 스택

### 백엔드
| 항목 | 내용 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Security | Spring Security + JWT |
| ORM | Spring Data JPA |
| DB | MySQL 8.0 |
| Build | Gradle |

### 프론트엔드
| 항목 | 내용 |
|---|---|
| Framework | React 18 (SPA) |
| Build Tool | Vite |
| Language | TypeScript |
| Style | Bootstrap 5 |
| Grid | TUI Grid (코어 직접 사용) |
| Calendar | FullCalendar React |
| Editor | Toast UI Editor |
| Modal/Alert | SweetAlert2 |
| Toast | React-Toastify |

### 인프라
| 항목 | 내용 |
|---|---|
| 운영 환경 | Linux 홈서버 |
| 컨테이너 | Docker |
| 프록시 | Nginx (리버스 프록시) |

---

## 프로젝트 구조

```
park302-dashboard/
├── backend/          # Spring Boot API 서버
│   └── src/
│       └── main/
│           ├── java/com/park302/dashboard/
│           │   ├── config/       # Security, 예외처리 등 설정
│           │   ├── controller/   # REST API 컨트롤러
│           │   ├── service/      # 비즈니스 로직
│           │   ├── repository/   # Spring Data JPA Repository
│           │   ├── entity/       # JPA 엔티티
│           │   ├── dto/          # 요청/응답 DTO
│           │   ├── mapper/       # MapStruct 매퍼
│           │   └── common/
│           │       └── enums/    # 업무 Enum
│           └── resources/
│               └── application.properties
├── frontend/         # React SPA
│   └── src/
│       ├── components/   # 공통 UI 컴포넌트
│       ├── pages/        # 페이지 컴포넌트
│       ├── hooks/        # 커스텀 훅
│       ├── services/     # API 호출 로직
│       ├── utils/        # 공통 유틸 (gFetch 등)
│       ├── lib/          # 외부 라이브러리 래퍼
│       └── styles/       # 공통 CSS
├── docs/             # 기획 참고 이미지 등
├── .env.example      # 환경변수 목록 (값 없음, 참고용)
└── .gitignore
```

---

## 메뉴 구조

```
대시보드       캘린더(기본) / 리스트 전환
업체관리       목록 / 등록·수정·상세(모달)
문의관리       목록 / 등록·수정·상세(모달)
공지관리       목록 / 등록·수정·상세(모달)
게시판         목록 / 등록·수정·상세(모달)
설정관리       (2차 이후 구현)
```

---

## 로컬 개발 환경 세팅

### 사전 요구사항
- JDK 17
- Node.js 18+
- MySQL 8.0

### 1. 저장소 클론

```bash
git clone https://github.com/{username}/park302-dashboard.git
cd park302-dashboard
```

### 2. 백엔드 설정

`backend/src/main/resources/application-local.properties` 파일을 직접 생성한다.
`.env.example`을 참고하여 DB 접속 정보와 JWT 시크릿을 입력한다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/park302?serverTimezone=Asia/Seoul
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password

jwt.secret=your-local-secret-key
jwt.expiration-ms=86400000

spring.jpa.show-sql=true
logging.level.com.park302=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

### 3. 백엔드 실행

```bash
cd backend
./gradlew bootRun
# 서버 기동 확인: http://localhost:8060
```

### 4. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
# 브라우저 접속: http://localhost:3010
```

---

## 환경변수

민감 정보는 코드에 하드코딩하지 않는다.
로컬 개발 시 `application-local.properties`에 직접 작성하고, 운영 배포 시 `.env` 파일로 주입한다.

| 변수명 | 설명 |
|---|---|
| `DB_HOST` | DB 호스트 |
| `DB_PORT` | DB 포트 (기본값: 3306) |
| `DB_NAME` | DB 스키마명 |
| `DB_USERNAME` | DB 사용자명 |
| `DB_PASSWORD` | DB 비밀번호 |
| `JWT_SECRET` | JWT 서명 키 |
| `JWT_EXPIRATION_MS` | JWT 만료 시간(ms, 기본값: 86400000) |

> 전체 목록은 `.env.example` 참고

---

## 포트

| 서비스 | 포트 |
|---|---|
| 백엔드 API | 8060 |
| 프론트엔드 (개발) | 3010 |
