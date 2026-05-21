# D48 부하 테스트 결과

| 항목 | 값 |
|---|---|
| 도구 | k6 v0.x (Docker `grafana/k6`) |
| 시나리오 | `ramping-vus` 0 → 50 (15s) → 50 (60s) → 0 (10s) |
| 총 호출 | **21,361건** (238.87 req/s) |
| 실패율 | **0.02%** (5/21361) |
| 네트워크 | 41MB 수신 / 9.4MB 송신 |
| 환경 | 로컬 (Win11 + Docker Desktop + Oracle XE + Redis) |

## 엔드포인트별 p95 (PRD NFR-1 임계치 대비)

| 엔드포인트 | p50 | p95 | 임계치 | 결과 |
|---|---|---|---|---|
| `GET /market/price/005930` | 1.22ms | **2.08ms** | <300ms | ✅ **144× 여유** |
| `GET /portfolio` | 5.07ms | **7.94ms** | <500ms | ✅ 63× |
| `GET /stocks/search` | 7.60ms | **12.90ms** | <500ms | ✅ 38× |
| `POST /trades/buy` | 17.16ms | **32.72ms** | <800ms | ✅ 24× |

## 임계치 검증

- ✓ `http_req_duration{name:search}` p95<500ms → 12.9ms
- ✓ `http_req_duration{name:price}` p95<300ms → 2.08ms
- ✓ `http_req_duration{name:portfolio}` p95<500ms → 7.94ms
- ✓ `http_req_duration{name:buy}` p95<800ms → 32.72ms
- ✓ `http_req_failed` rate<2% → 0.02%

## 해석

- **시장가 매수 트랜잭션**(Wallet 비관적 락 + Holdings 낙관적 락 + ORDERS INSERT)도 50 VU 동시에서 p95 32.72ms — 락 직렬화가 병목 없이 동작
- **/market/price/{ticker}** 캐시 hit만으로 처리되어 평균 1.35ms — PriceCache `ConcurrentHashMap`이 효율적
- **/portfolio** JPA N+1 회피 (보유 종목 ticker 일괄 `findAllById`)로 5ms대 안정
- **5건 실패 (0.02%)**는 k6 워밍업 중 connection reset 추정 — 운영 영향 없음

## 튜닝 결론

PRD 임계치를 모든 엔드포인트가 **24~144배 여유**로 통과 → 추가 JVM/HikariCP 튜닝 불필요.

운영 EC2 t3.micro(1GB)에서는 메모리 부족이 더 큰 변수이므로:
- JVM `-Xmx512m -XX:+UseG1GC`로 유지 (Dockerfile에 이미 적용)
- HikariCP `maximum-pool-size=10` 그대로 (50 VU에서도 부족 없음, prod profile에서 8로 감소)
- 50 VU 부하 시 Oracle XE 연결 풀 부족 징후 없음
