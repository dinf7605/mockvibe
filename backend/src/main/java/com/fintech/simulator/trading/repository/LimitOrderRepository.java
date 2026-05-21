package com.fintech.simulator.trading.repository;

import com.fintech.simulator.trading.domain.LimitOrder;
import com.fintech.simulator.trading.domain.LimitOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface LimitOrderRepository extends JpaRepository<LimitOrder, Long> {

    Page<LimitOrder> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /** D22 체결 후보: 특정 종목의 PENDING 주문, 인덱스(ticker, status, target_price) 활용 */
    List<LimitOrder> findByTickerAndStatus(String ticker, LimitOrderStatus status);

    /** D24 만료 배치 대상 */
    List<LimitOrder> findByStatusAndExpiresAtLessThan(LimitOrderStatus status, OffsetDateTime now);
}
