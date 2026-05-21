package com.fintech.simulator.auth.service;

import com.fintech.simulator.auth.AuthProperties;
import com.fintech.simulator.auth.domain.Role;
import com.fintech.simulator.auth.domain.User;
import com.fintech.simulator.auth.dto.SignupRequest;
import com.fintech.simulator.auth.dto.SignupResponse;
import com.fintech.simulator.auth.repository.UserRepository;
import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.portfolio.domain.Wallet;
import com.fintech.simulator.portfolio.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    @Mock UserRepository userRepository;
    @Mock WalletRepository walletRepository;
    @Mock PasswordEncoder passwordEncoder;

    AuthProperties authProperties = new AuthProperties(new BigDecimal("10000000"), null);

    @InjectMocks SignupService signupService;

    SignupServiceTest() {
        // @InjectMocks가 생성자/필드 주입을 처리하지만 record 의존성은 수동 주입
    }

    private SignupService service() {
        return new SignupService(userRepository, walletRepository, passwordEncoder, authProperties);
    }

    @Test
    @DisplayName("정상 가입 시 User 저장, BCrypt 인코딩, 시드머니 1,000만원으로 Wallet 생성")
    void signup_success() {
        // given
        SignupRequest req = new SignupRequest("홍길동", "hong@example.com", "Passw0rd!");
        given(userRepository.existsByEmail(req.email())).willReturn(false);
        given(passwordEncoder.encode(req.password())).willReturn("ENCODED_HASH");

        // when
        SignupResponse res = service().signup(req);

        // then
        assertThat(res.username()).isEqualTo("홍길동");
        assertThat(res.email()).isEqualTo("hong@example.com");
        assertThat(res.role()).isEqualTo(Role.USER);
        assertThat(res.seedMoneyKrw()).isEqualByComparingTo("10000000");
        assertThat(res.userId()).isNotBlank();

        ArgumentCaptor<User> userCap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCap.capture());
        assertThat(userCap.getValue().getPassword()).isEqualTo("ENCODED_HASH");
        assertThat(userCap.getValue().getRole()).isEqualTo(Role.USER);

        ArgumentCaptor<Wallet> walletCap = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCap.capture());
        assertThat(walletCap.getValue().getCashBalance()).isEqualByComparingTo("10000000");
        assertThat(walletCap.getValue().getUserId()).isEqualTo(userCap.getValue().getUserId());
    }

    @Test
    @DisplayName("이메일 중복 시 EMAIL_ALREADY_EXISTS 예외 발생 및 어떤 INSERT도 일어나지 않음")
    void signup_duplicate_email() {
        SignupRequest req = new SignupRequest("홍길동", "dup@example.com", "Passw0rd!");
        given(userRepository.existsByEmail(req.email())).willReturn(true);

        assertThatThrownBy(() -> service().signup(req))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }
}
