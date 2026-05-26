# ADR-005: 외부 API 회복력 — Resilience4j Circuit Breaker 5 인스턴스

| | |
|---|---|
| **상태** | Accepted |
| **결정일** | 2026-05-19 (D25 외부 API 안정화) |
| **관련** | PRD §10 NFR-3 (외부 API 장애 대응), [application.yml](../../backend/src/main/resources/application.yml) |

## 1. 배경

MockVibe는 4개의 외부 API에 의존한다.

| 외부 API | 장애 유형 | 비즈니스 영향 |
|---|---|---|
| KIS OAuth | 토큰 만료/거부 | 한국 시세 전체 중단 |
| KIS Approval Key | 30분 TTL 갱신 실패 | WebSocket 재구독 실패 |
| KIS REST (현재가) | 일일 호출 한도 | 시세 stale |
| ExchangeRate-API | 무료 티어 한도 | USD→KRW 환산 stale |
| Gemini | rate limit / quota | AI 코멘트 미생성 |

이 중 하나라도 죽으면 **시연 중 화면이 멈춘다**. 면접 데모 도중 외부 의존성 때문에 망가지는 건 치명적.

## 2. 결정 — 의존성별 독립 Circuit Breaker + Mock Fallback

### 2.1 5개 인스턴스 분리

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50          # 50% 이상 실패 → Open
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
    instances:
      kis-auth:     { base-config: default }
      kis-approval: { base-config: default }
      kis-rest:     { base-config: default }
      fx-rate:      { base-config: default }
      claude:       { base-config: default }
```

**왜 분리?** 한 API의 장애가 다른 정상 API를 막으면 안 됨. 예: Gemini 죽어도 KIS 시세는 계속 흘러야.

### 2.2 임계값 결정 근거

| 값 | 근거 |
|---|---|
| `failure-rate-threshold: 50%` | 외부 API 의 일시적 timeout은 흔하므로 너무 민감하면 false positive. 50%가 sweet spot |
| `wait-duration: 30s` | 외부 API 대부분의 일시 장애가 30초 내 복구 (Cloudflare, 라우팅 깜빡임 등) |
| `minimum-calls: 5` | 5건 미만은 통계 불충분 → 일찍 Open 차단 |
| `permitted-half-open: 3` | Half-Open 에서 3건 시도 후 결정. 외부 API 부담 최소 |

### 2.3 적용 — `@CircuitBreaker` 어노테이션

```java
@CircuitBreaker(name = "kis-rest", fallbackMethod = "fallbackPrice")
public BigDecimal getCurrentPrice(String ticker) {
    return kisRestClient.fetchPrice(ticker);   // 실 호출
}

private BigDecimal fallbackPrice(String ticker, Throwable t) {
    log.warn("CB open for kis-rest, fallback to cache or mock", t);
    return priceCache.get(ticker)              // 1순위: 마지막 캐시 값
        .orElseGet(() -> mockProvider.price(ticker));  // 2순위: Mock
}
```

### 2.4 Mock Fallback 전략

| 외부 API | Open 시 fallback |
|---|---|
| KIS REST | priceCache(최근 값) → MockMarketDataProvider(랜덤워크) |
| KIS Auth/Approval | 캐시된 토큰 그대로 사용. 만료 시 KIS WebSocket이 자동 끊김 → reconnect 루프 |
| ExchangeRate-API | FX_RATES 테이블의 최신 row (1분 캐시) |
| Gemini | "AI 코멘트 생성 실패 — 잠시 후 다시 시도해주세요" 사용자 메시지 |

→ **모든 화면이 데이터 없이도 동작 가능**. 시연 멈춤 0건 보장.

## 3. 모니터링

### 3.1 Micrometer 자동 노출

```
GET /actuator/prometheus

resilience4j_circuitbreaker_state{name="kis-rest",state="closed"} 1.0
resilience4j_circuitbreaker_state{name="kis-rest",state="open"}   0.0
resilience4j_circuitbreaker_calls_total{name="kis-rest",kind="successful"} 1234
resilience4j_circuitbreaker_calls_total{name="kis-rest",kind="failed"}     12
```

### 3.2 관리자 페이지 (Phase 7) — 신호등

`/admin/system` 페이지에 5 인스턴스를 색깔로:
- 🟢 CLOSED (정상)
- 🟡 HALF_OPEN (검증 중)
- 🔴 OPEN (장애 격리)

운영자가 한눈에 어느 외부 API가 죽었는지 확인.

### 3.3 `register-health-indicator: true`

`/actuator/health` 의 `details` 에 각 CB 상태 포함 → Kubernetes/ALB readiness probe에 즉시 활용 가능.

## 4. 대안 비교

| 옵션 | 평가 |
|---|---|
| (A) **Resilience4j CB** ← 선택 | Spring Boot 통합 우수, Micrometer 자동 노출, 어노테이션 기반 |
| (B) Hystrix | EOL (Netflix 유지보수 중단). 비추 |
| (C) Spring Cloud CircuitBreaker (Spring Retry 기반) | 추상화 레이어 추가. 단일 CB로는 Resilience4j 직접 사용이 간결 |
| (D) Bulkhead 만 사용 (thread isolation) | 격리만 되고 자가 차단 X. CB와 조합이 더 강력 |

Resilience4j 자체는 Bulkhead, RateLimiter, Retry, TimeLimiter 도 제공. 향후 필요 시 같은 라이브러리에서 확장.

## 5. 트레이드오프

| 항목 | 비용 |
|---|---|
| 5 인스턴스 별도 관리 | yaml 길어짐. base-config 상속으로 boilerplate 최소화 |
| Fallback 코드의 분기 | 모든 외부 호출 메서드에 fallbackMethod 명시 필요 — 잊으면 CB 효과 X |
| Open 상태에서 stale 데이터 사용 | 사용자가 모를 수 있음. UI 에 "시세 지연" 배지로 보완 (D50 todo) |

## 6. 검증

### 6.1 단위 — `CircuitBreakerIntegrationTest`
KIS REST mock 을 의도적으로 6번 실패 → CB Open 확인 → `fallbackMethod` 호출 검증.

### 6.2 운영 (D49 부팅 시점)
운영 부팅 직후 KIS WebSocket connect 실패 (`input is null` warning) → CB가 자동 차단 → Finnhub WebSocket은 정상 → 미국 시세만 흘러도 사이트 동작.

→ **실제 운영에서 CB가 단일 외부 API 장애를 격리한 사례** 확인.

## 7. 후속 ADR 후보

- ADR-007: WebSocket reconnect 정책 (KIS/Finnhub 끊김 시 exponential backoff)
- ADR-009: 관측성 — CB 상태 변화 alert (Slack/Email)
- ADR-010: 외부 API SLA 측정 — Micrometer 분포 + p95 추적

## 8. 참고
- Resilience4j docs: https://resilience4j.readme.io/
- PRD §10 NFR-3: "외부 API 장애 시에도 데모 멈춤 0건"
- D49 운영 부팅 로그에서 CB 동작 확인: `KIS WebSocket connect failed → fallback`
