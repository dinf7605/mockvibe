# ADR-006: 시세 Provider 추상화 — KIS · Finnhub · Mock

| | |
|---|---|
| **상태** | Accepted |
| **결정일** | 2026-05-13 (D07~D08 Provider 인터페이스 도입) |
| **관련** | PRD §6.2 (외부 시세 통합), [MarketDataProvider.java](../../backend/src/main/java/com/fintech/simulator/market/provider/MarketDataProvider.java) |

## 1. 배경

MockVibe는 **3가지 시세 출처**를 다룬다.

| Provider | 시장 | 통신 | 환경 |
|---|---|---|---|
| **KIS** | KRX (한국) | OAuth → approval_key → WebSocket | API 키 보유 시 |
| **Finnhub** | NYSE/NASDAQ (미국) | API key → WebSocket | API 키 보유 시 |
| **Mock** | 가상 | (내부 랜덤워크) | 로컬 / API 키 미보유 / CB Open |

세 출처는 **데이터 형식·인증·구독 메커니즘·rate limit이 전부 다르다**. 도메인 코드(TradingService 등)가 매 호출마다 if/else 분기로 처리하면 코드 폭발.

## 2. 결정 — 인터페이스 통일 + Spring 조건부 Bean

### 2.1 인터페이스 — `MarketDataProvider`

```java
public interface MarketDataProvider {
    /** 시장 식별 */
    Market market();          // KRX | NASDAQ | NYSE | MOCK

    /** 현재가 조회 (REST 폴링 / 캐시) */
    Quote currentPrice(String ticker);

    /** WebSocket 구독 시작/해제 */
    void subscribe(String ticker);
    void unsubscribe(String ticker);

    /** 헬스 — Resilience4j CB 상태 위에서 보조 신호 */
    boolean isHealthy();
}
```

`Quote` 는 가격 + 통화 + 환산 시각의 공통 DTO. 각 Provider가 내부 응답을 이걸로 변환.

### 2.2 구현체 — `@ConditionalOnProperty` 가드

```java
@Component
@ConditionalOnProperty(name = "app.external.kis.app-key")
public class KisMarketDataProvider implements MarketDataProvider { ... }

@Component
@ConditionalOnProperty(name = "app.external.finnhub.api-key")
public class FinnhubMarketDataProvider implements MarketDataProvider { ... }

@Component
@ConditionalOnProperty(name = "app.market.mock.enabled", havingValue = "true",
                      matchIfMissing = true)
public class MockMarketDataProvider implements MarketDataProvider { ... }
```

→ **환경변수만으로 Provider 활성/비활성**. 로컬에선 키 없이도 Mock 만으로 전체 시연 가능. 운영에선 KIS+Finnhub+Mock 셋 다 등록되고 Mock 은 fallback 으로.

### 2.3 디스패치 — `MarketDataRouter`

```java
@Service
@RequiredArgsConstructor
public class MarketDataRouter {
    private final List<MarketDataProvider> providers;  // Spring 자동 주입
    private Map<Market, MarketDataProvider> byMarket;

    @PostConstruct
    void index() {
        byMarket = providers.stream()
            .collect(Collectors.toMap(MarketDataProvider::market, p -> p));
    }

    public Quote getPrice(Stock stock) {
        return byMarket
            .getOrDefault(stock.market(), byMarket.get(Market.MOCK))
            .currentPrice(stock.ticker());
    }
}
```

종목의 `market` 컬럼으로 라우팅. 매칭 실패 시 Mock fallback.

## 3. 대안 비교

| 옵션 | 평가 |
|---|---|
| (A) 도메인 서비스에 if/else 분기 | 가장 단순. 매 호출마다 분기 → SOLID 위반, 확장성 X |
| (B) **인터페이스 + 조건부 Bean** ← 선택 | Spring 표준. 추가 Provider 시 클래스 1개 만들면 끝 |
| (C) Strategy + Factory 수동 등록 | 보일러플레이트. (B)와 효과 같지만 코드 더 많음 |
| (D) Reactive Streams (Project Reactor) 통합 | KIS/Finnhub 가 모두 WebSocket 이라 후보였으나, REST/JPA 와 mixed 환경에서 복잡도 ↑ |

## 4. 효과

### 4.1 코드 격리

- TradingService 는 `MarketDataRouter.getPrice(stock)` 만 호출. KIS 인증, Finnhub WebSocket subscribe, Mock 랜덤워크 — 전혀 모름
- 신규 시장 추가 시: Provider 구현체 1개 + `@ConditionalOnProperty` 만 추가. **기존 코드 0줄 변경**

### 4.2 운영 안정성

- KIS API 키 발급 안 된 상태에서도 부팅 가능 (Mock 만 활성)
- D49 운영 부팅 시 KIS WebSocket connect 실패 → 자동으로 Mock fallback 사용 (CB와 결합)
- 테스트 환경: `app.external.kis.app-key` 미설정 → KIS Provider Bean 자체가 안 만들어짐 → Mock 만 동작 → 외부 의존성 없는 단위 테스트 가능

### 4.3 WebSocket lifecycle 통일

세 Provider 모두 동일한 인터페이스 (`subscribe`/`unsubscribe`) 로 노출. 사용자가 종목 상세 페이지에 진입하면 `PriceBroadcaster` 가 `subscribe(ticker)` 호출 → Provider 가 자체 WebSocket 으로 외부에 구독 등록 → 수신 데이터를 STOMP `/topic/price/{ticker}` 로 fan-out.

→ **클라이언트(브라우저)는 KIS/Finnhub/Mock 차이를 모름**. 동일한 STOMP topic 만 구독.

## 5. 트레이드오프

| 항목 | 비용 |
|---|---|
| 인터페이스 design 시간 | 초기 설계 1일. 이후 변경 사실상 0 |
| 모든 Provider 가 isHealthy 등 보일러플레이트 구현 | 명확한 분리의 비용. 인터페이스에 default method 도입 검토 가능 |
| Spring conditional resolve 디버깅 | Bean 안 떴을 때 원인이 conditional 일 수 있음. `/actuator/conditions` 로 확인 |

## 6. 후속 ADR 후보

- ADR-007: WebSocket reconnect 정책 — KIS/Finnhub 끊김 시 exponential backoff
- ADR-008: 시세 데이터 정규화 — KIS의 `output1.stck_prpr` 같은 raw 필드를 Quote 로 변환하는 mapping layer
- ADR-011: 신규 시장 추가 가이드 — Tokyo Stock Exchange 등 추가 시 PR 체크리스트

## 7. 참고

- 코드: 
  - [`MarketDataProvider.java`](../../backend/src/main/java/com/fintech/simulator/market/provider/MarketDataProvider.java)
  - [`MockMarketDataProvider.java`](../../backend/src/main/java/com/fintech/simulator/market/provider/MockMarketDataProvider.java)
  - [`KisMarketDataProvider.java`](../../backend/src/main/java/com/fintech/simulator/market/provider/kis/KisMarketDataProvider.java)
  - [`FinnhubMarketDataProvider.java`](../../backend/src/main/java/com/fintech/simulator/market/provider/finnhub/FinnhubMarketDataProvider.java)
- Spring Boot `@ConditionalOnProperty` 공식 문서
- PRD §6.2: "외부 시세 출처 추상화로 확장성 확보"
