package com.fintech.simulator.market.dto;

/**
 * 종목 뉴스 한 건.
 *
 * @param datetime 발행 시각 (unix epoch seconds)
 */
public record NewsItem(
        String headline,
        String source,
        String summary,
        String url,
        long datetime,
        String image
) {}
