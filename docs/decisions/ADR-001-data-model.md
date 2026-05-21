# ADR-001: 데이터 모델 V1 설계

| | |
|---|---|
| **상태** | Accepted |
| **결정일** | 2026-05-20 (D03) |
| **관련** | PRD §8 데이터 모델, [V1__init_schema.sql](../../backend/src/main/resources/db/migration/V1__init_schema.sql) |

## 1. 결정 사항

V1 마이그레이션에서는 **핵심 매매 흐름에 필요한 6개 테이블만** 생성한다.

| 테이블 | 역할 |
|---|---|
| `USERS` | 회원, 권한(`role`), 정지 상태(`status`) |
| `WALLET` | 예수금 KRW 단일 지갑 (1 user : 1 wallet) |
| `STOCKS` | 종목 마스터 (한·미 60개) |
| `HOLDINGS` | 보유 종목 + 평균단가 + 낙관적 락 `version` |
| `ORDERS` | 체결된 거래 내역 (불변) |
| `FX_RATES` | 환율 시계열 |

확장 테이블(`LIMIT_ORDERS`, `PRICE_HISTORY`, `BACKTEST_RUNS`, `AI_REPORTS`, `ADMIN_AUDIT_LOGS`, `ANNOUNCEMENTS`, `WATCHLIST`)은 **해당 기능을 구현하는 Phase**에서 V2~ 마이그레이션으로 점진 추가한다.

## 2. 배경 / 문제

PRD §8 ERD에는 13개 테이블이 있다. 두 가지 옵션:
- **(A)** V1에서 13개 모두 생성
- **(B)** V1은 핵심 6개만, 나머지는 Phase별로 V2~ 점진 추가

## 3. 선택 — (B) 점진적 마이그레이션

### 이유
1. **운영의 사실성 어필**: 실제 서비스는 첫 릴리스에 모든 스키마를 동시에 깔지 않는다. Phase별 V2~ 추가는 마이그레이션 관리 역량을 보여준다.
2. **롤백 단위 명확화**: AI 코치(D36~)에서 문제가 생기면 V6 다운만으로 격리 가능. 한 덩어리면 무엇이 깨졌는지 추적이 어렵다.
3. **리뷰어 인지부하 감소**: 면접에서 ADR을 설명할 때, "지금 단계에 필요한 것만" 보여줄 수 있다.

### 트레이드오프
- 단점: 마이그레이션 파일이 7~8개로 늘어남
- 보완: 파일명에 Phase·기능 명시 (`V3__limit_orders.sql`, `V6__ai_reports.sql`)

## 4. 세부 설계 결정

### 4.1 `USERS.user_id`는 `VARCHAR2(50)` UUID
- 시퀀스 PK 대신 UUID → 외부 노출 시 추측 불가, 분산 가능
- 회원가입 시 애플리케이션에서 `UUID.randomUUID()` 생성

### 4.2 `HOLDINGS.average_price_krw` — 미국 종목도 KRW 환산
- 평가손익을 KRW 단일 통화로 일관 계산하기 위함
- 매매 시점 `fx_rate`를 `ORDERS`에 기록 → 추후 재계산 가능

### 4.3 락 전략은 컬럼 수준에서 준비
- `WALLET`: 별도 컬럼 없음. `@Lock(PESSIMISTIC_WRITE)`로 `SELECT FOR UPDATE`
- `HOLDINGS.version`: JPA `@Version` 낙관적 락
- 데드락 방지를 위해 항상 **Wallet → Holdings → Orders 순서**로 락 획득 (D09에서 코드로 강제)

### 4.4 `ORDERS`는 불변
- 정정·취소 없음. 한 번 INSERT되면 그대로 유지
- 거래 내역의 무결성·재계산 가능성을 보장 (백테스트, AI 회고에서 신뢰)

### 4.5 인덱스
| 인덱스 | 목적 |
|---|---|
| `idx_orders_user_created (user_id, created_at DESC)` | 거래 내역 페이지네이션 |
| `idx_orders_ticker_created (ticker, created_at DESC)` | 종목별 거래 통계 (관리자) |
| `idx_fx_lookup (base, quote, fetched_at DESC)` | 최신 환율 1건 조회 |
| `idx_users_role / idx_users_status` | 관리자 사용자 필터링 |
| `idx_stocks_market / sector / active` | 종목 검색·필터링 |

### 4.6 Oracle 21c 기능 활용
- `GENERATED ALWAYS AS IDENTITY` (12c+) — 시퀀스 + 트리거 보일러플레이트 제거
- `CHECK` 제약으로 enum 표현 (`role`, `status`, `market`, `order_type`...)
- `COMMENT ON` 으로 의도 명시 → DB 단독 분석 시에도 의미 파악 가능

## 5. 다음 단계

| Phase | 추가될 마이그레이션 |
|---|---|
| D06 | (옵션) `V2__admin_seed.sql` — 초기 ADMIN 계정 시드 |
| D08 | `V3__stocks_master_seed.sql` — 한·미 60종목 |
| D21 | `V4__limit_orders.sql` — 지정가 + 인덱스 `(ticker, status, target_price)` |
| D26 | `V5__price_history.sql` — 일별 OHLC |
| D31 | `V6__backtest_runs.sql` |
| D36 | `V7__ai_reports.sql` |
| D41 | `V8__admin_audit_logs.sql` |
| D44 | `V9__announcements.sql` |
