# 로컬 인프라 (Docker Compose)

## 구성
| 서비스 | 이미지 | 포트 | 용도 |
|---|---|---|---|
| `oracle` | `gvenzl/oracle-xe:21-slim-faststart` | 1521 | Oracle XE 21c (XEPDB1) |
| `redis` | `redis:7-alpine` | 6379 | Refresh Token / 응답 캐싱 |
| `app` | `backend/Dockerfile`로 빌드 | 8080 | Spring Boot (Actuator + Prometheus 노출) |

> 풀스택 한 줄 기동: `docker compose up -d` → DB·Redis healthy 후 app 빌드·기동.
> 백엔드만 IDE에서 띄울 거면 `docker compose up -d oracle redis`

## 사전 요구사항
- Docker Desktop (또는 Docker Engine) 24+
- WSL2 환경 권장 (Windows)
- 디스크 여유 4GB+ (Oracle 이미지 약 1.6GB + 데이터)

## 사용법

```bash
# 환경변수 준비
cp .env.example .env

# 기동
docker compose up -d

# 상태 확인
docker compose ps
docker compose logs -f oracle

# Oracle 준비 완료까지 대기 (최초 1~2분)
# 헬스체크: docker compose ps에서 oracle이 (healthy) 상태가 될 때까지 대기

# 종료
docker compose down

# 데이터까지 완전 초기화
docker compose down -v
```

## 접속 정보
| 항목 | 값 |
|---|---|
| JDBC URL | `jdbc:oracle:thin:@localhost:1521/XEPDB1` |
| 사용자 | `simulator` |
| 비밀번호 | `simulator` (`.env`에서 변경 가능) |
| SYS 비밀번호 | `OraclePwd123!` (`.env`에서 변경 가능) |
| Redis | `redis://localhost:6379` |

## 트러블슈팅
- **포트 1521 충돌**: `docker-compose.yml`의 `ports`에서 호스트 포트 변경 (예: `1522:1521`)
- **Oracle healthcheck 실패**: `docker compose logs oracle` 확인. 초기 약 1~2분 소요.
- **WSL2 메모리 부족**: `%USERPROFILE%\.wslconfig`에 `memory=4GB` 이상 설정.
