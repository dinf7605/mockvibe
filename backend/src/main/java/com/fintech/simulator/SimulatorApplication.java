package com.fintech.simulator;

import com.fintech.simulator.ai.GeminiProperties;
import com.fintech.simulator.auth.AuthProperties;
import com.fintech.simulator.auth.jwt.JwtProperties;
import com.fintech.simulator.config.CookieProperties;
import com.fintech.simulator.config.CorsProperties;
import com.fintech.simulator.fx.FxProperties;
import com.fintech.simulator.market.provider.finnhub.FinnhubProperties;
import com.fintech.simulator.market.provider.kis.KisProperties;
import com.fintech.simulator.trading.TradingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication(exclude = {
		// JWT 기반이라 inMemoryUserDetailsManager가 필요 없음. 기본 비밀번호 로그 워닝 제거.
		UserDetailsServiceAutoConfiguration.class
})
@EnableMethodSecurity     // @PreAuthorize / @PostAuthorize 활성화 (메서드 단위 깊이 방어)
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties({
		AuthProperties.class,
		CorsProperties.class,
		JwtProperties.class,
		CookieProperties.class,
		TradingProperties.class,
		KisProperties.class,
		FinnhubProperties.class,
		FxProperties.class,
		GeminiProperties.class
})
public class SimulatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimulatorApplication.class, args);
	}

}
