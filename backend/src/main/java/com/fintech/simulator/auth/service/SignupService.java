package com.fintech.simulator.auth.service;

import com.fintech.simulator.auth.AuthProperties;
import com.fintech.simulator.auth.domain.User;
import com.fintech.simulator.auth.dto.SignupRequest;
import com.fintech.simulator.auth.dto.SignupResponse;
import com.fintech.simulator.auth.repository.UserRepository;
import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.portfolio.domain.Wallet;
import com.fintech.simulator.portfolio.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 트랜잭션.
 * 단일 트랜잭션 내에서 User INSERT + Wallet INSERT(시드머니 포함)를 수행.
 * 이메일 중복은 DB UNIQUE 제약과 사전 체크 모두 적용 (race condition 시 DB가 최종 방어).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    @Transactional
    public SignupResponse signup(SignupRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String encoded = passwordEncoder.encode(req.password());
        User user = User.newUser(req.username(), req.email(), encoded);
        userRepository.save(user);

        Wallet wallet = Wallet.openWith(user.getUserId(), authProperties.seedMoneyKrw());
        walletRepository.save(wallet);

        log.info("Signup completed: userId={}, email={}", user.getUserId(), user.getEmail());
        return SignupResponse.from(user, wallet.getCashBalance());
    }
}
