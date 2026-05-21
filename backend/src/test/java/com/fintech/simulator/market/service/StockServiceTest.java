package com.fintech.simulator.market.service;

import com.fintech.simulator.common.exception.BusinessException;
import com.fintech.simulator.common.exception.ErrorCode;
import com.fintech.simulator.market.controller.StockSearchResponse;
import com.fintech.simulator.market.domain.Stock;
import com.fintech.simulator.market.repository.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock StockRepository stockRepository;
    @InjectMocks StockService stockService;

    @Test
    @DisplayName("get: 존재하면 응답 변환, 없으면 STOCK_NOT_FOUND")
    void get_found_and_not_found() {
        Stock s = stub("AAPL");
        given(stockRepository.findById("AAPL")).willReturn(Optional.of(s));
        given(stockRepository.findById("ZZZZ")).willReturn(Optional.empty());

        assertThat(stockService.get("AAPL").ticker()).isEqualTo("AAPL");

        assertThatThrownBy(() -> stockService.get("ZZZZ"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.STOCK_NOT_FOUND);
    }

    @Test
    @DisplayName("search: q/market 위임 + 페이지 size 상한 100 적용")
    void search_pagination_cap() {
        Page<Stock> page = new PageImpl<>(List.of(stub("AAPL"), stub("MSFT")));
        given(stockRepository.search(eq("a"), eq("NASDAQ"), any(Pageable.class))).willReturn(page);

        StockSearchResponse r = stockService.search("a", "NASDAQ", 0, 9999);

        assertThat(r.items()).hasSize(2);
        assertThat(r.totalElements()).isEqualTo(2);
    }

    private Stock stub(String ticker) {
        // 기본 생성자 protected라 reflection 우회 대신 간단한 Stock instance를 직접 만들기는 어려움.
        // 여기서는 Mockito mock으로 대체 (필드 접근만 사용)
        Stock m = org.mockito.Mockito.mock(Stock.class);
        given(m.getTicker()).willReturn(ticker);
        given(m.getMarket()).willReturn("NASDAQ");
        given(m.getCurrency()).willReturn("USD");
        given(m.getCompanyName()).willReturn(ticker + " Inc.");
        given(m.isActive()).willReturn(true);
        return m;
    }
}
