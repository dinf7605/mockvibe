# ADR-008: SSH 접근 정책 — 0.0.0.0/0 + 키 인증 강제 (현재) → SSM 전환 (목표)

| | |
|---|---|
| **상태** | Accepted (Phase 1) · 향후 Phase 2 (SSM) 전환 검토 |
| **결정일** | 2026-05-26 (D49 자동 배포 도입 시점) |
| **관련** | [ADR-003 운영 회복력](ADR-003-operational-resilience.md), [deploy-ec2.yml](../../.github/workflows/deploy-ec2.yml) |

## 1. 배경

D49에서 **GitHub Actions 자동 배포**를 도입하며 EC2 SSH 22 포트가 외부에서 접근 가능해야 했다. 선택지:

| 옵션 | 보안 | 운영 편의 | 비용 |
|---|---|---|---|
| (A) My IP 만 허용 | ⭐⭐⭐ | ⭐ (IP 자주 바뀜, GitHub Actions runner IP 매번 다름) | 0 |
| (B) **0.0.0.0/0 + 키 인증** ← Phase 1 | ⭐⭐ | ⭐⭐⭐ | 0 |
| (C) GitHub Actions IP 대역 화이트리스트 | ⭐⭐⭐ | ⭐⭐ (대역 자주 갱신) | 0 |
| (D) AWS SSM Session Manager | ⭐⭐⭐⭐ | ⭐⭐ (IAM 설정 + 학습) | 0 |
| (E) VPN/Bastion | ⭐⭐⭐⭐ | ⭐ | $ |

Phase 1 (D49 시점) 은 **빠르게 가동 + 적절한 안전판** 균형으로 **(B)** 선택. Phase 2 (운영 안정화 후) 는 **(D) SSM** 으로 전환 검토.

## 2. 결정 — Phase 1: 0.0.0.0/0 + 강한 키 인증

### 2.1 EC2 보안그룹

```
mockvibe-sg 인바운드:
  SSH   TCP 22  0.0.0.0/0   # GitHub Actions runner + 운영자
  HTTP  TCP 80  0.0.0.0/0   # Let's Encrypt 챌린지 + HTTPS 리다이렉트
  HTTPS TCP 443 0.0.0.0/0   # 실서비스
```

### 2.2 EC2 측 강화 (sshd_config 기본값 활용)

Ubuntu 24.04 기본 `/etc/ssh/sshd_config` 가 이미:
```
PasswordAuthentication no            # 비밀번호 로그인 차단
PubkeyAuthentication yes              # 키 인증만
PermitRootLogin prohibit-password    # root 비밀번호 차단
ChallengeResponseAuthentication no
```

→ 무차별 비밀번호 공격은 처음부터 0% 가능. 키 파일이 유출 안 되는 한 안전.

### 2.3 키 관리

- 키 형식: AWS 콘솔 생성 시 **RSA 2048** (필요시 `ssh-keygen -t ed25519`로 더 강한 키 교체)
- 보관: `C:\Users\PC\.ssh\mockvibe-key.pem` 로컬 + GitHub Secrets `EC2_SSH_KEY`
- 백업: 따로 안 함 (분실 시 EC2 인스턴스에 새 키 추가 후 기존 키 회수)
- 회수: AWS 콘솔 → EC2 인스턴스 → Actions → Manage key pairs (필요 시)

### 2.4 GitHub Secrets 격리

`EC2_SSH_KEY` 는 GitHub repo Settings → Secrets and variables → Actions에 암호화 저장. 워크플로 외부에선 읽기 불가:
- 워크플로 로그에 secret 출력 시 자동 `***` 마스킹
- forked repo PR 의 워크플로 실행 시 secret 미주입 (외부인이 PR로 key 탈취 불가)

## 3. 위협 모델 분석

| 위협 | Phase 1 방어 | 잔여 위험 |
|---|---|---|
| 비밀번호 무차별 대입 | sshd `PasswordAuthentication no` | ✅ 차단 |
| 키 유출 (clipboard, 멀웨어) | 로컬 파일 + GitHub Secrets 둘 다 보호 | 사용자 PC 침해 시 키 유출 가능 |
| Zero-day SSH 버그 | Ubuntu 자동 보안 패치 | 발견 시점 ~ 패치 적용 사이 노출 |
| 무차별 connect 시도 (로그 노이즈) | 로그 누적, CPU 약간 소모 | fail2ban 미적용 |
| AWS account 자체 탈취 | AWS 자체 IAM/MFA | 본 ADR 범위 외 |

### 3.1 fail2ban 미적용 이유
- 키 인증만이라 무차별 시도가 성공 불가 → 실제 보안 영향 거의 없음
- 단 로그 노이즈는 누적 → Phase 2 SSM 전환 시 자동 해소

## 4. Phase 2 목표 — AWS SSM Session Manager

### 4.1 동작
```
운영자 / GitHub Actions
   ↓ (AWS IAM 인증)
AWS Systems Manager
   ↓ (EC2 의 ssm-agent 가 outbound 로 연결 유지)
EC2 (SSH 22 포트 닫혀있음, public IP 없어도 무관)
```

- **SSH 22 인바운드 = 완전 차단**
- EC2 가 SSM 에 outbound 만 연결 → 외부에서 직접 접근 0%
- 모든 세션은 CloudWatch Logs 에 자동 기록 (audit trail)

### 4.2 GitHub Actions 측 변경
`appleboy/ssh-action` 대신 `aws-actions/configure-aws-credentials` + `aws ssm send-command`:
```yaml
- uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::<acct>:role/github-actions-deploy
    aws-region: ap-northeast-2
- run: |
    aws ssm send-command \
      --instance-ids i-01d838a03575b45af \
      --document-name AWS-RunShellScript \
      --parameters 'commands=["cd /home/ubuntu/mockvibe && git fetch && git reset --hard origin/main && bash deploy/scripts/deploy.sh"]'
```

GitHub OIDC + AWS IAM Role 로 *키 없는 인증*. AWS_ACCESS_KEY_ID 자체가 불필요.

### 4.3 전환 비용
- IAM Role + OIDC Provider 설정: 30분
- ssm-agent 설치 (Ubuntu 24.04 는 기본 설치됨)
- IAM Instance Profile EC2 에 부여
- 검증 + 워크플로 변경: 1시간

총 1.5시간. 운영 안정화 직후 (Phase 2) 진행.

## 5. 트레이드오프 — 왜 Phase 1을 굳이 0.0.0.0/0 으로 두나

| 비판 | 답변 |
|---|---|
| "0.0.0.0/0 은 보안 위험" | 키 인증만이라 비밀번호 공격은 0%. 실제 침해는 키 유출 시점만 발생 |
| "처음부터 SSM 으로 가지 그랬나" | D49 우선순위가 **운영 가동 + 자동 배포**였음. SSM 학습 곡선이 그 우선순위를 늦춤. Phase 분리는 합리적 |
| "fail2ban 정도는 켜야" | Phase 2 SSM 으로 22 자체가 닫히면 fail2ban 자체가 무의미. Phase 1 일시적 미적용은 비용 대비 효과 낮음 |

## 6. 측정 지표

- [ ] Phase 2 전환 후 SSH 22 인바운드 완전 차단 (`aws ec2 describe-security-groups` 로 검증)
- [ ] CloudWatch Logs에 모든 운영자 세션 기록
- [ ] GitHub Actions 측 IAM Role 권한 최소화 (ssm:SendCommand on specific instance만)

## 7. 참고
- AWS Systems Manager: https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager.html
- GitHub OIDC + AWS: https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services
- 현재 deploy 워크플로: [deploy-ec2.yml](../../.github/workflows/deploy-ec2.yml)
