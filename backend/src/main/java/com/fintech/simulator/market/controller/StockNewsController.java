package com.fintech.simulator.market.controller;

import com.fintech.simulator.market.dto.NewsItem;
import com.fintech.simulator.market.provider.finnhub.FinnhubNewsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 종목 뉴스 — 미국 종목(Finnhub company-news)만 지원.
 *
 * <p>GET /stocks/{ticker}/news?days=7
 * - 미국 심볼(영문 1~5자)만 Finnhub 호출, KRX(숫자) 등은 빈 리스트.
 * - Finnhub 키 미설정(Bean 없음) 시에도 빈 리스트 — ObjectProvider 안전 조회.
 */
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockNewsController {

    private final ObjectProvider<FinnhubNewsClient> newsClientProvider;

    @GetMapping("/{ticker}/news")
    public List<NewsItem> news(@PathVariable String ticker,
                               @RequestParam(defaultValue = "7") int days) {
        if (ticker == null || !ticker.matches("[A-Za-z]{1,5}")) {
            return List.of();   // 미국 종목만 지원
        }
        FinnhubNewsClient client = newsClientProvider.getIfAvailable();
        if (client == null) return List.of();
        return client.companyNews(ticker.toUpperCase(), Math.min(Math.max(days, 1), 30));
    }
}
