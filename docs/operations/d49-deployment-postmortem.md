# D49 운영 배포 Postmortem — 8건의 이슈와 해결

| | |
|---|---|
| **일자** | 2026-05-22 ~ 2026-05-26 |
| **환경** | AWS EC2 t3.micro (ap-northeast-2) · Oracle ADB Always Free (ap-chuncheon-1) · DuckDNS · Let's Encrypt |
| **스택** | Spring Boot 3.5.6 · Flyway 11.7.2 · Oracle JDBC 23ai · Tomcat 10.1 · Nginx 1.27 · springdoc 2.6 → 2.8.6 |
| **결과** | 모든 endpoint(actuator/health · swagger-ui · v3/api-docs · /api/*) 정상 가동 |

---

## TL;DR

D48까지 로컬에서 통과한 코드를 운영 환경(EC2 + Oracle ADB)에 처음 올리며 **총 8건의 이슈**가 발생했다. 모두 **로컬에서는 절대 안 나타나는** 운영 특화 문제였고 (외부 DB · TLS · 리버스 프록시 · 의존성 호환성), 각각의 진단 → 해결 → 영구 수정까지 기록한다.

| # | 카테고리 | 한 줄 요약 |
|---|---|---|
| 1 | DB Driver | Oracle Wallet SSO에 oraclepki 라이브러리 필요 |
| 2 | SQL Parsing | INSERT 데이터의 `&` 가 Flyway/SQL\*Plus 호환 모드에서 치환변수로 오해석 |
| 3 | Env File | Docker Compose `--env-file` 의 CRLF/공백 처리 차이 |
| 4 | Container Lifecycle | restart 정책이 DB 정리 작업을 무력화 |
| 5 | Flyway Baseline | 빈 ADB의 시스템 테이블 때문에 "non-empty schema" 인식 |
| 6 | Flyway Recovery | failed migration entry가 다음 부팅을 영구 차단 |
| 7 | Nginx ↔ Tomcat | HTTP/1.0 요청을 Tomcat 10.1이 strict 모드에서 거부 |
| 8 | 라이브러리 호환 | springdoc 2.6 ↔ Spring Framework 6.2 (Boot 3.5) 시그니처 변경 |

---

## 환경 컨텍스트

```
              ┌─────────────────────────────────────────┐
              │  EC2 t3.micro (Ubuntu 24.04, 1GB RAM)  │
              │  ┌────────┐  ┌────────┐  ┌──────────┐ │
   user ─https→│ nginx  │→ │  app   │→ │  redis   │ │
              │  └────────┘  └────────┘  └──────────┘ │
              │       │           │                    │
              │   certbot       wallet                  │
              └───────│───────────│────────────────────┘
                      ↓           ↓
                Let's Encrypt   Oracle ADB
                                (Always Free)
```

---

## 이슈 1. ORA-17957 "SSO not found" — oraclepki 누락

### 증상
첫 부팅 시 Spring Boot가 Oracle 연결 단계에서 죽음.
```
Caused by: java.security.NoSuchAlgorithmException: SSO KeyStore not available
  at oracle.net.nt.ExtendedSSLContext.createKeyStore(...)
Caused by: oracle.net.ns.NetException: ORA-17957: Unable to initialize the key store.
```

### 원인
ADB의 `cwallet.sso`(자동로그인 PKI keystore)를 Oracle JDBC Thin Driver가 열려면 `oraclepki.jar`가 classpath에 필요하다. `ojdbc11.jar` 단독으로는 SSO 형식 keystore 처리 불가. Spring Boot가 의존성 자동 포함 안 함.

### 해결
`build.gradle`에 의존성 명시.
```gradle
runtimeOnly 'com.oracle.database.security:oraclepki:23.7.0.25.01'
```
ojdbc 23.7 + Java 11+ 조합은 `osdt_cert`/`osdt_core`가 oraclepki에 번들 포함되어 단일 의존성으로 충분.

### 관련 커밋
`0331a85 fix(deploy): oraclepki 의존성 추가로 ADB Wallet(cwallet.sso) 자동로그인 지원`

### 교훈
**관리형 DB(ADB)는 mTLS Wallet이 기본**. 로컬 Docker XE에서는 절대 안 나타나는 의존성. PRD 운영 환경 결정 시점에 driver 의존성 표를 같이 작성해야 했다.

---

## 이슈 2. V2 INSERT의 `&` 가 치환변수로 해석

### 증상
```
Detected failed migration to version 2 (stocks master seed).
```
ADB SQL Worksheet에서 V2 SQL을 직접 실행하면 **"치환변수 입력" 팝업** 발생.

### 원인
V2 시드의 회사명 4건이 `&` 문자 포함:
- `'KT&G'`, `'Johnson & Johnson'`, `'Procter & Gamble'`, `'Merck & Co.'`

Flyway의 `flyway-database-oracle` 플러그인 + Oracle SQL\*Plus 호환 파싱이 `&xyz` 패턴을 substitution variable로 인식 → INSERT 실행 자체가 실패. Flyway는 한 번 실패한 마이그레이션을 `schema_history` 에 `success=false`로 기록하고, 이후 부팅 시 `validate` 단계에서 무조건 차단.

### 진단 흐름 (5번 막힌 후 결정타)
1. ADB 정리 → deploy → 같은 에러
2. 컨테이너 restart 정책으로 history 즉시 재생성 의심 → 강제 down
3. ADB SQL Worksheet 에서 **V2를 손으로 실행** → "치환변수 입력" 팝업 ← **결정타**
4. `SET DEFINE OFF;` 후 V2 재실행 → 60건 정상 INSERT

### 해결
Oracle quote literal로 escape — 어떤 클라이언트에서도 `&`를 literal로만 해석.
```sql
-- 변경 전
'KT&G'
-- 변경 후
q'[KT&G]'
```
`q'[...]'` 문법은 Oracle 9i+ 모든 버전 지원, 동작 동일.

### 관련 커밋
`3298ea1 fix(migration): V2의 '&' 4건을 Oracle quote literal로 escape하여 Flyway 실패 해소`

### 교훈
- **시드 데이터의 특수문자**는 운영 환경 호환성을 미리 점검. 로컬 XE에서는 SQL\*Plus 모드 차이로 그냥 통과했음.
- **Flyway는 한 번 실패하면 영구 차단**. 일회성 SQL 실패가 무한 루프가 되는 구조라 회복 옵션이 필수.

---

## 이슈 3. `.env.prod` 의 CRLF/공백 파싱 차이

### 증상
```
WARN The "REDIS_PASSWORD" variable is not set. Defaulting to a blank string.
WARN The "DB_PASSWORD" variable is not set. ...
```
deploy.sh의 `source .env.prod` 는 정상 작동하는데 `docker compose --env-file .env.prod` 는 못 읽음.

### 원인
- bash `source` 는 관대 (따옴표, 공백, CRLF 일부 허용)
- Docker Compose `--env-file` 은 엄격 (`KEY=value` 정확 형식만)

Windows 환경에서 파일을 만지면 CRLF 줄바꿈이 들어가고, `KEY = value` (등호 양쪽 공백)도 거부.

### 해결
일괄 정리 명령:
```bash
sed -i 's/\r$//; s/^[ \t]*//; s/ *= */=/' .env.prod
```
검증:
```bash
docker compose -f docker/docker-compose.prod.yml --env-file .env.prod config 2>&1 | grep -i WARN
# 빈 줄이어야
```

### 교훈
**Bash와 Compose의 .env 파서가 다르다**. 셸에서 잘 읽혔다고 compose도 잘 읽힐 거라 가정 X. 운영 안정성을 위해 deploy.sh 시작에 형식 검증 hook을 넣어도 좋다.

---

## 이슈 4. 컨테이너 restart 정책이 DB 정리를 무력화

### 증상
ADB SQL Worksheet에서 `DROP TABLE "flyway_schema_history"` 실행 후 user_tables 조회하면 **다시 생긴 채로** 보임. 사용자는 "왜 안 지워지지?" 라고 의심.

### 원인
`docker-compose.prod.yml` 의 `restart: unless-stopped` 정책. Spring Boot가 V2 실패로 죽으면 즉시 재시작 → 새 schema_history 생성 → V2 또 실패 → ... 이걸 사용자가 SQL Worksheet에서 정리하는 그 순간에도 backend가 계속 만들어내는 race condition.

### 해결
```bash
docker compose -f docker/docker-compose.prod.yml --env-file .env.prod down
docker rm -f $(docker ps -aq --filter "name=mockvibe-") 2>/dev/null
docker ps -a | grep mockvibe   # 비어있어야
# 그 후 ADB 정리
```

### 교훈
**운영 컨테이너의 restart 정책은 양날의 검**. 자기 복구를 위한 정책이 디버깅을 어렵게 한다. 진단 시 항상 컨테이너를 먼저 죽이고 시작.

---

## 이슈 5. ADB 시스템 테이블이 "non-empty schema" 트리거

### 증상
ADB를 완전히 비웠는데도 Flyway 부팅 시:
```
Found non-empty schema(s) "ADMIN" but no schema history table.
Use baseline() or set baselineOnMigrate to true.
```

### 원인
ADB의 ADMIN schema에는 `DBTOOLS$EXECUTION_HISTORY` 등 Oracle Cloud Database Actions 가 만든 시스템 테이블이 **삭제 불가**하게 존재. Flyway는 이걸 보고 "비어있지 않다"고 판단.

- `baseline-on-migrate=false` 면 위 에러로 부팅 실패
- `baseline-on-migrate=true` 만 두면 V1을 baseline 표시(실행 X)해버려 USERS 등 테이블이 안 생김

### 해결
```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 0   # V0를 가상 baseline → V1부터 실제 실행
```
Flyway가 빈 schema_history를 만들고 V0 row 한 건 삽입 후, V1부터 정상 적용.

### 관련 커밋
`5731419 fix(flyway): baseline-version=0으로 ADB 시스템 테이블 회피`

### 교훈
**관리형 DB는 사용자 schema가 절대 깨끗하지 않다**. Flyway baseline 옵션은 보통 무시되는데, 관리형 DB 운영에서는 핵심 설정.

---

## 이슈 6. failed migration entry가 부팅 영구 차단

### 증상
이슈 2가 발생한 후 V2 SQL을 고쳐도 같은 에러가 반복:
```
Detected failed migration to version 2.
```
이미 history에 `success=false` row가 남아서, validate 단계에서 즉시 거부.

### 해결 (영구)
```yaml
spring:
  flyway:
    repair-on-migrate: true
```
부팅 시 자동으로 `repair()` 실행 → failed entry 정리 → 마이그레이션 재시도. 실제 DB 객체는 안 건드림(history만 정리)이라 안전.

### 관련 커밋
`e04a379 fix(flyway): baseline-on-migrate=false + repair-on-migrate=true 로 운영 회복력 확보`

### 교훈
**일회성 SQL 실패가 운영 영구 장애가 되는 구조**는 회복 옵션으로 미리 방어. `repair-on-migrate` 는 운영에서 양면성 있지만(잘못된 마이그레이션을 묻고 가는 위험), `validate-migration-naming=true` 와 함께 쓰면 안전한 자동 복구.

---

## 이슈 7. Tomcat 400 "request parsing errors"

### 증상
- `docker exec mockvibe-app wget http://localhost:8080/actuator/health` → ✅ `{"status":"UP"}`
- `curl https://localhost/actuator/health` (nginx 경유) → ❌ Tomcat 기본 400 페이지

### 원인
nginx conf의 `location = /actuator/health` 블록:
```nginx
location = /actuator/health {
    proxy_pass http://mockvibe_backend/actuator/health;
    access_log off;
}
```
`proxy_http_version 1.1` 명시 누락 → nginx 기본 HTTP/1.0 으로 backend 호출 → **Tomcat 10.1이 strict 모드에서 HTTP/1.0 요청을 거부**.

backend 로그에 결정적 단서:
```
Note: further occurrences of request parsing errors will be logged at DEBUG level.
```

### 해결 (영구)
server 블록 레벨에 default 명시 → 모든 location 상속.
```nginx
server {
    ...
    proxy_http_version 1.1;
    proxy_set_header   Connection ""; 
    proxy_set_header   Host        $host;
    ...
    location /api/ { ... }
    location /ws  { ... }
    location = /actuator/health { ... }
}
```

추가로 placeholder 치환 패턴 정리:
- `conf.d/mockvibe.conf` → `conf.d/mockvibe.conf.template`
- deploy.sh가 `*.template` → `*.conf` 로 sed
- `.gitignore` 에 `conf.d/*.conf` (template 제외)

### 관련 커밋
- `88fc802 fix(nginx): server 블록 레벨 proxy 헤더 default + template 패턴 정리`
- `7220694 fix(deploy): placeholder 검증 false positive 회피`

### 교훈
- **모든 location에 일관된 proxy 헤더**가 필요. 한 블록만 빠뜨려도 endpoint별로 다른 동작.
- Tomcat 10.x 는 9.x 대비 HTTP 파싱 엄격. Spring Boot 3 운영 환경의 새로운 제약.

---

## 이슈 8. springdoc 2.6 ↔ Spring Boot 3.5 호환성 깨짐

### 증상
`/v3/api-docs` 호출 시 500. backend 로그:
```
Caused by: java.lang.NoSuchMethodError:
  'void org.springframework.web.method.ControllerAdviceBean.<init>(java.lang.Object)'
    at org.springdoc.core.service.GenericResponseService...
```

### 원인
Spring Framework 6.2 (Spring Boot 3.5 동봉)에서 `ControllerAdviceBean(Object)` 생성자가 제거됨. springdoc 2.6.0이 그 옛 생성자를 직접 호출 → NoSuchMethodError. springdoc 2.6.x는 Spring Boot 3.4까지만 호환.

### 해결
```gradle
ext { springdocVersion = '2.8.6' }   // 2.6.0 → 2.8.6
```
2.8.x 시리즈가 Spring Boot 3.5 / Spring Framework 6.2 정식 지원.

### 관련 커밋
`0f41569 fix(deps): springdoc-openapi 2.6.0 → 2.8.6 (Spring Boot 3.5 호환)`

### 교훈
- **Spring Boot 메이저 업그레이드 시 보조 라이브러리(springdoc, micrometer 등) 호환 매트릭스 확인 필수**. Spring Boot 본체만 올리면 의존성이 자동으로 맞춰지지 않는다.
- 로컬 dev 단계에서는 `/v3/api-docs` 까지 일일이 호출하지 않으니, **운영 배포 직후 종단 endpoint 헬스체크 리스트가 필요**.

---

## 종합 교훈

### 1. "로컬 통과 ≠ 운영 통과" — D48까지의 80/80 테스트가 무용지물이었던 이유
- 외부 DB Wallet, TLS 종단, 리버스 프록시, HTTP 버전 협상은 로컬 단일 컨테이너에서 발생 안 함
- **운영-유사 환경에서 e2e 테스트 한 번을 D48 전에 했다면** 이슈 1·5·7·8을 미리 잡았을 것

### 2. 회복 옵션을 미리 박아라
- `repair-on-migrate` 같은 자동 회복 옵션은 운영 안정성 확보의 기본
- restart 정책 + 자동 회복 + 헬스체크 trio가 자가 치유 시스템의 시작

### 3. 진단 = 가설을 좁히는 과정
- 8건 모두 처음엔 "환경변수가 안 읽힌다", "Flyway가 이상하다" 같은 막연한 가설로 시작
- **컨테이너 직접 호출 vs nginx 경유 호출 비교** 같은 단순 비교가 결정타 (이슈 7)
- **다른 클라이언트로 같은 SQL 직접 실행** 이 진단 핵심 (이슈 2)

### 4. 이슈마다 영구 수정 + 자동화
- 한 번 우회로 끝내지 않고 코드/설정에 박아 다음 부팅에 다시 안 나오게
- 우회 → 영구 수정 → 재현 차단 패턴

### 5. 운영 환경의 "특수문자" 함정
- `&` (이슈 2), CRLF (이슈 3), placeholder 텍스트 (이슈 7) — 사람이 자연스럽게 쓰는 문자가 도구 체인의 어떤 단계에서 의미를 갖는다
- 시드 데이터 · 환경변수 · template 모두 escape 또는 검증 필수

---

## 부록: 자주 쓴 진단 명령

### 컨테이너 vs nginx 경유 비교
```bash
docker exec mockvibe-app wget -qO- http://localhost:8080/actuator/health   # 직접
curl -k -H "Host: mockvibe.duckdns.org" https://localhost/actuator/health  # nginx
```

### Flyway history 조회 (소문자 식별자 주의)
```sql
SELECT "installed_rank", "version", "description", "success"
FROM "flyway_schema_history" ORDER BY "installed_rank";
```

### 컨테이너 완전 정리
```bash
docker compose -f docker/docker-compose.prod.yml --env-file .env.prod down
docker rm -f $(docker ps -aq --filter "name=mockvibe-") 2>/dev/null
```

### Mount inode 확인 (bind mount stale 의심)
```bash
docker inspect mockvibe-nginx --format '{{range .Mounts}}{{.Source}} -> {{.Destination}}{{println}}{{end}}'
```

### Backend stack trace 추출
```bash
docker logs mockvibe-app --tail 200 2>&1 | grep -B 2 -A 40 -i "error\|exception\|caused by"
```

---

## 다음 작업 (D49 마무리 / D50 진입)

- [ ] Vercel 프론트 임포트 + `FRONTEND_URL` 갱신
- [ ] 종단 테스트 (회원가입 → 로그인 → 매수)
- [ ] ADR-003 작성 (운영 회복력 4종: oraclepki + repair-on-migrate + baseline-version=0 + nginx server-default)
- [ ] README 운영 URL/배지 갱신
- [ ] 데모 영상
