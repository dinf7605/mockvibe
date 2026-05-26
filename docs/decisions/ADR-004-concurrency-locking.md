# ADR-004: 동시성 — 비관락(Wallet) + 낙관락(Holdings) 하이브리드

| | |
|---|---|
| **상태** | Accepted |
| **결정일** | 2026-05-15 (D09 시장가 매매 구현 직전) |
| **관련** | PRD §10 NFR-2 (동시성 정합성), [TradingService.java](../../backend/src/main/java/com/fintech/simulator/trading/service/TradingService.java) |

## 1. 배경

매수/매도 트랜잭션은 동일 사용자의 **Wallet**(예수금)과 **Holdings**(보유 종목)를 함께 갱신한다. 동시 요청 시 정합성 깨질 수 있는 시나리오:

1. **이중 차감**: 잔고 1,000원으로 동시에 800원짜리 주문 2건 → 둘 다 통과하면 잔고 -600
2. **분실 갱신 (Lost Update)**: 보유 1주에 매수 1주 + 매도 1주 동시 → version 충돌로 한쪽 손실
3. **Phantom Read**: 평균단가 계산 중 다른 매수가 끼어들어 stale 데이터 사용

## 2. 결정 — 객체 특성별 락 전략 분리

| 객체 | 락 종류 | 이유 |
|---|---|---|
| **WALLET** | **비관적 락** `@Lock(LockModeType.PESSIMISTIC_WRITE)` | 1 user : 1 wallet 단독. 동일 자원 경합이 잦음. row lock 으로 직렬화 |
| **HOLDINGS** | **낙관적 락** `@Version` | user × ticker 분산. 충돌 빈도 낮음. retry 가능 |
| **ORDERS** | 락 없음 (INSERT-only) | 불변 이력. 충돌 자체가 정의되지 않음 |

### 2.1 Wallet 비관락 — `WalletRepository`

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select w from Wallet w where w.userId = :userId")
Optional<Wallet> findByUserIdForUpdate(@Param("userId") String userId);
```

Oracle 측에서 `SELECT ... FOR UPDATE` 로 변환되어 row lock. 트랜잭션 종료까지 다른 세션은 같은 wallet 조회 시 대기.

### 2.2 Holdings 낙관락 — `Holding` 엔티티

```java
@Column(nullable = false)
@Version
private Long version;
```

JPA가 UPDATE 시 `WHERE version = ?` 추가, `RETURNING version+1`. 다른 트랜잭션이 먼저 commit하면 row count = 0 → `OptimisticLockException`. 호출자가 retry.

### 2.3 트랜잭션 락 획득 순서 (deadlock 회피)

```
TradingService.executeMarketBuy(userId, ticker, qty)
  ├─ 1. wallet = walletRepo.findByUserIdForUpdate(userId)   ← 비관락
  ├─ 2. holding = holdingRepo.findByUserAndTicker(userId, ticker)   ← 낙관락
  ├─ 3. price = priceCache.get(ticker)
  ├─ 4. wallet.subtractCash(price × qty)
  ├─ 5. holding.addQuantity(qty, price)   ← UPDATE 시 version 증가
  └─ 6. orderRepo.save(Order.of(...))      ← INSERT
```

**락 순서는 항상 Wallet → Holdings → Orders**. 모든 매매 경로에서 동일 순서를 강제하므로 **deadlock 불가능** (락 순서 그래프에 cycle 없음).

## 3. 대안 비교

| 옵션 | 평가 |
|---|---|
| (A) **전 영역 비관락** | 단순. 단 holdings는 사용자 × 종목 조합 다양 → 불필요한 직렬화. throughput ↓ |
| (B) **전 영역 낙관락** | 동시성 ↑. 단 wallet 충돌이 잦아 OptimisticLockException → retry 폭증 → tail latency 악화 |
| (C) **하이브리드** ← **선택** | 특성별 최적 락. retry 부담 최소 |
| (D) Redis 분산 락 | 외부 의존성 추가. 단일 DB의 row lock 으로 충분한 규모 |

## 4. 검증

### 4.1 단위 테스트 — `TradingServiceConcurrencyTest`

```java
@Test
void 동시매수_10병렬_잔고정합성() {
    int N = 10;
    int unitCost = 80_000;        // 1주당 가격
    walletService.deposit(userId, 1_000_000L);   // 100만원

    var futures = IntStream.range(0, N)
        .mapToObj(i -> CompletableFuture.runAsync(
            () -> tradingService.buy(userId, "005930", 1, unitCost)))
        .toList();

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    Wallet result = walletRepo.findByUserId(userId).orElseThrow();
    assertThat(result.getCash()).isEqualTo(1_000_000L - (unitCost * N));
    assertThat(holdingRepo.findByUserAndTicker(userId, "005930").get().getQuantity())
        .isEqualTo(N);
}
```

결과: 10병렬 매수 후 잔고 정확히 차감, 보유 수량 정확. **PASS**.

### 4.2 부하 (D48 k6)

50 VU × 5분 동안 시장가 매수만 21,361건. 잔고 음수 0건, holdings UNIQUE 위반 0건. tail p95 32.72ms.

## 5. 트레이드오프

| 항목 | 비용 |
|---|---|
| 비관락의 직렬화 | 동일 사용자의 매매가 직렬 처리. **사용자별 throughput 제한**. 다른 사용자 매매에는 영향 X (row lock이라) |
| OptimisticLockException retry | holdings 충돌 시 호출자가 캐치. 우리는 `@Retryable(OptimisticLockException, maxAttempts=3)` 적용 |
| @Version 컬럼 추가 | DB 스키마 + JPA 엔티티에 1 컬럼. 무시 가능 |

## 6. 미래 확장

- **고빈도 동일 사용자 트레이딩**: wallet 비관락이 병목 → wallet 을 잔액 + 예약잔액으로 분리, 예약잔액만 비관락 + 정산은 비동기
- **분산 환경 전환 시**: 단일 RDB row lock으로 충분. 멀티 마스터 시 Redis 분산 락 또는 Saga 패턴 검토

## 7. 참고
- 코드: [`TradingService.java`](../../backend/src/main/java/com/fintech/simulator/trading/service/TradingService.java)
- Spring Data JPA Lock: https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html
- PRD §10 NFR-2: "동시 매매 시 잔고/보유 수량 정합성"
