#!/usr/bin/env bash
# ============================================================================
# 이전 이미지 태그로 롤백
# 사용:
#   bash deploy/scripts/rollback.sh <previous_sha>
#
# 직전 main 푸시의 git SHA를 GHCR 태그로 그대로 사용 (CI가 그렇게 푸시함)
# ============================================================================
set -euo pipefail

TAG="${1:-}"
if [ -z "$TAG" ]; then
  echo "사용: $0 <previous_sha_or_tag>"
  echo "현재 사용 가능한 태그:"
  docker images ghcr.io/dinf7605/mockvibe-backend --format '{{.Tag}}\t{{.CreatedSince}}' | head -10
  exit 1
fi

export APP_TAG="$TAG"
cd "$(dirname "$0")/../.."

echo "[rollback] APP_TAG=${TAG} 로 재기동"
docker compose -f docker/docker-compose.prod.yml --env-file .env.prod up -d app

# 헬스체크
source .env.prod
for i in $(seq 1 24); do
  if curl -fsS "https://${APP_DOMAIN}/actuator/health" >/dev/null 2>&1; then
    echo "[rollback] ✅ HEALTHY"
    exit 0
  fi
  sleep 5
done

echo "[rollback] ❌ 헬스체크 실패 — 추가 조치 필요"
exit 1
