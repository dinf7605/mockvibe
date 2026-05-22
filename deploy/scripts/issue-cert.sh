#!/usr/bin/env bash
# ============================================================================
# Let's Encrypt 인증서 최초 발급 (--webroot 방식)
# - Nginx가 80 포트에서 /.well-known/acme-challenge/ 경로를 서빙해야 함
# - 발급 전 임시 nginx (HTTP만)로 챌린지 통과 후, 본 배포로 전환
# ============================================================================
set -euo pipefail

cd "$(dirname "$0")/../.."

source .env.prod
: "${APP_DOMAIN:?APP_DOMAIN 필수}"
LE_EMAIL="${LE_EMAIL:-admin@$APP_DOMAIN}"

# nginx 설정의 플레이스홀더를 실 도메인/프론트URL로 치환 → conf.d.runtime/
RUNTIME_DIR="deploy/nginx/conf.d.runtime"
mkdir -p "$RUNTIME_DIR"
sed -e "s|APP_DOMAIN_PLACEHOLDER|${APP_DOMAIN}|g" \
    -e "s|\${FRONTEND_URL_PLACEHOLDER}|${FRONTEND_URL}|g" \
    deploy/nginx/conf.d/mockvibe.conf > "$RUNTIME_DIR/mockvibe.conf"

# 인증서 발급 (HTTP-01 webroot 챌린지)
# 처음에는 nginx의 ssl_certificate 경로가 없어서 nginx 컨테이너가 안 뜨므로,
# certbot standalone 모드로 80 포트 점유 후 발급
docker run --rm \
  -p 80:80 \
  -v mockvibe-certbot-etc:/etc/letsencrypt \
  -v mockvibe-certbot-www:/var/www/certbot \
  certbot/certbot:latest certonly \
    --standalone \
    --non-interactive \
    --agree-tos \
    --email "$LE_EMAIL" \
    -d "$APP_DOMAIN"

echo "[issue-cert] 발급 완료 → /etc/letsencrypt/live/$APP_DOMAIN/"
echo "[issue-cert] 이제 deploy.sh 를 실행하세요."
