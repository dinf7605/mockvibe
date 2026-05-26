# ADR-003: 운영 회복력 — D49 11건 이슈에서 도출한 영구 정책

| | |
|---|---|
| **상태** | Accepted |
| **결정일** | 2026-05-26 (D49 종료 직후) |
| **관련** | [D49 Postmortem](../operations/d49-deployment-postmortem.md), [ADR-002 배포 토폴로지](ADR-002-deployment-topology.md) |

## 1. 배경

D48까지 통과한 80/80 단위 + 22 e2e 테스트가 운영(EC2 + Oracle ADB + Nginx + Vercel)에 올라가자 **11건의 신규 이슈**가 터졌다. 모든 이슈는 *로컬 단일 컨테이너에서는 절대 안 나타나는* 환경 특화 문제였다 (외부 DB · TLS · 리버스 프록시 · ALPN · 라이브러리 호환성).

이 ADR은 그 11건을 **한 번 잡고 영구히 재발 안 하게** 박아둔 정책 묶음을 정리한다. 향후 동일한 trap을 다른 프로젝트에서 또 밟지 않도록.

## 2. 결정 — 5 가지 회복력 정책

### 2.1 의존성 명시 (Oracle ADB Wallet)

```gradle
runtimeOnly 'com.oracle.database.security:oraclepki:23.7.0.25.01'
```

- **Why**: `ojdbc11.jar` 단독은 `cwallet.sso` 형식 PKI keystore를 못 연다. Spring Boot가 자동 포함 안 함.
- **Alternative considered**: Wallet의 `ewallet.p12` + 비밀번호 명시 → URL 복잡해지고 비밀번호 또 하나 관리.
- **Trade-off**: 의존성 1개 추가 (~200KB). 무시할 만함.

### 2.2 Flyway 운영 회복력 4종

```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 0           # ADB 시스템 테이블 무시, V1부터 실행
    repair-on-migrate: true       # failed entry 자동 정리
    validate-migration-naming: true
```

| 옵션 | 막아낸 이슈 |
|---|---|
| `baseline-on-migrate: true` | ADB의 `DBTOOLS$EXECUTION_HISTORY` 시스템 테이블로 인한 "non-empty schema" 거부 |
| `baseline-version: 0` | baseline=1 default가 V1을 skip하는 부작용 |
| `repair-on-migrate: true` | V2 일회성 SQL 실패가 `success=false` 로 남아 다음 부팅 영구 차단 |
| `validate-migration-naming: true` | 향후 checksum mismatch 자동 보정 |

**Trade-off**: `repair-on-migrate` 는 잘못된 마이그레이션도 묻고 갈 수 있어 운영에서 양면성. 단, 우리는 V1~V7 모두 코드 리뷰된 fixed 파일이고 향후 변경 시 새 V8+ 추가 정책이라 위험 낮음.

### 2.3 Nginx 운영 패턴 4종

**(a) HTTP/1.1 강제**
```nginx
server {
    listen 443 ssl;
    # http2 on;  ← 제거. WebSocket(RFC 6455)과 ALPN h2 충돌 회피
    proxy_http_version 1.1;      # server 블록 default
}
```
**(b) Default 헤더 server 블록**
```nginx
server {
    proxy_set_header Connection        "";
    proxy_set_header Host              $host;
    proxy_set_header X-Real-IP         $remote_addr;
    proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```
**(c) Override location은 server default 모두 복제**

nginx 규칙: location에 `proxy_set_header` 하나라도 있으면 server default 비상속. 그래서 `/ws` 같은 특수 location은 default 헤더를 *모두* 다시 명시:
```nginx
location /ws {
    proxy_set_header Host $host;             # 다시
    proxy_set_header X-Forwarded-For ...;    # 다시
    proxy_set_header Upgrade    $http_upgrade;
    proxy_set_header Connection $connection_upgrade;
}
```

**(d) Template + sed 패턴**

```
conf.d/mockvibe.conf.template   ← git에. APP_DOMAIN_PLACEHOLDER, ${FRONTEND_URL_PLACEHOLDER}
conf.d/mockvibe.conf            ← .gitignore. deploy.sh가 매번 sed로 생성
```

deploy.sh가:
1. `.template` → `.conf` sed 치환
2. PLACEHOLDER 잔존 검사 (false positive 방지: 정확한 토큰만 검사)
3. `docker exec mockvibe-nginx nginx -t && nginx -s reload`

→ git pull 시 conf 손상 X, 변경 후 reload 자동.

### 2.4 컨테이너 lifecycle 정리

```yaml
services:
  app:
    restart: unless-stopped   # 자가 치유
```

**Trade-off**: 디버깅 시 자기 복구가 race condition을 만듦 (DB 정리 중에 backend가 자동 재시작 → 정리 무력화). 진단 시 항상 `docker rm -f` 강제 제거를 먼저.

### 2.5 자동 배포 파이프라인

```
git push main
  ↓
ci.yml (gradle test + vite build)
  ↓
docker-publish.yml (backend → GHCR ghcr.io/...:sha, :latest)
  ↓
deploy-ec2.yml (workflow_run 후 자동 실행)
  ↓
SSH → EC2 → git fetch + reset --hard origin/main → bash deploy/scripts/deploy.sh
  ↓
nginx reload + 헬스체크 폴링
```

- `EC2_HOST`, `EC2_SSH_KEY` 두 secret만 등록하면 가동
- 사람이 SSH 들어가 명령을 손으로 치지 않음 → 휴먼 에러 차단
- `git reset --hard origin/main` 으로 EC2 working tree 의 ad-hoc 수정을 항상 폐기 → 운영 환경이 항상 git main 과 동일

## 3. 트레이드오프

| 항목 | 수용한 단점 | 보완 |
|---|---|---|
| `http2 on` 제거 | REST API 가 HTTP/1.1로 작동 | 현재 트래픽(~50 VU)에선 무시 가능. 대규모 시 `ws.<domain>` 서브도메인으로 WebSocket 분리하고 메인은 h2 복귀 |
| `repair-on-migrate=true` | 잘못된 마이그레이션도 history 정리 가능 | 신규 V8+ 추가 시 코드 리뷰로 차단 |
| `git reset --hard` 자동 배포 | EC2 working tree dirty 변경 자동 폐기 | 운영자가 EC2에서 직접 conf 수정하는 패턴 자체를 권장 안 함. 모든 변경은 git → CI 거치도록 강제 |
| SSH 22 전체 허용 | 무차별 대입 시도 노이즈 | EC2 기본은 키 인증만이라 실제 침입 없음. fail2ban + GitHub Actions IP 대역 제한은 ADR-008로 미룸 |

## 4. 측정 지표

- [x] D49 11건 이슈 모두 영구 fix 또는 자동 회복으로 처리
- [x] `git push` 후 EC2 헬스체크 그린까지 평균 **< 3분**
- [x] 운영 부팅 시 Flyway V1~V7 자동 적용 (수동 SQL 0건)
- [x] Nginx + Tomcat + WebSocket + Swagger UI + OpenAPI JSON 모두 외부 도메인으로 정상 응답

## 5. 후속 ADR 후보
- ADR-008: SSH 접근 정책 (GitHub Actions IP 제한 또는 AWS SSM Session Manager)
- ADR-009: 관측성 — Prometheus → Grafana Cloud free tier
- ADR-010: Backup/Recovery — ADB 자동 백업 + Wallet 보관 정책

## 6. 참고

- 11건 이슈의 자세한 진단·해결 과정: [D49 Postmortem](../operations/d49-deployment-postmortem.md)
- 관련 커밋: `0331a85`(oraclepki) · `3298ea1`(V2 escape) · `e04a379`+`5731419`(Flyway) · `88fc802`+`9188245`(nginx 헤더) · `b718aee`(http2 off) · `e6351b8`(catch-all) · `e973a46`(자동배포) · `0f41569`(springdoc)
