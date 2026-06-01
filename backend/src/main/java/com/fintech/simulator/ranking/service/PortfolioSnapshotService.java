package com.fintech.simulator.ranking.service;

import com.fintech.simulator.auth.AuthProperties;
import com.fintech.simulator.auth.domain.User;
import com.fintech.simulator.auth.repository.UserRepository;
import com.fintech.simulator.portfolio.dto.PortfolioResponse;
import com.fintech.simulator.portfolio.service.PortfolioService;
import com.fintech.simulator.ranking.domain.PortfolioSnapshot;
import com.fintech.simulator.ranking.repository.PortfolioSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 일별 포트폴리오 자산 스냅샷 적재기 — 수익률 랭킹 + 자산 추이의 데이터 소스.
 *
 * <h3>스케줄</h3>
 * <ul>
 *   <li><b>부팅 시 1회</b>(@Async) — 오늘 스냅샷 보장 (재시작해도 UPSERT 라 멱등)</li>
 *   <li><b>매일 16:00 KST</b> — KRX 마감 후 전 사용자 스냅샷</li>
 * </ul>
 * 사용자별 try/catch — 한 명 실패해도 계속. UPSERT 는 명시적 save()(ambient tx 미의존).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Order(50)   // ApplicationRunner 순서: 시드/일봉 fetcher(@Order 20·21·기본) 이후에 스냅샷
public class PortfolioSnapshotService implements ApplicationRunner {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final PortfolioService portfolioService;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final AuthProperties authProperties;

    @Override
    @Async
    public void run(ApplicationArguments args) {
        int n = snapshotAll();
        log.info("PortfolioSnapshot: boot 스냅샷 완료 — {} 사용자", n);
    }

    @Scheduled(cron = "0 0 16 * * *", zone = "Asia/Seoul")
    @Async
    public void dailySnapshot() {
        int n = snapshotAll();
        log.info("PortfolioSnapshot: 일일 스냅샷 완료 — {} 사용자", n);
    }

    /** 전 사용자 오늘 스냅샷 UPSERT. @return 처리한 사용자 수. */
    public int snapshotAll() {
        LocalDate today = LocalDate.now(KST);
        BigDecimal seed = authProperties.seedMoneyKrw();
        List<User> users = userRepository.findAll();
        int count = 0;
        for (User u : users) {
            try {
                snapshotOne(u.getUserId(), today, seed);
                count++;
            } catch (Exception e) {
                log.warn("스냅샷 실패 user={}: {}", u.getUserId(), e.toString());
            }
        }
        return count;
    }

    private void snapshotOne(String userId, LocalDate date, BigDecimal seed) {
        PortfolioResponse pf = portfolioService.get(userId);
        BigDecimal total = nz(pf.totalAssetKrw());
        BigDecimal cash = nz(pf.cashBalanceKrw());
        BigDecimal holding = nz(pf.holdingValueKrw());
        BigDecimal pnl = nz(pf.totalPnlKrw());
        BigDecimal returnPct = seed.signum() > 0
                ? total.subtract(seed).divide(seed, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        snapshotRepository.findByUserIdAndSnapshotDate(userId, date)
                .ifPresentOrElse(
                        existing -> {
                            existing.update(total, cash, holding, pnl, returnPct);
                            snapshotRepository.save(existing);
                        },
                        () -> snapshotRepository.save(
                                PortfolioSnapshot.of(userId, date, total, cash, holding, pnl, returnPct))
                );
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
