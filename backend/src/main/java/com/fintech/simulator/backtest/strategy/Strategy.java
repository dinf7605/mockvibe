package com.fintech.simulator.backtest.strategy;

import com.fintech.simulator.market.domain.PriceHistory;

import java.util.List;

/**
 * 백테스트 전략. 각 시점에 매수/매도/홀드 신호 반환.
 * - history: 처음부터 현재 인덱스까지의 시계열 (lookahead bias 방지를 위해 i+1 데이터는 전달 X)
 * - position: 현재 보유 중인지
 */
public interface Strategy {
    String name();

    /** index: 0-based 현재 일자 인덱스 */
    Signal evaluate(int index, List<PriceHistory> history, boolean inPosition);

    enum Signal { BUY, SELL, HOLD }
}
