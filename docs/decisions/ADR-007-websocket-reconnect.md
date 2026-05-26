# ADR-007: WebSocket 재연결 — Exponential Backoff + Resilience4j CB 협력

| | |
|---|---|
| **상태** | Accepted |
| **결정일** | 2026-05-14 (D11~D14 외부 WebSocket 통합) |
| **관련** | [ADR-005 Circuit Breaker](ADR-005-circuit-breaker.md), [KisWebSocketClient.java](../../backend/src/main/java/com/fintech/simulator/market/provider/kis/KisWebSocketClient.java), [FinnhubWebSocketClient.java](../../backend/src/main/java/com/fintech/simulator/market/provider/finnhub/FinnhubWebSocketClient.java) |

## 1. 배경

KIS·Finnhub WebSocket은 끊어진다.

| 끊김 원인 | 빈도 | 자가 복구 가능? |
|---|---|---|
| 무료 티어 idle timeout (Finnhub ~30분) | 자주 | ✅ 재연결만 하면 됨 |
| KIS approval_key 만료 (30분) | 자주 | ✅ 키 재발급 + 재연결 |
| 외부 인프라 깜빡임 (라우팅) | 가끔 | ✅ 수초 내 복구 |
| 외부 API 점검 | 드뭄 | ⏸ 분~시간 단위, CB로 격리 |
| 자체 코드 버그로 send 폭주 → 강제 종료 | 드뭄 | ❌ 코드 수정 필요 |

운영에서 시세가 5분 이상 멈추면 대시보드 신뢰도 ↓. **자동 재연결**이 필수이지만, 무지성 즉시 재연결은:
- 외부 서버에 thundering herd
- 자체 코드 버그로 인한 끊김이면 무한 루프
- rate limit 가속 소진

## 2. 결정 — Exponential Backoff (1s → 2s → 4s → ... → 60s 상한)

### 2.1 알고리즘

```
attempt(n) → wait time
  attempt 0: connect 즉시
  attempt 1: 1s 대기
  attempt 2: 2s
  attempt 3: 4s
  attempt 4: 8s
  attempt 5: 16s
  attempt 6: 32s
  attempt 7+: 60s (상한)

성공 시 attempt = 0 으로 리셋
```

지수증가로 외부 서버에 부담↓, 60s 상한으로 영구 끊김 시에도 분당 1회 시도 (rate 최소화).

### 2.2 코드 — 단순화된 reconnect 루프

```java
@Component
public class KisWebSocketClient {

    private final AtomicInteger attempt = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler;
    private volatile WebSocketSession session;

    public void connect() {
        try {
            session = doConnect();    // 실제 connect
            attempt.set(0);            // 성공 → reset
            log.info("KIS WebSocket connected (attempt={})", attempt.get());
        } catch (Exception e) {
            scheduleReconnect(e);
        }
    }

    private void scheduleReconnect(Throwable cause) {
        int n = attempt.incrementAndGet();
        long delay = Math.min(60_000L, (1L << Math.min(n - 1, 6)) * 1000L);  // 1s..60s
        log.warn("KIS WebSocket connect failed, retry in {}ms (attempt={})", delay, n, cause);
        scheduler.schedule(this::connect, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onWebSocketClose(int code, String reason) {
        if (code != NORMAL_CLOSE) {
            scheduleReconnect(new IOException("server closed: " + code + " " + reason));
        }
    }
}
```

### 2.3 Circuit Breaker 와의 협력

`@CircuitBreaker(name = "kis-auth")` 가 OAuth/Approval Key 호출을 감싼다.

| 시나리오 | 동작 |
|---|---|
| KIS OAuth 401/403 5번 연속 실패 | CB Open → 30s wait → Half-Open 으로 3건 시도. WebSocket connect 도 OAuth 호출하므로 자동 지연. |
| WebSocket 단독 끊김 (인증 OK, 네트워크 깜빡임) | CB는 OAuth 정상이라 Closed 유지 → 우리 backoff 만 작동 → 1s → 2s ... 재연결 시도 |

→ **CB 는 인증/REST 호출 레이어, Backoff 는 WebSocket 레이어 책임 분리**.

## 3. 대안 비교

| 옵션 | 평가 |
|---|---|
| (A) 즉시 재연결 + 횟수 제한 | 단순. 외부 서버 부담 ↑. 깜빡임 시 thundering herd 가능 |
| (B) **Exponential backoff + 상한** ← 선택 | 외부 부담 ↓, 영구 끊김 시에도 분당 1회 cap |
| (C) Linear backoff (1s, 2s, 3s, ...) | exponential 보다 보수적. 단순 깜빡임 후 복구가 더 느림 |
| (D) Spring Retry `@Retryable` 단독 | Retry 는 동일 메서드 재호출용. WebSocket 같은 lifecycle 객체에는 부적합 |
| (E) Resilience4j Retry 모듈 | (D)와 유사. CB 와 같은 라이브러리지만 stateful WebSocket 에는 적용 어색 |

## 4. 트레이드오프

| 항목 | 비용 |
|---|---|
| **60초 상한이 길다** | 영구 끊김 시 1분 idle 가능. 그 사이 시세 stale. Mock fallback이 보완. |
| **Jitter 미적용** | 동일 시점에 여러 클라이언트가 동일 backoff → 동시 retry 가능. 우리는 backend 1대라 영향 X. 다중 인스턴스 시 ±20% jitter 추가 검토 |
| **attempt 카운터가 메모리** | 인스턴스 재시작 시 0부터 시작. 영구 attempt 추적 필요 시 Redis 또는 DB |

## 5. 검증

### 5.1 운영 (D49 부팅)
KIS WebSocket 첫 connect 시 `Cannot invoke "String.length()" because "this.input" is null` 경고 발생 (KIS API 의 일시적 응답 형식 이상) → 자동 backoff → 그 다음 시도에 정상 connect → seed 3종목(`005930`, `000660`, `035420`) 구독 성공.

→ **실제 운영에서 backoff 가 작동**한 사례.

### 5.2 모니터링
- `kis_websocket_reconnect_attempts_total` (Counter) — Micrometer
- `kis_websocket_connected` (Gauge 0/1)
- 관리자 페이지 시스템 탭에서 실시간 표시

## 6. 후속

- ADR-011 후보: Jitter 추가 + 멀티 인스턴스 동기 회피
- 외부 점검 알림 시 reconnect 일시 중단 (운영자 수동 제어 endpoint)

## 7. 참고
- 코드: [KisWebSocketClient](../../backend/src/main/java/com/fintech/simulator/market/provider/kis/KisWebSocketClient.java), [FinnhubWebSocketClient](../../backend/src/main/java/com/fintech/simulator/market/provider/finnhub/FinnhubWebSocketClient.java)
- AWS 권장 backoff: https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
