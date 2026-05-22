# ADR-002: 배포 토폴로지 — EC2 + Nginx + GHCR + Oracle ADB + Vercel

| | |
|---|---|
| **상태** | Accepted |
| **결정일** | 2026-05-22 (D49) |
| **관련** | PRD §13 운영, [docker-compose.prod.yml](../../docker/docker-compose.prod.yml), [.github/workflows/](../../.github/workflows/) |

## 1. 결정 사항

운영 환경을 다음과 같이 구성한다.

| 계층 | 선택 | 이유 (한 줄) |
|---|---|---|
| 컴퓨트 | **AWS EC2 t3.micro** | 12개월 프리티어, 1GB RAM, 면접에서 익숙한 스택 |
| 컨테이너 오케스트레이션 | **docker compose (단일 호스트)** | k8s/ECS는 모의투자 트래픽에 과잉, 학습 ROI 낮음 |
| 이미지 레지스트리 | **GitHub Container Registry (GHCR)** | 저장소와 일체화, 공개 이미지 풀 무료 |
| Edge | **Nginx + Let's Encrypt** | TLS 종단·WS 프록시·정적 캐싱 한 번에, certbot 갱신 자동화 검증 용이 |
| DB | **Oracle Autonomous Database (Always Free)** | PRD에 명시된 Oracle 종속성 유지, Wallet 기반 mTLS, 백업 자동 |
| Cache | **Redis 컨테이너 (동일 호스트)** | 외부 Redis 비용↓, RT/블랙리스트 latency 최소 |
| Frontend | **Vercel (Hobby)** | `*.vercel.app` 무료 HTTPS, GitHub 연동 PR 프리뷰 |
| 도메인 | **DuckDNS (Dynamic DNS)** | 무료, Let's Encrypt 호환 확인 |
| CI/CD | **GitHub Actions** | gradle test + Docker GHCR 푸시, OIDC로 시크릿 최소화 |

## 2. 배경 / 문제

D48까지의 코드는 컨테이너화·헬스체크·Prometheus 노출까지 마쳤지만, **외부에서 접근 가능한 단일 URL이 없어** 면접 시연이 불가능했다. 또한 데모 중 데이터 손실/롤백 가능성을 통제할 운영 절차가 부재했다.

선택지:
- **(A)** ECS Fargate + ALB + RDS + CloudFront + Route53
- **(B)** EC2 t3.micro + docker compose + Nginx + Oracle ADB + Vercel ← **선택**
- **(C)** Render / Railway 같은 PaaS에 일임

## 3. 선택 이유 — (B)

### 3.1 비용
- 12개월 프리티어 내 **월 $0 운영** 가능. ADB는 영구 무료.
- (A)는 ALB 단독으로 월 ~$18, NAT GW 추가 시 더 큼.
- (C)는 편하지만 Spring Boot + WebSocket + Oracle Wallet 조합 지원이 모호.

### 3.2 학습/면접 어필
- "어떤 책임을 누가 지나"를 직접 설계한 흔적이 남는다 (Nginx conf, compose, GHA workflow, 헬스체크, 롤백 스크립트).
- 단일 호스트라도 **이미지 태깅·롤백 절차**는 동일하므로, 면접에서 "프로덕션 운영을 안다"의 증거로 충분.
- Fargate는 인프라가 추상화되어 *결정 가능한 표면적*이 적다 — 포트폴리오로는 역설적으로 약함.

### 3.3 회복탄력성
- Nginx에서 TLS 종단 + 백엔드 헬스체크 폴백 → 단일 호스트에서도 무중단 배포 가능 (`docker compose up -d`는 변경 서비스만 재기동).
- ADB는 RPO/RTO를 Oracle이 보장. EC2는 휘발성이 있으나, **상태는 ADB+Redis 볼륨에만** 둠.

## 4. 트레이드오프

| 항목 | 수용한 단점 | 보완 |
|---|---|---|
| t3.micro 1GB RAM | JVM이 매우 빠듯 (Xmx=512m) | Swap 2GB, G1GC, `XX:+ExitOnOutOfMemoryError`로 폭주 시 빠른 재시작 |
| SPOF (단일 EC2) | 인스턴스 다운 = 서비스 다운 | 면접용 데모 수준에서 수용. Auto Scaling Group은 ADR-008(예정)에서 검토 |
| docker compose 수동 배포 | 무중단 보장이 약함 (재시작 동안 ~10초 다운) | `restart: unless-stopped` + Vercel은 백엔드 다운 시 stale UI로 graceful fail |
| GHCR public 이미지 | 누구나 pull 가능 | 비밀값은 이미지에 미포함 (env 주입). 이미지에는 jar만 |
| Wallet 파일 관리 | EC2 디스크 손상 시 재배포 필요 | Wallet은 Oracle Console에서 항시 재다운로드 가능 — 백업 불필요 |

## 5. 운영 시나리오

1. **첫 배포**: `bootstrap-ec2.sh` → Wallet 풀기 → `.env.prod` 작성 → `issue-cert.sh` → `deploy.sh`
2. **일상 배포**: main 푸시 → GHA가 GHCR로 이미지 푸시 → SSH로 `deploy.sh` 실행 (= pull + up -d)
3. **롤백**: `rollback.sh <previous_sha>` — GHCR에 sha 태그가 항상 남음
4. **인증서 갱신**: compose의 certbot 사이드카가 12시간마다 자동 갱신, Nginx는 `reload` 불필요 (volume 공유)

## 6. 측정 지표 (D49 완료 시 검증)

- [ ] EC2 부팅 → `https://${APP_DOMAIN}/actuator/health` 200 응답까지 **< 5분**
- [ ] GHA `Docker Publish` 워크플로 평균 **< 4분**
- [ ] `deploy.sh` 실행 후 헬스체크 그린까지 **< 60초**
- [ ] Vercel 빌드 **< 90초**

## 7. 후속 ADR 후보
- ADR-003: 단일 호스트 → Auto Scaling Group 전환 기준
- ADR-004: 관측성 — Prometheus → Grafana Cloud free tier 연동
- ADR-005: 로그 — 파일 → CloudWatch / Loki 전환 시점
