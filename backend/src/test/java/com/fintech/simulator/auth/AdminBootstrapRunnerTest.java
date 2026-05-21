package com.fintech.simulator.auth;

import com.fintech.simulator.auth.AuthProperties.Admin;
import com.fintech.simulator.auth.domain.Role;
import com.fintech.simulator.auth.domain.User;
import com.fintech.simulator.auth.repository.UserRepository;
import com.fintech.simulator.portfolio.domain.Wallet;
import com.fintech.simulator.portfolio.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock UserRepository userRepository;
    @Mock WalletRepository walletRepository;
    @Mock PasswordEncoder passwordEncoder;

    private AdminBootstrapRunner runner(AuthProperties props) {
        return new AdminBootstrapRunner(props, userRepository, walletRepository, passwordEncoder);
    }

    private AuthProperties withAdmin(String email, String password) {
        return new AuthProperties(new BigDecimal("10000000"),
                new Admin(email, "Administrator", password));
    }

    @Test
    @DisplayName("password가 비어있으면 시드 스킵")
    void skip_when_no_password() throws Exception {
        AuthProperties props = new AuthProperties(new BigDecimal("10000000"), new Admin("a@b.com", "A", null));

        runner(props).run(null);

        verify(userRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 존재하는 이메일이면 시드 스킵 (idempotent)")
    void skip_when_email_exists() throws Exception {
        AuthProperties props = withAdmin("admin@simulator.local", "secret");
        given(userRepository.existsByEmail("admin@simulator.local")).willReturn(true);

        runner(props).run(null);

        verify(userRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    @DisplayName("정상 시드: ADMIN role + Wallet 시드머니 생성")
    void seed_admin() throws Exception {
        AuthProperties props = withAdmin("admin@simulator.local", "secret");
        given(userRepository.existsByEmail("admin@simulator.local")).willReturn(false);
        given(passwordEncoder.encode("secret")).willReturn("ENCODED");

        runner(props).run(null);

        ArgumentCaptor<User> userCap = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCap.capture());
        assertThat(userCap.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(userCap.getValue().getEmail()).isEqualTo("admin@simulator.local");
        assertThat(userCap.getValue().getPassword()).isEqualTo("ENCODED");

        ArgumentCaptor<Wallet> walletCap = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, times(1)).save(walletCap.capture());
        assertThat(walletCap.getValue().getCashBalance()).isEqualByComparingTo("10000000");
        assertThat(walletCap.getValue().getUserId()).isEqualTo(userCap.getValue().getUserId());
    }
}
