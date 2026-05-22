#!/usr/bin/env bash
# ============================================================================
# EC2 t3.micro 초기 셋업 (Ubuntu 22.04 또는 24.04 LTS 가정)
# - Docker / docker compose 플러그인 / Nginx 설정 디렉토리 / Swap 설정
# - 한 번만 실행 (idempotent)
#
# 사용:
#   scp -i key.pem bootstrap-ec2.sh ubuntu@<EC2_IP>:~
#   ssh -i key.pem ubuntu@<EC2_IP> 'bash ~/bootstrap-ec2.sh'
# ============================================================================
set -euo pipefail

log() { echo -e "\033[1;32m[bootstrap]\033[0m $*"; }

# ----------------------------------------------------------------------------
# 1) Swap 2GB — t3.micro 1GB RAM 보강 (Oracle 클라이언트 / G1GC 메모리 압박 대비)
# ----------------------------------------------------------------------------
if ! swapon --show | grep -q swapfile; then
  log "Swap 2GB 생성"
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
fi

# ----------------------------------------------------------------------------
# 2) Docker + compose plugin
# ----------------------------------------------------------------------------
if ! command -v docker >/dev/null; then
  log "Docker 설치"
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker "$USER"
fi

if ! docker compose version >/dev/null 2>&1; then
  log "docker compose plugin 설치"
  sudo apt-get update -y
  sudo apt-get install -y docker-compose-plugin
fi

# ----------------------------------------------------------------------------
# 3) 프로젝트 디렉토리 / 필요한 환경 파일 안내
# ----------------------------------------------------------------------------
PROJ_DIR="$HOME/mockvibe"
if [ ! -d "$PROJ_DIR" ]; then
  log "프로젝트 clone (public 저장소 가정)"
  git clone https://github.com/dinf7605/mockvibe.git "$PROJ_DIR"
fi

cd "$PROJ_DIR"

if [ ! -f .env.prod ]; then
  log ".env.prod 템플릿 생성 — 값을 채운 뒤 deploy.sh를 실행하세요"
  cp backend/.env.example .env.prod
  echo
  echo "==> $PROJ_DIR/.env.prod 를 편집해 다음을 채우세요:"
  echo "    JWT_SECRET / ORACLE_TNS_NAME / DB_USERNAME / DB_PASSWORD /"
  echo "    REDIS_PASSWORD / APP_DOMAIN / FRONTEND_URL /"
  echo "    KIS_* / FINNHUB_* / GEMINI_API_KEY"
  echo
fi

# ----------------------------------------------------------------------------
# 4) Oracle Wallet 자리 안내
# ----------------------------------------------------------------------------
WALLET_DIR="$PROJ_DIR/deploy/oracle-wallet"
if [ -z "$(ls -A "$WALLET_DIR" 2>/dev/null | grep -v '^\.')" ]; then
  log "Oracle Wallet 미배치 — $WALLET_DIR 에 ADB Wallet zip을 풀어주세요"
fi

# ----------------------------------------------------------------------------
# 5) Let's Encrypt 첫 발급 안내
# ----------------------------------------------------------------------------
log "다음 단계:"
echo "  1) .env.prod 채우기"
echo "  2) deploy/oracle-wallet/ 에 Wallet 압축 해제"
echo "  3) bash deploy/scripts/issue-cert.sh   # Let's Encrypt 인증서 발급"
echo "  4) bash deploy/scripts/deploy.sh       # 본 배포"
echo
log "재로그인 후 docker 그룹 권한이 적용됩니다 (newgrp docker 또는 logout)"
