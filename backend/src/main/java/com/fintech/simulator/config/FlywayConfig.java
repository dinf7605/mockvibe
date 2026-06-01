package com.fintech.simulator.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 부팅 시 Flyway 마이그레이션 전략 — <b>repair() 후 migrate()</b>.
 *
 * <h3>왜 필요한가</h3>
 * 운영(ADB)에서 한 마이그레이션이 실패하면 schema history 에 failed 엔트리가 남아,
 * 다음 부팅의 validate 가 "Detected failed migration / run repair" 로 실패 → 재시작 무한 루프.
 * Spring Boot 의 {@code spring.flyway.repair-on-migrate} 는 실효성이 없어(미적용),
 * 매 부팅 시 명시적으로 {@link org.flywaydb.core.Flyway#repair()} 를 먼저 돌려
 * 실패 엔트리를 정리한 뒤 {@link org.flywaydb.core.Flyway#migrate()} 한다.
 *
 * repair() 는 (1) 실패 마이그레이션 엔트리 제거 (2) 적용된 마이그레이션의 체크섬/설명을
 * 현재 스크립트와 정합화한다. 잦은 배포 환경에서 부분 실패(DDL 자동커밋)로부터 자가복구.
 */
@Slf4j
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            log.info("Flyway: repair() 선행 — 실패/체크섬 엔트리 정리");
            flyway.repair();
            flyway.migrate();
        };
    }
}
