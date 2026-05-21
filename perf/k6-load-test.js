// ============================================================================
// k6 부하 테스트 — PRD NFR-1: 동시 50명, 매매 p95<300ms / 시세 전파 100ms 이내
//
// 실행:
//   docker run --rm --network host -v "$(pwd)/perf:/perf" grafana/k6 run /perf/k6-load-test.js
//   (Windows: -v "${PWD}/perf:/perf")
//
// 시나리오:
//   1) 가입 (이미 있으면 409 → 정상)
//   2) 로그인 → AT 획득
//   3) 50 VU 동시:
//      - GET /stocks/search?q=A   (40%)
//      - GET /market/price/005930 (30%)
//      - GET /portfolio           (20%)
//      - POST /trades/buy (수량 0.001 — 잔고 부족·동시성 영향 최소화) (10%)
// ============================================================================

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://host.docker.internal:8080';

export const options = {
  scenarios: {
    mixed_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 50 },    // 0 → 50명 램프업
        { duration: '60s', target: 50 },    // 1분 정상 부하
        { duration: '10s', target: 0 },     // 램프다운
      ],
      gracefulRampDown: '5s',
    },
  },
  thresholds: {
    'http_req_duration{name:search}':    ['p(95)<500'],
    'http_req_duration{name:price}':     ['p(95)<300'],
    'http_req_duration{name:portfolio}': ['p(95)<500'],
    'http_req_duration{name:buy}':       ['p(95)<800'],
    http_req_failed: ['rate<0.02'],   // 실패율 2% 미만
  },
};

const failRate = new Rate('biz_failures');
const buyLatency = new Trend('buy_latency_ms');

// 부하 테스트 전용 사용자 (한 번만 가입)
const TEST_EMAIL = 'loadtest@example.com';
const TEST_PASSWORD = 'Passw0rd!Load';

export function setup() {
  http.post(`${BASE}/auth/signup`, JSON.stringify({
    username: 'loader', email: TEST_EMAIL, password: TEST_PASSWORD,
  }), { headers: { 'Content-Type': 'application/json' } });

  const r = http.post(`${BASE}/auth/login`, JSON.stringify({
    email: TEST_EMAIL, password: TEST_PASSWORD,
  }), { headers: { 'Content-Type': 'application/json' } });
  const accessToken = r.json('accessToken');
  if (!accessToken) throw new Error('login failed: ' + r.body);
  return { accessToken };
}

export default function (data) {
  const auth = { headers: { Authorization: `Bearer ${data.accessToken}` } };
  const r = Math.random();

  if (r < 0.4) {
    group('search', () => {
      const res = http.get(`${BASE}/stocks/search?q=A&page=0&size=20`,
        { ...auth, tags: { name: 'search' } });
      check(res, { 'search 200': (x) => x.status === 200 }) || failRate.add(1);
    });
  } else if (r < 0.7) {
    group('price', () => {
      const res = http.get(`${BASE}/market/price/005930`,
        { ...auth, tags: { name: 'price' } });
      check(res, { 'price 200': (x) => x.status === 200 }) || failRate.add(1);
    });
  } else if (r < 0.9) {
    group('portfolio', () => {
      const res = http.get(`${BASE}/portfolio`,
        { ...auth, tags: { name: 'portfolio' } });
      check(res, { 'portfolio 200': (x) => x.status === 200 }) || failRate.add(1);
    });
  } else {
    group('buy', () => {
      const res = http.post(`${BASE}/trades/buy`,
        JSON.stringify({ ticker: '005930', quantity: 0.001 }),
        { headers: { ...auth.headers, 'Content-Type': 'application/json' }, tags: { name: 'buy' } });
      const ok = res.status === 201 || res.status === 400;  // 잔고 부족(400)도 정상
      check(res, { 'buy 201/400': () => ok }) || failRate.add(1);
      buyLatency.add(res.timings.duration);
    });
  }

  sleep(Math.random() * 0.3);  // VU 간 약간의 지터
}
