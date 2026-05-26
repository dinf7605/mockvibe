# Architecture Decision Records (ADR)

각 결정의 *왜 그렇게 선택했나* 를 짧고 명확하게 기록. 대안 비교 + 트레이드오프 + 후속 ADR 후보 포함.

| # | 제목 | 영역 | 한 줄 |
|---|---|---|---|
| [001](ADR-001-data-model.md) | 데이터 모델 V1 | DB | V1에 핵심 6개만, 나머지는 Phase별 V2~ 점진 추가 |
| [002](ADR-002-deployment-topology.md) | 배포 토폴로지 | Infra | EC2 + docker compose + Nginx + Oracle ADB + Vercel |
| [003](ADR-003-operational-resilience.md) | 운영 회복력 | Ops | D49 11건 이슈 → 5가지 영구 정책 (oraclepki + Flyway + nginx + lifecycle + 자동배포) |
| [004](ADR-004-concurrency-locking.md) | 동시성 락 하이브리드 | Domain | Wallet 비관락 + Holdings 낙관락 + 락 순서로 deadlock 회피 |
| [005](ADR-005-circuit-breaker.md) | Circuit Breaker | Resilience | Resilience4j 5 인스턴스 + Mock fallback |
| [006](ADR-006-provider-abstraction.md) | Provider 추상화 | Domain | MarketDataProvider 인터페이스 + @ConditionalOnProperty 가드 |
| [007](ADR-007-websocket-reconnect.md) | WebSocket 재연결 | Resilience | Exponential backoff 1s → 60s + CB와 협력 |
| [008](ADR-008-ssh-access-policy.md) | SSH 접근 정책 | Security | 0.0.0.0/0 + 키 인증 (Phase 1) → AWS SSM (Phase 2) |
| [009](ADR-009-observability.md) | 관측성 | Ops | Micrometer + Prometheus + 자체 도메인 지표 + 관리자 신호등 |

## 후속 ADR 후보

| # | 제목 | 우선순위 |
|---|---|---|
| 010 | JWT + RT Rotation + Blacklist 설계 | Med |
| 011 | STOMP 시세 파이프 + Micrometer | Med |
| 012 | 분산 추적 (OpenTelemetry + Tempo) | Low |
| 013 | 로그 (파일 → Loki / CloudWatch) | Low |

## ADR 형식

모든 ADR은 다음 구조:

```
1. 결정 사항    — 무엇을
2. 배경/문제   — 왜 (선택지가 있었는지)
3. 선택 이유    — 비교 + 근거
4. 트레이드오프 — 수용한 단점 + 보완
5. 측정 지표   — 효과 정량화
6. 후속 ADR 후보 — 미해결 / 향후 전환
7. 참고       — 코드 / 외부 문서 링크
```

이 형식이 면접에서 "어떤 결정을 했나" → "왜 그렇게 했나" → "다른 옵션은?" 흐름과 자연스럽게 매칭.
