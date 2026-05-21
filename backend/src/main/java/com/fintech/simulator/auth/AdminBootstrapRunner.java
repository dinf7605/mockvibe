package com.fintech.simulator.auth;

import com.fintech.simulator.auth.domain.Role;
import com.fintech.simulator.auth.domain.User;
import com.fintech.simulator.auth.repository.UserRepository;
import com.fintech.simulator.portfolio.domain.Wallet;
import com.fintech.simulator.portfolio.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 초기 관리자 1회 부트스트랩.
 *
 * - app.auth.admin.password가 비어있으면 시드 스킵 (보안: 운영에서 env 미주입 시 안전)
 * - 이미 같은 이메일이 존재하면 스킵 (idempotent)
 * - 기존 사용자가 USER role이어도 강제 승격하지 않는다 (사이드 이펙트 방지)
 *
 * 운영 절차:
 *   1) 최초 배포 시 ADMIN_INITIAL_PASSWORD env 주입
 *   2) 부팅 → 시드 완료 후 즉시 env 제거 권장 (다음 부팅부터는 그냥 idempotent하게 스킵)
 *   3) 더 강한 보안이 필요하면 시드 후 직접 비밀번호 교체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AuthProperties authProperties;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AuthProperties.Admin admin = authProperties.admin();
        if (!admin.isReady()) {
            log.info("Admin bootstrap skipped: ADMIN_INITIAL_PASSWORD not provided.");
            return;
        }
        if (userRepository.existsByEmail(admin.email())) {
            log.info("Admin bootstrap skipped: email already exists ({})", admin.email());
            return;
        }

        // 관리자 외 다른 ADMIN 계정이 이미 있어도 새 ADMIN 시드는 허용 (다중 관리자 운영 가능)
        User adminUser = User.newAdmin(
                admin.username(),
                admin.email(),
                passwordEncoder.encode(admin.password())
        );
        userRepository.save(adminUser);

        // 관리자도 모의투자 가능하도록 동일 시드머니 지급
        Wallet wallet = Wallet.openWith(adminUser.getUserId(), authProperties.seedMoneyKrw());
        walletRepository.save(wallet);

        log.info("✅ Admin bootstrapped: userId={}, email={}, role={}, seed={}KRW",
                adminUser.getUserId(), adminUser.getEmail(), Role.ADMIN, authProperties.seedMoneyKrw());
    }
}
