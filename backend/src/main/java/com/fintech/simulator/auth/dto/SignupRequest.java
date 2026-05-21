package com.fintech.simulator.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청.
 * 비밀번호: 8~64자, 영문/숫자/특수문자 중 2종 이상 포함.
 */
public record SignupRequest(

        @NotBlank(message = "사용자명은 필수입니다.")
        @Size(min = 2, max = 50, message = "사용자명은 2~50자 사이여야 합니다.")
        String username,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100)
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자 사이여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다."
        )
        String password
) {}
