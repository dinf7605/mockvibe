package com.fintech.simulator.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * application.yml의 `app.auth.*` 매핑.
 * - seedMoneyKrw: 회원가입 시 자동 지급되는 시드머니 (KRW)
 * - admin: 부팅 시 1회 시드되는 초기 관리자 계정 정보. password가 비어있으면 시드 스킵.
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        BigDecimal seedMoneyKrw,
        Admin admin
) {
    public AuthProperties {
        if (seedMoneyKrw == null) {
            seedMoneyKrw = new BigDecimal("10000000"); // 1,000만원 기본값
        }
        if (admin == null) {
            admin = new Admin(null, null, null);
        }
    }

    public record Admin(String email, String username, String password) {
        public boolean isReady() {
            return email != null && !email.isBlank()
                    && username != null && !username.isBlank()
                    && password != null && !password.isBlank();
        }
    }
}
