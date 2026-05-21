package com.fintech.simulator.auth.dto;

import com.fintech.simulator.auth.domain.Role;
import com.fintech.simulator.auth.domain.User;

import java.math.BigDecimal;

public record SignupResponse(
        String userId,
        String username,
        String email,
        Role role,
        BigDecimal seedMoneyKrw
) {
    public static SignupResponse from(User user, BigDecimal seedMoney) {
        return new SignupResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                seedMoney
        );
    }
}
