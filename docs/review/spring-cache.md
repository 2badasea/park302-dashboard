# Spring Cache 학습 정리

## 목차
1. [Spring Cache 추상화 개념](#1-spring-cache-추상화-개념)
2. [핵심 어노테이션](#2-핵심-어노테이션)
3. [ConcurrentHashMap (in-memory) 캐시](#3-concurrenthashmap-in-memory-캐시)
4. [우리 프로젝트에서의 동작 — 새 업체 등록 시 캐시는?](#4-우리-프로젝트에서의-동작--새-업체-등록-시-캐시는)
5. [캐시 생명주기 정리](#5-캐시-생명주기-정리)
6. [한계점 및 주의사항](#6-한계점-및-주의사항)
7. [Redis로 전환 시 차이점](#7-redis로-전환-시-차이점)
8. [캐시 전략 개념](#8-캐시-전략-개념)

---

## 1. Spring Cache 추상화 개념

Spring Cache는 **캐시 구현체를 교체해도 비즈니스 코드를 바꾸지 않도록** 추상화된 레이어다.

```
비즈니스 코드 (@Cacheable 등 어노테이션)
        │
Spring Cache 추상화 (CacheManager 인터페이스)
        │
구현체: ConcurrentHashMap (기본) / Redis / Ehcache / Caffeine 등
```

`@EnableCaching` 하나로 활성화되며, 별도 빈 설정 없이 기본 구현체인
`SimpleCacheManager` (내부적으로 `ConcurrentHashMap` 사용)가 자동 등록된다.

---

## 2. 핵심 어노테이션

### @Cacheable — 캐시에서 읽기 (없으면 실행 후 저장)

```java
@Cacheable(value = "agentByApiKey", key = "#apiKey")
public Agent findByApiKey(String apiKey) {
    return agentRepository.findByApiKey(apiKey)
        .orElseThrow(() -> new UnauthorizedException("유효하지 않은 API 키입니다."));
}
```

**동작 순서:**
1. `agentByApiKey` 캐시에서 키 `apiKey`로 조회
2. **캐시 HIT**: 메서드 실행 없이 캐시 값 반환
3. **캐시 MISS**: 메서드 실행 → 결과를 캐시에 저장 → 반환

### @CacheEvict — 캐시 삭제

```java
// 특정 키만 삭제
@CacheEvict(value = "agentByApiKey", key = "#apiKey")
public void deleteByApiKey(String apiKey) { ... }

// 해당 캐시 전체 삭제
@CacheEvict(value = "agentByApiKey", allEntries = true)
public ResMessage<AgentDTO.DetailResponse> update(Long id, AgentDTO.UpdateRequest req) { ... }
```

- `allEntries = true`: 캐시 내 모든 항목을 지운다.
- 기본 동작: 메서드 실행 **후** 캐시 삭제.
- `beforeInvocation = true`로 바꾸면 실행 **전** 삭제 (예외 발생 시에도 무조건 삭제).

### @CachePut — 항상 실행 후 캐시 갱신 (MISS 없이 최신화)

```java
@CachePut(value = "agentByApiKey", key = "#result.apiKey")
public Agent refreshCache(String apiKey) {
    return agentRepository.findByApiKey(apiKey).orElseThrow(...);
}
```

- `@Cacheable`과 달리 캐시에 있어도 메서드를 항상 실행한다.
- 결과를 캐시에 덮어 씌운다.

---

## 3. ConcurrentHashMap (in-memory) 캐시

### 구조

```
JVM 힙 메모리
  └─ CacheManager
       └─ Cache "agentByApiKey"  (ConcurrentHashMap)
            ├─ "cali-local-api" → Agent{ id=1, name="cali", ... }
            ├─ "cali-dev-api"   → Agent{ id=1, name="cali", ... }
            └─ ...
```

### 특징

| 항목 | 내용 |
|---|---|
| 저장 위치 | JVM 힙 메모리 |
| TTL (만료) | **없음** (직접 설정 불가. 항목은 명시적 evict 또는 서버 재시작 전까지 유지) |
| 최대 항목 수 제한 | **없음** (메모리 가득 찰 때까지 무한 증가) |
| 서버 재시작 시 | **모두 사라짐** (휘발성) |
| 분산 환경 | 각 서버가 독립된 캐시를 가짐 → 서버 간 불일치 가능 |
| 스레드 안전성 | `ConcurrentHashMap` — 멀티스레드 동시 접근 안전 |

---

## 4. 우리 프로젝트에서의 동작 — 새 업체 등록 시 캐시는?

> **핵심 질문**: 새 업체를 등록하면 캐시는 언제 최신화되나?
> 서버를 재시작하지 않아도 새 업체 API 키가 인식되나?

### 결론: **서버 재시작 불필요. 자동으로 동작한다.**

이유를 단계별로 살펴보자.

```
[새 업체 등록 시나리오]

1. 대시보드에서 cali-prod 업체 등록
   → AgentService.create() 호출
   → DB에 INSERT
   → @CacheEvict 없음 → 기존 캐시 그대로 유지

2. cali-prod 서버가 X-Api-Key: cali-prod-api 로 요청을 보냄
   → ApiKeyAuthFilter.findByApiKey("cali-prod-api") 호출
   → @Cacheable → 캐시 조회

3. 캐시에 "cali-prod-api" 키가 없음 (MISS)
   → DB 조회 실행
   → Agent{ id=2, name="cali-prod", ... } 를 DB에서 가져옴
   → 결과를 캐시에 저장

4. 이후 같은 API 키로 요청이 오면
   → 캐시 HIT → DB 조회 없이 바로 반환
```

**요약:** 새 업체는 처음 API 키로 요청이 들어오는 순간 자동으로 캐시에 등록된다.
등록 시점에 캐시를 갱신할 필요가 없다. 없으면 DB에서 가져오고, 이후부터 캐시를 쓴다.

### 업체 수정/삭제 시 시나리오

```
[업체 apiKey 변경 시나리오]

1. 대시보드에서 cali-local 업체의 apiKey를 "cali-local-v2"로 변경
   → AgentService.update() 호출
   → DB 업데이트
   → @CacheEvict(allEntries = true) → 캐시 전체 삭제!

2. 이후 첫 요청에서 각 API 키가 다시 DB에서 캐시로 로딩됨
   → 구 키 "cali-local-api" 로 요청이 오면 DB 조회 → 없으므로 401
   → 신 키 "cali-local-v2" 로 요청이 오면 DB 조회 → 캐시에 저장 → 200
```

`allEntries = true`를 쓰는 이유: apiKey가 바뀌면 구 키 항목을 정확히 특정하기 어렵기 때문.
전체를 지우고 다음 요청 때 재로딩하는 것이 가장 안전하다.

---

## 5. 캐시 생명주기 정리

| 이벤트 | 캐시 동작 |
|---|---|
| 새 업체 등록 | 아무 변화 없음. 첫 API 요청 시 자동 로딩(MISS → DB → 캐시 저장) |
| 업체 정보 수정 | `@CacheEvict(allEntries=true)` → 전체 삭제 → 다음 요청 시 재로딩 |
| 업체 삭제 | `@CacheEvict(allEntries=true)` → 전체 삭제 |
| 서버 재시작 | **전체 초기화** (in-memory이므로) |
| TTL 만료 | **없음** (기본 ConcurrentHashMap은 TTL 미지원) |

---

## 6. 한계점 및 주의사항

### TTL이 없다

한번 캐시에 올라간 항목은 명시적으로 evict하거나 서버를 재시작하기 전까지 메모리에 남는다.
업체 수가 적고 변경이 드문 우리 프로젝트에서는 문제가 없다.
하지만 항목이 무한정 쌓이는 데이터에는 적합하지 않다.

### 메모리 무제한

`ConcurrentHashMap` 캐시는 크기 제한이 없다.
항목 수가 급증하면 OOM(Out of Memory) 위험이 있다.
→ 사이즈가 커질 가능성이 있다면 Caffeine(`maximumSize`, `expireAfterWrite` 설정 가능)으로 전환 권장.

### 멀티 인스턴스 (분산 서버)

서버 A와 서버 B가 각각 독립된 캐시를 가진다.
서버 A에서 업체를 수정해 캐시를 evict해도, 서버 B의 캐시는 여전히 구버전.
→ 현재는 단일 서버이므로 문제 없음. 수평 확장 시 Redis 등 분산 캐시로 전환 필요.

### 서버 재시작 시 캐시 워밍업 없음

재시작 직후 처음 요청들은 캐시 MISS → 모두 DB 조회.
트래픽이 많은 시스템에서는 재시작 직후 DB 부하 급증("Thundering Herd") 문제가 생길 수 있다.
→ 현재 규모에서는 무관.

---

## 7. Redis로 전환 시 차이점

나중에 분산 환경 또는 TTL/영속성이 필요해지면 Redis로 전환한다.

### 의존성 변경
```groovy
// 기존
implementation 'org.springframework.boot:spring-boot-starter-cache'

// Redis 추가
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
// (spring-boot-starter-cache는 data-redis에 포함)
```

### 설정 변경
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1시간 (ms)
```

### 비즈니스 코드는 변경 없음

`@Cacheable`, `@CacheEvict` 어노테이션은 그대로 유지된다.
Spring Cache 추상화 덕분에 구현체만 교체되고 코드는 동일하다.

### Redis vs ConcurrentHashMap 비교

| 항목 | ConcurrentHashMap | Redis |
|---|---|---|
| TTL | X | O (항목별 설정 가능) |
| 서버 재시작 후 유지 | X | O (AOF/RDB 설정 시) |
| 분산 서버 공유 | X | O |
| 사이즈 제한 | X (무제한) | O (maxmemory 정책) |
| 설정 복잡도 | 매우 낮음 | 보통 |
| 인프라 추가 필요 | X | O (Redis 서버) |

---

## 8. 캐시 전략 개념

### Cache-Aside (Lazy Loading) — 우리가 사용하는 방식

```
1. 캐시 조회
2. MISS이면 DB 조회
3. DB 결과를 캐시에 저장
4. 반환
```
`@Cacheable`이 이 패턴을 구현한다. 실제로 사용되는 데이터만 캐시에 올라오는 장점이 있다.

### Write-Through

쓰기 작업 시 캐시와 DB를 동시에 업데이트한다.
`@CachePut`으로 구현 가능. 캐시와 DB 동기화가 보장된다.

### Write-Behind (Write-Back)

먼저 캐시에 쓰고, 비동기로 DB에 반영한다.
쓰기 성능은 높지만 데이터 유실 위험이 있다. Spring Cache 기본 지원 없음.

### Cache Eviction 정책 (주로 Redis/Caffeine에서 설정)

| 정책 | 설명 |
|---|---|
| LRU (Least Recently Used) | 가장 오래 사용 안 한 항목 삭제 |
| LFU (Least Frequently Used) | 사용 빈도가 낮은 항목 삭제 |
| TTL | 일정 시간 후 자동 만료 |
| allKeys-random | 무작위 삭제 |
