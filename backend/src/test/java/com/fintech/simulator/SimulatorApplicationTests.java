package com.fintech.simulator;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("D10에서 Testcontainers + Oracle XE로 통합 테스트 본격 추가 예정")
@SpringBootTest
class SimulatorApplicationTests {

    @Test
    void contextLoads() {
        // 풀 ApplicationContext 로드 검증은 Oracle/Redis 의존 환경에서 진행
    }
}
