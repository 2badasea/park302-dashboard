# JWT (JSON Web Token) 학습 정리

## 목차
1. [사전 지식](#1-사전-지식)
2. [JWT란 무엇인가](#2-jwt란-무엇인가)
3. [구조 분석](#3-구조-분석)
4. [동작 원리](#4-동작-원리)
5. [예시 코드 (JJWT 0.12.x)](#5-예시-코드-jjwt-012x)
6. [우리 프로젝트에서 활용하는 방식](#6-우리-프로젝트에서-활용하는-방식)
7. [특징 · 장단점](#7-특징--장단점)
8. [보안 취약점 및 주의사항](#8-보안-취약점-및-주의사항)
9. [보완재 · 함께 알아두면 좋은 것](#9-보완재--함께-알아두면-좋은-것)

---

## 1. 사전 지식

### Base64URL 인코딩
- JWT의 각 파트는 Base64URL로 인코딩된다.
- Base64와 유사하지만 URL-safe 문자(+→-, /→_, = 패딩 생략)를 사용한다.
- **암호화가 아니다.** 누구나 디코딩해서 내용을 볼 수 있다.

### HMAC (Hash-based Message Authentication Code)
- 비밀 키와 메시지를 합쳐 해시를 생성하는 알고리즘.
- JWT에서 서명(Signature) 생성에 사용되는 방식(HS256, HS384, HS512).
- **대칭 방식**: 서명 생성과 검증에 동일한 비밀 키를 사용한다.
- 키를 아는 사람만 서명을 생성하거나 검증할 수 있다.

### RSA / ECDSA (비대칭 방식)
- 공개키(public key)로 서명 검증, 개인키(private key)로 서명 생성.
- 여러 서비스가 토큰을 검증해야 하는 분산 환경에서 유리하다.
- HMAC보다 연산 비용이 높다.

---

## 2. JWT란 무엇인가

JWT(JSON Web Token)는 **당사자 간에 정보를 안전하게 전달하기 위한 자기완결형(self-contained) 토큰 표준(RFC 7519)** 이다.

"자기완결형"의 의미: 서버가 토큰을 검증할 때 DB를 조회하지 않아도 된다.
토큰 자체에 사용자 식별 정보와 유효성 검증에 필요한 서명이 담겨 있다.

### 왜 JWT를 쓰는가?

| 방식 | 방식 설명 | 단점 |
|---|---|---|
| 세션(Session) | 서버가 상태를 저장. 클라이언트는 세션 ID만 보유 | 서버 메모리 사용, 수평 확장 시 세션 공유 필요 |
| JWT | 토큰 자체에 정보 내장. 서버는 서명만 검증 | 토큰 탈취 시 즉각 무효화 어려움 |

Spring Security + JWT 조합은 **무상태(stateless) REST API** 설계의 표준 패턴이다.

---

## 3. 구조 분석

JWT는 점(`.`)으로 구분된 세 파트로 구성된다.

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
.
eyJzdWIiOiJhZG1pbiIsImlhdCI6MTcxMTU4MDgwMCwiZXhwIjoxNzExNjY3MjAwfQ
.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

### Header (헤더)
```json
{
  "alg": "HS256",   // 서명 알고리즘
  "typ": "JWT"      // 토큰 타입
}
```
- `alg`: HS256(HMAC + SHA-256), RS256(RSA), ES256(ECDSA) 등

### Payload (페이로드)
```json
{
  "sub": "admin",        // subject: 토큰 주체 (사용자 식별자)
  "iat": 1711580800,     // issued at: 발급 시각 (Unix timestamp)
  "exp": 1711667200      // expiration: 만료 시각
}
```
- **Claim**: 페이로드에 담기는 각 키-값 쌍을 "클레임"이라 부른다.
- **등록 클레임(Registered Claims)**: `sub`, `iat`, `exp`, `iss`, `aud` 등 표준 정의.
- **공개 클레임(Public Claims)**: 충돌 없이 사용 가능한 URI 형태.
- **비공개 클레임(Private Claims)**: 서비스 간 합의한 커스텀 데이터. 예: `"role": "ADMIN"`.
- **주의**: 페이로드는 Base64URL로 인코딩만 되며 암호화되지 않는다.
  비밀번호, 개인정보 등 민감 데이터는 절대 담지 말 것.

### Signature (서명)
```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secretKey
)
```
- Header + Payload를 비밀 키로 서명한 값.
- **서명의 역할**: 토큰이 위변조되지 않았음을 검증한다.
  Payload를 수정하면 서명이 달라지므로 서버가 변조를 감지한다.

---

## 4. 동작 원리

```
[로그인 흐름]

클라이언트                          서버
    │                                 │
    │── POST /api/auth/login ────────►│
    │   { username, password }        │
    │                                 │  1. 비밀번호 검증 (BCrypt.matches)
    │                                 │  2. JWT 생성 (JwtUtil.generate)
    │◄─ 200 { token: "eyJ..." } ──────│
    │                                 │

[인증이 필요한 API 호출 흐름]

클라이언트                          서버
    │                                 │
    │── GET /api/works ──────────────►│
    │   Authorization: Bearer eyJ...  │
    │                                 │  1. JwtAuthenticationFilter 동작
    │                                 │  2. 토큰 추출 및 서명 검증
    │                                 │  3. 만료 여부 확인
    │                                 │  4. SecurityContext에 Authentication 저장
    │                                 │  5. 컨트롤러 실행
    │◄─ 200 { data: [...] } ──────────│
    │                                 │
```

---

## 5. 예시 코드 (JJWT 0.12.x)

### 의존성 (build.gradle)
```groovy
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly    'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly    'io.jsonwebtoken:jjwt-jackson:0.12.6'
```

> **주의**: 0.11.x와 0.12.x는 API가 다르다.
> 0.12.x에서는 `Jwts.parserBuilder()` 대신 `Jwts.parser()`.
> `signWith(key, algorithm)` 대신 `signWith(key)` (알고리즘 자동 결정).

### 키 생성
```java
// 비밀 키는 HS256 기준 최소 32바이트(256비트) 이상이어야 한다.
SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
```

### 토큰 생성
```java
String token = Jwts.builder()
    .subject(username)             // sub 클레임
    .issuedAt(new Date())          // iat 클레임
    .expiration(new Date(System.currentTimeMillis() + expirationMs)) // exp 클레임
    .signWith(secretKey)           // 서명 (HS256 자동 선택)
    .compact();
```

### 토큰 검증 및 파싱
```java
try {
    Claims claims = Jwts.parser()
        .verifyWith(secretKey)         // 서명 검증
        .build()
        .parseSignedClaims(token)
        .getPayload();

    String username = claims.getSubject();
    Date expiration = claims.getExpiration();

} catch (ExpiredJwtException e) {
    // 만료된 토큰
} catch (JwtException e) {
    // 서명 불일치, 형식 오류 등
}
```

### OncePerRequestFilter 패턴
```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain chain) throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (header != null && header.startsWith("Bearer ")) {
        String token = header.substring(7);
        try {
            String username = jwtUtil.validateAndExtract(token);
            // SecurityContext에 인증 정보 등록
            var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException e) {
            // 유효하지 않은 토큰 — 인증 없이 통과 (이후 Security가 403 처리)
        }
    }

    chain.doFilter(request, response);
}
```

---

## 6. 우리 프로젝트에서 활용하는 방식

### 인증 구조

```
요청 경로                인증 방식              필터
/api/auth/login         없음 (permitAll)        -
/api/external/**        X-Api-Key 헤더          ApiKeyAuthFilter
/api/**                 Authorization: Bearer   JwtAuthenticationFilter
```

### 단일 관리자 계정

DB에 사용자 테이블이 없다. 관리자 계정은 환경변수로 관리한다.

```properties
# application.properties
admin.username=${ADMIN_USERNAME:admin}
admin.password-hash=${ADMIN_PASSWORD_HASH}
```

```java
// AuthController.java
// BCrypt 해시 비교 — 비밀번호를 평문으로 저장하지 않는다.
if (!passwordEncoder.matches(req.password(), storedHash)) {
    throw new UnauthorizedException("비밀번호가 올바르지 않습니다.");
}
String token = jwtUtil.generate(adminUsername);
```

### BCrypt 해시 생성 방법 (CLI)
```bash
# htpasswd 사용 (Apache 유틸, macOS/Linux 기본 제공)
htpasswd -nbB -C 10 admin mypassword | cut -d: -f2

# Python 사용 (어디서든 가능)
python3 -c "import bcrypt; print(bcrypt.hashpw(b'mypassword', bcrypt.gensalt(10)).decode())"
```

### 관련 파일 구조

```
config/
  JwtUtil.java                  # 토큰 생성 / 검증
  JwtAuthenticationFilter.java  # Bearer 토큰 추출 및 SecurityContext 등록
  ApiKeyAuthFilter.java         # X-Api-Key → AgentPrincipal (외부 API 전용)
  SecurityConfig.java           # 필터 체인 구성, 경로별 인가
controller/
  AuthController.java           # POST /api/auth/login
```

### 토큰 만료 시간

```properties
jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}  # 기본 24시간
```

현재는 Refresh Token 없이 Access Token만 발급한다.
1인 운영 대시보드이므로 만료 시 재로그인 처리.

---

## 7. 특징 · 장단점

### 장점

| 특징 | 설명 |
|---|---|
| **무상태(Stateless)** | 서버가 세션을 저장하지 않아도 됨. DB/Redis 조회 불필요 |
| **자기완결형** | 토큰에 사용자 정보 포함. 마이크로서비스 간 공유 용이 |
| **표준** | RFC 7519 표준. 언어/플랫폼 무관 |
| **확장성** | 수평 확장(Scale Out) 시 세션 공유 문제 없음 |

### 단점

| 문제 | 설명 |
|---|---|
| **즉각 무효화 불가** | 발급된 토큰은 만료 전까지 서버가 취소할 수 없음 |
| **토큰 크기** | 쿠키/세션 ID보다 크다. 매 요청마다 헤더에 포함됨 |
| **Payload 노출** | Base64 디코딩으로 누구나 내용을 볼 수 있음 |
| **비밀 키 단일 장애점** | 키 유출 시 모든 토큰이 위조 가능 |

---

## 8. 보안 취약점 및 주의사항

### alg:none 공격
초기 JWT 구현체 일부는 `"alg":"none"` 헤더를 허용해 서명 없이 통과시켰다.
JJWT 0.12.x는 이를 차단한다. 하지만 `verifyWith(secretKey)`를 반드시 명시해야 한다.

### 약한 비밀 키
HS256 기준 최소 256비트(32바이트) 이상 랜덤 문자열이어야 한다.
```bash
# 안전한 키 생성
openssl rand -base64 32
```

### 토큰 저장 위치 (XSS vs CSRF)

| 저장 위치 | XSS 취약 | CSRF 취약 | 비고 |
|---|---|---|---|
| localStorage | **O** (JS 접근 가능) | X | SPA에서 많이 씀. XSS 주의 |
| sessionStorage | **O** | X | 탭 닫으면 삭제 |
| HttpOnly Cookie | X | **O** | CSRF 대응 추가 필요 |
| Memory (JS 변수) | X | X | 새로고침 시 소멸 |

우리 프로젝트(1인 관리자 대시보드): localStorage가 실용적이나 XSS 방어(Content-Security-Policy) 함께 고려.

### 토큰 탈취 시 대응

JWT는 기본적으로 서버에서 개별 토큰을 무효화할 수 없다.
대응 방법:
1. **짧은 만료 시간**: 1시간 이하의 Access Token + Refresh Token 패턴
2. **블랙리스트**: 로그아웃/강제 만료가 필요한 토큰을 Redis에 저장해 조회
3. **버전 번호**: DB에 tokenVersion을 두고 토큰의 버전이 맞지 않으면 거부

---

## 9. 보완재 · 함께 알아두면 좋은 것

### Refresh Token 패턴

```
Access Token:  짧은 만료 (15분~1시간). API 인증에 사용.
Refresh Token: 긴 만료 (7일~30일). Access Token 재발급에만 사용.
               HttpOnly Cookie에 저장해 XSS 차단.
```

1인 대시보드 현재 구조는 Access Token만 사용하므로 필요 시 추가.

### OAuth 2.0 / OIDC

- **OAuth 2.0**: 권한 위임 프로토콜. "Google로 로그인" 같은 제3자 인증의 기반.
- **OIDC (OpenID Connect)**: OAuth 2.0 위에 "신원 확인" 계층 추가. JWT를 ID 토큰으로 사용.
- 우리 프로젝트는 자체 인증이므로 직접 관계는 없지만, 나중에 외부 인증 연동 시 필요.

### JWE (JSON Web Encryption)

- JWT는 서명만 하고 암호화는 하지 않는다 (JWS).
- 페이로드까지 암호화하려면 JWE를 사용한다.
- Payload에 민감 정보를 넣어야 한다면 고려. 일반적으로는 필요 없음.

### 토큰 검사 도구

- [jwt.io](https://jwt.io): 토큰을 붙여넣으면 Header/Payload를 디코딩해 보여줌. 서명 검증도 가능.
  개발 중 디버깅에 유용하지만, **운영 토큰을 외부 사이트에 붙여넣지 말 것.**
