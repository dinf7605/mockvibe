package com.fintech.simulator.admin.security;

import com.fintech.simulator.auth.domain.User;
import com.fintech.simulator.auth.repository.UserRepository;
import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 위험 작업(시드머니 조정/권한 변경/강제 초기화) 전 비밀번호 재인증.
 *
 *  - issue(): 비밀번호 검증 → 단기 토큰(랜덤 UUID, Redis 5분) 발급
 *  - validate(): 토큰 검증 (1회용, 검증 후 즉시 삭제)
 *  - 키: STEPUP:{token} → adminUserId
 */
@Service
@RequiredArgsConstructor
public class StepUpService {

    private static final String KEY_PREFIX = "STEPUP:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;

    /** 비밀번호 재인증 → 단기 토큰 발급 */
    public String issue(String adminUserId, String password) {
        User u = userRepository.findById(adminUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(password, u.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(KEY_PREFIX + token, adminUserId, TTL);
        return token;
    }

    /** 위험 작업 메서드에서 호출. 검증 + 1회용 삭제. 실패 시 STEPUP_REQUIRED. */
    public void validate(String adminUserId, String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.STEPUP_REQUIRED);
        }
        String stored = redis.opsForValue().getAndDelete(KEY_PREFIX + token);
        if (stored == null || !stored.equals(adminUserId)) {
            throw new BusinessException(ErrorCode.STEPUP_REQUIRED);
        }
    }
}
