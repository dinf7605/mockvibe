# ADR-009: 관측성 — Micrometer + Prometheus + 자체 도메인 지표

| | |
|---|---|
| **상태** | Accepted (자체 지표) · Phase 2 Grafana Cloud 외부화 검토 |
| **결정일** | 2026-05-15 (D16 PriceBroadcaster 지표 도입) |
| **관련** | [ADR-005 Circuit Breaker](ADR-005-circuit-breaker.md), [PriceBroadcaster.java](../../backend/src/main/java/com/fintech/simulator/market/websocket/PriceBroadcaster.java) |

## 1. 배경

부하 테스트 (D48) 직전까지는 "잘 동작한다"의 기준이 정성적이었다. 운영에서 다음 질문에 답하려면 **정량적 지표**가 필요:

- 시세 push 가 클라이언트에 몇 ms 만에 전달되나? (PRD §10 NFR-1: late SLA 100ms)
- DB connection pool 이 포화되나? (Hikari)
- Circuit Breaker가 몇 번 Open 됐나?
- AI 코치가 일일 한도를 얼마나 썼나?
- Flyway 마이그레이션이 얼마나 걸렸나?

## 2. 결정 — Spring Actuator + Micrometer Prometheus Registry + 자체 도메인 Counter/Timer

### 2.1 인프라

```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus    # 운영
  metrics:
    tags:
      application: ${spring.application.name}
```

- `/actuator/prometheus` → Prometheus scrape endpoint
- `/actuator/health` → ALB/Kubernetes readiness probe 호환 형식
- 운영에서 metrics endpoint 는 Nginx 사설망 IP 만 허용 권장 (현재는 공개)

### 2.2 자체 도메인 지표 — 4 영역

#### (a) PriceBroadcaster (시세 fan-out 성능)

```java
private final Counter pricesPublished;
private final Timer publishLatency;
private final Counter lateBroadcasts;        // > 100ms (NFR-1 위반)

public void publish(String ticker, Quote q) {
    long start = System.nanoTime();
    template.convertAndSend("/topic/price/" + ticker, q);
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
    pricesPublished.increment();
    publishLatency.record(Duration.ofMillis(elapsedMs));
    if (elapsedMs > 100) lateBroadcasts.increment();
}
```

→ 지표:
- `mockvibe_prices_published_total`
- `mockvibe_publish_latency_seconds{quantile="0.95"}`
- `mockvibe_late_broadcasts_total`

#### (b) 외부 API 사용량

```java
@CircuitBreaker(name = "claude")
public AiComment generate(String prompt) {
    var resp = geminiClient.call(prompt);
    aiTokensUsed.increment(resp.usageMetadata().totalTokenCount());
    return resp.toComment();
}
```

→ `mockvibe_ai_tokens_used_total`, `mockvibe_ai_daily_calls{user_id="..."}` (Redis 한도 추적)

#### (c) Flyway 적용 시간 + 결과
- Flyway 자체가 `flyway_schema_history.execution_time` 컬럼에 기록
- 부팅 시 로그로 출력 (D49 운영 부팅 로그에서 V1~V7 1.249초 확인)

#### (d) 자동 노출 지표 (코드 작성 0줄)

Spring Boot Actuator + Micrometer Resilience4j integration 이 자동:
- `hikaricp_*` (DB pool)
- `jvm_memory_*`, `jvm_gc_*`
- `tomcat_threads_*`
- `resilience4j_circuitbreaker_state{name=...}`
- `http_server_requests_seconds{uri=...,status=...}`

## 3. D48 부하 테스트에서 활용

```bash
# k6 스크립트 실행 중 별도 터미널에서
curl -s http://localhost:8080/actuator/prometheus | grep -E "http_server|hikaricp_connections|jvm_memory_used"
```

확인 결과:
- buy API p95: 32.72ms (NFR-1 24배 여유)
- Hikari 활성 connection: 평균 4 / max 8 (포화 안 함)
- JVM heap: 평균 280MB / max 480MB (Xmx 512m 직전, 좀 빠듯)
- CB 상태: 5 인스턴스 모두 CLOSED 유지

→ 부하 테스트가 **숫자로** 정량화됨. "잘 됨" 이 아니라 "p95 32.72ms".

## 4. 시각화 — 관리자 페이지

`/admin/system` 페이지에 핵심 지표 5개를 GUI:
- CB 5개 신호등 (🟢🟡🔴)
- 가격 push count (실시간 증가)
- AI 토큰 일일 누계
- DB pool 사용률
- 최근 1분 buy API p95

→ 면접 데모 시 별도 Grafana 띄울 필요 없이 **사이트 안에서** 운영 지표 확인 가능. 어필 강함.

## 5. Phase 2 — Grafana Cloud Free Tier 외부화

### 5.1 동기
- 자체 endpoint 는 backend 가 죽으면 같이 죽음 (관측성도 disposed)
- 30일 이상 metric retention 필요 (Prometheus 자체는 메모리/디스크 단명)
- 알림 (Slack/Email) 자동화 필요

### 5.2 전환 비용
- Grafana Cloud free: 14일 retention, 50GB metric storage, 50 alert. 비용 0
- backend 에 prometheus remote write 추가:
  ```yaml
  management.prometheus.metrics.export.pushgateway:
    enabled: true
    base-url: https://prometheus-prod-XX.grafana.net
    ...
  ```
- Grafana Cloud 대시보드 import (Spring Boot starter pack 있음)

총 30분 작업. D50 ~ Phase 후속.

## 6. 대안 비교

| 옵션 | 평가 |
|---|---|
| (A) **Micrometer + Prometheus** ← 선택 | Spring Boot 통합. 자동 노출 풍부. 자체 메트릭도 한 줄 |
| (B) Datadog | 강력하지만 무료 한도 매우 작음 (~5 host). 비용 발생 가능 |
| (C) New Relic | 무료 100GB/월. 강력하나 학습 곡선 |
| (D) CloudWatch | AWS 통합 강함. PromQL 못 씀. 우리는 Grafana 호환 우선 |
| (E) 자체 logging 만 | grep 으로 분석. p95 같은 통계 직접 계산. 비추 |

## 7. 트레이드오프

| 항목 | 비용 |
|---|---|
| `/actuator/prometheus` 가 공개 | 누구나 메트릭 조회 가능. 민감 정보(사용자 수 등) 노출. **운영에선 Nginx 단에서 사설망 IP 또는 Basic Auth 권장**. 현재는 데모 편의로 공개 |
| 도메인 메트릭 코드 boilerplate | 매 service에 Counter/Timer 주입. AOP 또는 @Timed 어노테이션으로 boilerplate 감소 가능 |
| Prometheus 자체 시계열 보관 (단명) | Grafana Cloud 또는 Thanos/Mimir로 외부화 필요 (Phase 2) |

## 8. 측정 지표 (Self-Reference)

이 ADR이 효과 있었는지의 지표:
- [x] D48 부하 테스트에서 p95 숫자 확보 (`32.72ms`)
- [x] D49 운영 부팅 시 CB 상태 + Hikari pool 실시간 확인
- [x] 관리자 페이지에 5개 지표 GUI
- [ ] Phase 2 Grafana Cloud 외부화 + 30일 retention
- [ ] Slack 알림 (CB Open / late SLA 위반 / AI 토큰 한도 임박)

## 9. 후속 ADR 후보
- ADR-012: 분산 추적 — OpenTelemetry + Tempo (KIS handshake → STOMP → 클라이언트 전 구간)
- ADR-013: 로그 — 파일 → Loki / CloudWatch

## 10. 참고
- 코드: [PriceBroadcaster](../../backend/src/main/java/com/fintech/simulator/market/websocket/PriceBroadcaster.java), [GeminiClient](../../backend/src/main/java/com/fintech/simulator/ai/GeminiClient.java)
- Micrometer: https://micrometer.io/
- Spring Boot Actuator: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- D48 부하 결과: [docs/perf/D48-load-test.md](../perf/D48-load-test.md)
