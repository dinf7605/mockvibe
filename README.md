# fintech-simulator

한국·미국 주식 통합 모의투자 + AI 매매 코치 + 백테스트 + 리스크 분석 풀스택 플랫폼.

## 📚 문서
- [PRD.md](./PRD.md) — 제품 요구사항 v2.0
- [DAILY_PLAN.md](./DAILY_PLAN.md) — 10주 50일 작업 계획

## 🛠 기술 스택
| 영역 | 스택 |
|---|---|
| Backend | Spring Boot 3.5, Java 17, JPA, WebSocket(STOMP), Spring Security |
| DB | Oracle (로컬 XE / 운영 Autonomous DB) + Flyway |
| Cache | Redis (Refresh Token / 응답 캐싱) |
| Frontend | React + Vite + TypeScript + Zustand + TradingView Lightweight + ApexCharts |
| External | KIS, Finnhub, ExchangeRate-API, Claude API |
| Infra | AWS EC2 t3.micro + Nginx + Vercel + Oracle Cloud |

## 📁 디렉토리 구조
```
mockvibe/
├── PRD.md
├── DAILY_PLAN.md
├── backend/                   # Spring Boot
├── frontend/                  # React (예정, D02)
├── docker/                    # 로컬 인프라 (예정, D02)
├── deploy/                    # EC2/Vercel 배포 설정 (예정, P7)
└── docs/                      # ADR, API, 데모 시나리오 (예정, P7)
```

## 🚀 백엔드 로컬 실행 (Phase 1)
```bash
cd backend
./gradlew bootRun
# 기본 프로파일: local (jdbc:oracle:thin:@localhost:1521/XEPDB1)
```

환경 변수 (`.env` 또는 IDE Run Config):
```
KIS_APP_KEY=...
KIS_APP_SECRET=...
FINNHUB_API_KEY=...
CLAUDE_API_KEY=...
DB_USERNAME=simulator
DB_PASSWORD=simulator
```

## 🚀 프론트엔드 로컬 실행
```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
npm run build        # tsc + vite build (배포 검증)
```

## 🗓 현재 진행 상황
- ✅ **Day 1**: Spring Boot 골격, 패키지 스켈레톤, application.yml 분리
- ✅ **Day 2**: Docker Compose (Oracle XE + Redis), Vite + React + TS 초기화, Pretendard + 디자인 토큰, 라우팅 골격, axios/zustand 스토어
- ✅ **Day 3**: Flyway V1 마이그레이션 (USERS/WALLET/STOCKS/HOLDINGS/ORDERS/FX_RATES + 인덱스 + 점진적 마이그레이션 ADR)
- ✅ **Day 4**: 회원가입 API (`POST /auth/signup`), BCrypt(cost 12), Wallet 1,000만원 자동 생성, 전역 예외 처리, 단위/슬라이스 테스트 5개 통과
- ✅ **Day 5**: 로그인/리프레시/로그아웃 (`/auth/login`·`/refresh`·`/logout`), JWT(HS256, Access 15분/Refresh 7일), Redis RT 저장소 + Rotation, AT 블랙리스트(jti), httpOnly 쿠키, JwtAuthenticationFilter + Entry Point, **테스트 21/21 통과**
- ✅ **Day 6**: RBAC `@EnableMethodSecurity` + `@PreAuthorize`, JwtAccessDeniedHandler(403), AdminBootstrapRunner(env 주입 idempotent 시드), `/admin/ping` + 깊이 방어, **테스트 26/26 통과** + **e2e RBAC 매트릭스 검증 완료**(401/403/200)
- ⏳ **Day 7**: Provider 인터페이스 + Mock Engine + PriceCache
