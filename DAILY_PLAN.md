# 📅 fintech-simulator 일일 작업 계획 (Daily Roadmap)

| 항목 | 값 |
|---|---|
| 기준 PRD | `PRD.md` v2.0 |
| 총 기간 | 10주 (50 영업일) |
| 일 단위 | 1일 = 명확히 완결되는 산출물 1개 (대략 3~5시간 분량) |
| 원칙 | ① 의존성 우선 ② 데모 가능 상태를 자주 만든다 ③ 어려운 통합은 Mock 먼저 |

> **빠른 데모 마일스톤**
> - **Day 10**: Mock 시세로 시장가 매매 + 포트폴리오 화면이 도는 상태
> - **Day 20**: 실시간 시세(KIS/Finnhub) + 환율 + 차트가 도는 상태
> - **Day 30**: 지정가·리스크 대시보드 완성
> - **Day 40**: 백테스트 + AI 코치 완성
> - **Day 50**: 관리자 페이지 + AWS 배포 완료

---

## 🗓️ Week 1 — Phase 1-A: 기반 환경 & 인증

| Day | 작업 | 산출물 |
|---|---|---|
| **D01** | 프로젝트 골격 | `backend/` (Spring Boot 3.x + Gradle), 패키지 스켈레톤(`config/auth/market/trading/portfolio/fx/ai/backtest/risk/admin/common`), `application-{local,prod}.yml` |
| **D02** | 인프라 & 프론트 골격 | `docker-compose.yml` (Oracle XE + Redis 기동), `frontend/` (Vite + React + TS + Zustand + axios + Pretendard), 라우팅 골격 |
| **D03** | DB 스키마 V1 | Flyway `V1__init.sql`: USERS(role/status 포함), WALLET, STOCKS, ORDERS, HOLDINGS, FX_RATES + 기본 인덱스 |
| **D04** | 회원가입 | DTO/Validation, BCrypt(cost 12), WALLET 자동 생성 + 시드머니 1,000만원, 통합 테스트 |
| **D05** | 로그인 + JWT | Access 15분 / Refresh 7일, Redis 블랙리스트, Refresh Rotation, `/auth/refresh`·`/auth/logout` |

---

## 🗓️ Week 2 — Phase 1-B: RBAC + Provider + 시장가 매매

| Day | 작업 | 산출물 |
|---|---|---|
| **D06** | RBAC 기반 | JWT `role` claim, Spring Security `@PreAuthorize`, 초기 관리자 시드 SQL(env 비밀번호 주입), 403/200 테스트 |
| **D07** | Provider 추상화 + Mock | `MarketDataProvider` 인터페이스, `MockMarketDataProvider` (랜덤워크 가격), `PriceCache` (ConcurrentHashMap), 가격 조회 API |
| **D08** | 종목 마스터 | STOCKS 시드 SQL: 한국 30 + 미국 30 (티커/종목명/섹터/통화), `/stocks/search`·`/stocks/{ticker}` |
| **D09** | 시장가 매수 | `TradingService.buyMarket()`, Wallet **비관적 락** + Holdings **낙관적 락**, ORDERS 기록 + 수수료 |
| **D10** | 시장가 매도 + 동시성 테스트 | `sellMarket()`, `ExecutorService` 10병렬 동시 매수 테스트 → 잔고 음수 0건 검증, **데모 1**: Mock 시세로 매매·포트폴리오 작동 |

---

## 🗓️ Week 3 — Phase 2: 실시간 시세 (백엔드)

| Day | 작업 | 산출물 |
|---|---|---|
| **D11** | STOMP 서버 | `WebSocketConfig`, `/ws` 엔드포인트, `/topic/price/{ticker}` 브로드캐스트 채널, Heartbeat 15초 |
| **D12** | KIS 인증 | OAuth 2.0 AppKey/Secret → AccessToken (24h 캐싱), REST 호출 래퍼, Token Bucket(초당 5건) |
| **D13** | KIS WebSocket | 실시간 체결가 수신 → `PriceCache` 적재 → `PriceUpdatedEvent` 발행, 41종목 한도 관리 |
| **D14** | Finnhub WebSocket | `wss://ws.finnhub.io` 구독/해제, 60 calls/min Bucket4j, 동적 구독(보는 종목만) |
| **D15** | 환율 | ExchangeRate-API 클라이언트, FX_RATES 1분 캐시, USD→KRW 환산 유틸 + 매매 시점 fx_rate ORDERS 기록 |

---

## 🗓️ Week 4 — Phase 2 마무리: 프론트 통합

| Day | 작업 | 산출물 |
|---|---|---|
| **D16** | Price Broadcaster | 캐시 갱신 → STOMP 즉시 릴레이 (외부 수신 후 100ms 이내), 부하 단위 테스트 |
| **D17** | 프론트 - 인증 | 로그인/회원가입 화면, 토큰 저장(메모리 + Refresh httpOnly 또는 secure), axios 인터셉터, 자동 재발급 |
| **D18** | 프론트 - 대시보드 | 총자산 카드, ApexCharts 도넛(자산 비중), 보유 종목 카드, 일별 추이 라인 차트 |
| **D19** | 프론트 - 종목 상세 | TradingView Lightweight 캔들, STOMP 클라이언트 + Exponential Backoff 재연결, 가격 깜빡임 애니메이션 |
| **D20** | 프론트 - 매매 패널 + 거래 내역 | 시장가 탭, Toss 키패드, 슬라이더, 거래 내역 페이지네이션 + 필터, **데모 2**: 실시간 시세 + 매매 |

---

## 🗓️ Week 5 — Phase 3: 지정가 + 정합성 + Circuit Breaker

| Day | 작업 | 산출물 |
|---|---|---|
| **D21** | 지정가 등록 | LIMIT_ORDERS 스키마 + `/orders/limit` 등록 API, 유효성(잔고/수량/가격/유효기한), 인덱스 `(ticker, status, target_price)` |
| **D22** | 이벤트 기반 체결 골격 | `LimitOrderProcessor` (`@EventListener`), `PriceUpdatedEvent` → 후보 조회 SQL (인덱스 사용) |
| **D23** | 체결 트랜잭션 | 락 순서 **Wallet → Holdings → Orders** 데드락 방지, 단일 트랜잭션 정합성, LIMIT_ORDERS.status 전이 |
| **D24** | 지정가 취소/만료 + 동시성 테스트 | 취소 API, 만료 배치 (00:00), 통합 동시성 테스트 (낙관적 락 충돌 재시도) |
| **D25** | Circuit Breaker | Resilience4j 설정(5회 실패 → Open → 30초 후 Half-Open), KIS/Finnhub/FX/Claude 적용, Mock 자동 전환 |

---

## 🗓️ Week 6 — Phase 4: 리스크 대시보드

| Day | 작업 | 산출물 |
|---|---|---|
| **D26** | PRICE_HISTORY 적재 | 스키마 + 일별 OHLC 배치 (KIS REST + Finnhub REST), 1년치 시드 |
| **D27** | VaR 계산기 | 95%/99% 1일 VaR (Historical Simulation), 단위 테스트 (수식 검증) |
| **D28** | Sharpe + Beta | Sharpe(1년), Beta vs KOSPI/S&P500, 무위험금리 입력 |
| **D29** | MDD + 집중도 | Max Drawdown, 섹터/지역 집중도 게이지 + 임계치 경고 |
| **D30** | 배치 + 프론트 | 매일 자정 리스크 배치, 리스크 대시보드 화면 (카드 4종 + 게이지 + 상관관계 히트맵), **데모 3**: 리스크 대시 완성 |

---

## 🗓️ Week 7 — Phase 5-A: 백테스트 엔진

| Day | 작업 | 산출물 |
|---|---|---|
| **D31** | 백테스트 도메인 + BuyHold | `BacktestEngine`, `Strategy` 인터페이스, `BuyAndHoldStrategy`, BACKTEST_RUNS 스키마 |
| **D32** | 엔진 코어 | In-Memory 시뮬레이션 루프, 일별 평가, 결과 지표(누적 수익률/MDD/샤프/거래횟수/승률) |
| **D33** | MA + RSI 전략 | `MovingAverageStrategy(20)`, `RsiStrategy(14)`, 신호 발생 시점 기록 |
| **D34** | 백테스트 API + 영구 저장 | `POST /backtest/run` (1년치 1종목 3초 이내 검증), 결과 JSON CLOB 저장, 비교 API |
| **D35** | 프론트 - 백테스트 | 전략·종목·기간 선택, TradingView 자산 곡선 + 매수/매도 마커, 결과 카드 |

---

## 🗓️ Week 8 — Phase 6-A: AI 코치 (Claude)

| Day | 작업 | 산출물 |
|---|---|---|
| **D36** | Claude 클라이언트 | `claude-haiku-4-5-20251001` SDK 연동, env 키 분리, 토큰 사용량 Micrometer 메트릭 |
| **D37** | PromptBuilder + 캐싱 | 시스템 프롬프트 + 종목 메타 **prompt caching**, 사용자 컨텍스트 동적 부분 |
| **D38** | 매매 직후 코멘트 | 매매 이벤트 리스너 → 한 줄 코멘트, 사용자당 일 10회 트리거 제한 (Redis 카운터) |
| **D39** | 주간 회고 리포트 | 매주 일요일 스케줄러, AI_REPORTS 저장, 주간 매매 패턴 분석 + 감정적 매매 감지(3일 연속 손절) |
| **D40** | 즉시 분석 + 응답 캐싱 + 프론트 | `POST /ai/analyze`, `(user_id, portfolio_hash)` 응답 캐싱, 거래 내역 인라인 코멘트 + 위클리 카드, **데모 4** |

---

## 🗓️ Week 9 — Phase 7-A: 관리자 페이지 (백엔드 + 일부 프론트)

| Day | 작업 | 산출물 |
|---|---|---|
| **D41** | 사용자 관리 API + step-up | 목록/상세/정지/시드머니/권한 변경, **비밀번호 재인증 → step-up 토큰**, ADMIN_AUDIT_LOGS 스키마 |
| **D42** | 감사 로그 AOP | `@Auditable` 어노테이션 + AOP, before/after JSON 자동 기록, IP/UA 기록, INSERT-only 보장 |
| **D43** | 종목 관리 + 시스템 운영 API | STOCKS CRUD, 활성 토글, Circuit Breaker 상태/Reset, 캐시 메트릭, Provider 강제 전환 |
| **D44** | 거래 모니터링 + AI 비용 + 공지 | 전체 거래 필터, 이상 거래 탐지(단시간 다수/대금액), AI 토큰 일/월 집계, ANNOUNCEMENTS CRUD |
| **D45** | 프론트 - 관리자 (1) | 관리자 레이아웃(좌측 메뉴), 사용자 화면 + 액션 모달(재인증 다이얼로그), 종목 화면 |

---

## 🗓️ Week 10 — Phase 7-B: 관리자 마무리 + 배포 + 문서

| Day | 작업 | 산출물 |
|---|---|---|
| **D46** | 프론트 - 관리자 (2) | 거래/시스템 대시보드(헬스 신호등)/AI 비용 차트/공지/감사 로그(diff 뷰) 화면 |
| **D47** | 관측성 + Docker Compose | Actuator + Micrometer + Prometheus 엔드포인트, 로그 MDC(requestId), Docker Compose 풀스택 한 줄 기동 |
| **D48** | 부하 테스트 + 튜닝 | k6 시나리오: 동시 50명 매매·시세 구독, p95 응답·시세 전파 측정, JVM/커넥션풀 튜닝 |
| **D49** ✅ | AWS 배포 + 자동화 | EC2 t3.micro + Nginx + Let's Encrypt + Swap 2GB + Oracle ADB Wallet mTLS + Vercel 프론트 + GitHub Actions 자동 배포 (`git push` 한 줄). **11건 운영 이슈 진단·해결** ([postmortem](docs/operations/d49-deployment-postmortem.md)) |
| **D50** 🟡 | 문서화 + 마무리 | README + 운영 URL/배지 + mermaid 다이어그램 4종 ✅ / ADR 9건 ✅ (목표 13) / Postmortem ✅ / 데모 스크립트 ✅ / 데모 GIF 영상 ⏳ |

---

---

## 🚀 D50 이후 후속 작업 후보 (Optional / Phase 8+)

| 영역 | 작업 | ADR |
|---|---|---|
| 보안 | SSH 22 → AWS SSM Session Manager + GitHub OIDC | [ADR-008](docs/decisions/ADR-008-ssh-access-policy.md) Phase 2 |
| 관측성 | Prometheus → Grafana Cloud free tier + Slack 알림 | [ADR-009](docs/decisions/ADR-009-observability.md) Phase 2 |
| 분산 추적 | OpenTelemetry + Tempo (KIS → STOMP → 클라이언트 전 구간) | ADR-012 (예정) |
| 알림 | Web Push (지정가 체결 / 리스크 임계 돌파) | ADR-013 (예정) |
| 도메인 ADR | JWT/RT Rotation · STOMP 시세 파이프 · AI 캐시 · 관리자 감사 AOP | ADR-010~011 (예정) |
| 트래픽 증대 시 | WebSocket을 `ws.<domain>` 서브도메인으로 분리하고 메인은 HTTP/2 복귀 | [ADR-003 §3](docs/decisions/ADR-003-operational-resilience.md) 메모 |

---

## 📌 작업 원칙 (매일 적용)

1. **그날 작업 시작 전**: 어제 작성한 코드를 한 번 빌드/테스트 → 그린 상태에서 시작
2. **그날 작업 끝**: 단위 테스트 1개 이상 + 커밋 메시지에 Day 번호 표기 (`[D07] Mock Provider + PriceCache`)
3. **Mock 우선**: 외부 API 통합이 막히면 Mock으로 우회 → 본 통합은 다음 날 재시도
4. **블로커 발생 시**: 즉시 다른 독립 작업으로 스위치 (예: KIS 발급 지연 → 프론트 작업 선행)
5. **데모 가능 상태 유지**: 매주 금요일 종료 시점 화면이 동작해야 함 (D05/D10/D15/D20/D25/D30/D35/D40/D45/D50)

---

## 🔁 우선순위 조정 가이드 (일정 압박 시)

| 상황 | 컷오프 대상 |
|---|---|
| 2일 이상 지연 | FR-2.7 호가창, FR-4.6 Watchlist, FR-8.2 사용자 DSL, FR-9.6 상관관계 히트맵 |
| 4일 이상 지연 | AI 주간 리포트 → 즉시 분석만 유지, 백테스트 전략 3종 → BuyHold + MA만 |
| 7일 이상 지연 | 관리자 페이지 P1 항목만 (사용자/거래 모니터링/감사 로그) |
