# D50 데모 영상 스크립트

| | |
|---|---|
| **목표** | 면접관 / 채용 담당 / GitHub 방문자가 30초~1분 안에 핵심 가치를 이해 |
| **길이** | **45초 ~ 60초** 권장 (3분 넘어가면 안 봄) |
| **포맷** | MP4 1080p 30fps 또는 GIF (README 임베드용은 GIF) |
| **음성** | 무음 + 자막 (모바일 / 사일런트 환경 대응) |

## 1. 사전 준비

### 1.1 환경

```bash
# 운영 환경 직접 사용 권장 (Live demo 어필)
# https://mockvibe-hazel.vercel.app

# 또는 로컬 docker compose (외부 API 키 채워둠)
cd C:\Users\PC\Desktop\Android\mockvibe
docker compose -f docker/docker-compose.yml up -d
cd frontend && npm run dev
```

### 1.2 녹화 도구

| 도구 | 추천도 | 장점 |
|---|---|---|
| **OBS Studio** | ⭐⭐⭐ | 무료, 1080p, 게임 캡처 가능, GIF는 별도 변환 |
| **ShareX** | ⭐⭐⭐ | 화면 일부만 녹화 + 즉시 GIF, 가볍다 |
| **ScreenToGif** | ⭐⭐⭐ | GIF 전용. README 임베드 용도면 1순위 |
| **Windows + G** | ⭐⭐ | Xbox Game Bar, 윈도우 기본. 빠르지만 영역 선택 불가 |
| **LightShot / Loom** | ⭐⭐ | 클라우드 업로드 가능 |

**추천 조합**:
- 풀 영상 (README 헤더 GIF): **ScreenToGif** → 빨간 영역 지정 → REC → 저장 → 자동 최적화
- 1080p MP4 (LinkedIn 등): **OBS** → 800×600 또는 1280×720 화면 영역

### 1.3 더미 데이터 세팅

녹화 직전 깔끔한 화면을 위해:
```sql
-- 데모용 사용자 생성 / 시드머니 초기화
-- (관리자 페이지에서 GUI로 가능)
```

또는 신규 회원 가입 → 자동으로 1,000만원 시드머니 받음 → 그 사용자로 시연.

## 2. 시나리오 — 45초 컷

### 컷 1 — 랜딩 페이지 (5초)
```
[화면] https://mockvibe-hazel.vercel.app 첫 접속
[보임]
  - Hero "데이터로 전략을 검증하는 실시간 모의투자 플랫폼"
  - 그라데이션 타이틀
  - "무료로 시작하기 →" CTA
[자막] "1분 회원가입, 1,000만원 시드머니"
```

### 컷 2 — 회원가입 (5초)
```
[액션] "무료로 시작하기 →" 클릭 → /signup
[입력]
  Email:    demo@example.com
  Username: demo
  Password: Demo1234!
[액션] 회원가입 버튼 → 자동 로그인 → /dashboard
[자막] "JWT + Refresh Rotation 인증"
```

### 컷 3 — 대시보드 + 종목 매수 (15초)
```
[보임] /dashboard
  - KPI 4개: 잔고 10,000,000 KRW / 보유 가치 0 / 손익 0 / 변동률 -
  - 도넛 차트 (자산 배분)
[액션] 헤더 "종목 검색" 클릭 → /search
[입력] 검색창에 "삼성"
[보임] 삼성전자 카드 + 가격
[액션] 카드 클릭 → /stocks/005930
[보임]
  - 실시간 차트 (lightweight-charts)
  - 가격 깜빡임 효과 (STOMP wss)
[액션] "매수" 버튼 → 1주 입력 → 주문 → 체결 토스트
[자막]
  - "STOMP WebSocket 실시간 시세"
  - "비관락 + 낙관락 하이브리드 동시성"
```

### 컷 4 — AI 코멘트 (5초)
```
[보임] 매매 직후 우측 또는 하단에 AI 코멘트 카드 등장
  "삼성전자 매수가 KRW 75,000으로 체결됐습니다. 현재 포트폴리오의
   83%가 반도체 섹터에 집중되어 있어..."
[자막] "Gemini 기반 매매 코치"
```

### 컷 5 — 백테스트 (10초)
```
[액션] 헤더 "백테스트" 클릭 → /backtest
[입력]
  종목: 삼성전자 (또는 SK하이닉스)
  전략: MA20 (이동평균 골든크로스)
  기간: 1년
[액션] 실행 버튼
[보임]
  - 자산 곡선 차트 (lightweight-charts v5)
  - 매매 마커 (createSeriesMarkers)
  - 최종 수익률, MDD, Sharpe
[자막] "BuyAndHold / MA20 / RSI14 전략 비교"
```

### 컷 6 — 리스크 대시보드 (5초)
```
[액션] 헤더 "리스크" 클릭 → /risk
[보임]
  - VaR(95%) -2.1%
  - Sharpe 0.8
  - Beta 1.15
  - MDD -8.3%
  - 집중도 경고: "삼성전자 단일 종목 83%"
[자막] "VaR · Sharpe · Beta · MDD · 집중도"
```

### 컷 7 (선택) — 관리자 + CB 신호등 (5초)
```
[액션] /admin → 시스템 페이지
[보임]
  Circuit Breaker 5개
  🟢 kis-auth  🟢 kis-approval  🟢 kis-rest
  🟢 fx-rate  🟢 claude
[자막] "Resilience4j Circuit Breaker — 외부 API 장애 격리"
```

### 엔딩 (3초)
```
[보임] GitHub 로고 + URL
  github.com/dinf7605/mockvibe
[자막]
  "Spring Boot 3.5 · React 19 · Oracle 23ai · STOMP"
  "11 운영 이슈 진단 / 자동 배포 / 풀스택"
```

## 3. 녹화 팁

- **첫 1초**가 가장 중요 — 정적 랜딩이 아니라 매매 시연부터 시작하는 cold open 버전도 만들기
- **마우스 커서**: 윈도우 마우스 설정에서 크기 키우기 (캡처 시 시인성 ↑)
- **속도**: 일부 동작 (검색 입력 등)은 2x 가속. ScreenToGif/OBS 후처리에서 가능
- **컷 전환**: 페이드 X, 즉시 컷이 더 dynamic
- **자막**: ScreenToGif는 자체 자막 기능. OBS는 후처리(DaVinci Resolve free) 또는 ffmpeg

## 4. 출력물

| 파일 | 용도 | 권장 spec |
|---|---|---|
| `docs/assets/demo.gif` | README 헤더 임베드 | 800×450, ≤8MB, 45초, 무한 루프 |
| `docs/assets/demo-1080p.mp4` | LinkedIn / 이력서 첨부 | 1920×1080, 30fps, ≤30MB |
| `docs/assets/demo-vertical.mp4` | (선택) 인스타/쇼츠 | 1080×1920 |

GIF 최적화:
```bash
# 8MB 이하로 축소 (gifsicle 또는 ezgif.com)
gifsicle -O3 --colors 128 --lossy=80 demo-raw.gif -o demo.gif
```

## 5. README 임베드

GIF 완성 후 README 헤더에 추가:

```markdown
# MockVibe — fintech-simulator
> 한국·미국 주식 통합 실시간 모의투자 + AI 매매 코치 + 백테스트 ...

![Demo](docs/assets/demo.gif)

[![Java](...)] ...
```

## 6. 체크리스트

- [ ] 운영 환경 (live) 또는 깨끗한 로컬 환경
- [ ] 데모용 신규 사용자 1,000만원 시드머니
- [ ] 외부 API 키 모두 가동 (KIS / Finnhub / Gemini / ExchangeRate)
- [ ] 마우스 커서 크기 키움
- [ ] 녹화 도구 설치 + 영역 지정
- [ ] 45초 흐름 1회 리허설
- [ ] 본 녹화 1~3 take
- [ ] ScreenToGif/ezgif로 GIF 변환 + 최적화
- [ ] README 임베드 + 커밋
- [ ] LinkedIn / GitHub Pages 업로드 (선택)

## 7. 추가 어필 포인트

녹화 영상 끝에 짧게 문구로:
- "**11건의 운영 이슈를 진단하며 만든** 운영 환경"
- "**git push 한 줄로 자동 배포**"
- "**50 VU 부하에 buy p95 32.72ms** (NFR 24배 여유)"

이 셋이 면접관의 '깊이 알고 있는지' 의심을 한 번에 푸는 키 메시지.
