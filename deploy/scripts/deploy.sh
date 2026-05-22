#!/usr/bin/env bash
# ============================================================================
# 본 배포 / 무중단 갱신
# - GHCR에서 최신 이미지 pull
# - docker compose up -d (이미 실행 중이면 변경된 서비스만 재기동)
# - 헬스체크 60초 폴링
# ============================================================================
set -euo pipefail

cd "$(dirname "$0")/../.."

source .env.prod
: "${APP_DOMAIN:?APP_DOMAIN 필수}"
: "${FRONTEND_URL:?FRONTEND_URL 필수}"

APP_TAG="${APP_TAG:-latest}"
export APP_TAG

# 1) Nginx 설정 플레이스홀더 치환
RUNTIME_DIR="deploy/nginx/conf.d.runtime"
mkdir -p "$RUNTIME_DIR"
sed -e "s|APP_DOMAIN_PLACEHOLDER|${APP_DOMAIN}|g" \
    -e "s|\${FRONTEND_URL_PLACEHOLDER}|${FRONTEND_URL}|g" \
    deploy/nginx/conf.d/mockvibe.conf > "$RUNTIME_DIR/mockvibe.conf"

# 2) 이미지 pull (실패해도 진행 — 기존 이미지로 폴백)
docker compose -f docker/docker-compose.prod.yml --env-file .env.prod pull || true

# 3) 기동
docker compose -f docker/docker-compose.prod.yml --env-file .env.prod up -d

# 4) 헬스체크 폴링
echo "[deploy] 헬스체크 대기..."
for i in $(seq 1 30); do
  if curl -fsS "https://${APP_DOMAIN}/actuator/health" >/dev/null 2>&1; then
    echo "[deploy] ✅ HEALTHY (${i}회차)"
    docker compose -f docker/docker-compose.prod.yml ps
    exit 0
  fi
  sleep 5
done

echo "[deploy] ❌ 60초 내 헬스체크 실패 — 로그 확인"
docker compose -f docker/docker-compose.prod.yml logs --tail=80 app
exit 1
