package com.fintech.simulator.watchlist.controller;

import com.fintech.simulator.market.controller.StockResponse;
import com.fintech.simulator.watchlist.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관심종목 API (로그인 필요 — SecurityConfig 의 anyRequest().authenticated()).
 *
 * <ul>
 *   <li>GET    /watchlist                 — 내 관심종목 목록 (종목 정보 포함)</li>
 *   <li>GET    /watchlist/{ticker}/contains — 특정 종목이 내 관심종목인지</li>
 *   <li>POST   /watchlist/{ticker}         — 추가 (멱등)</li>
 *   <li>DELETE /watchlist/{ticker}         — 제거 (멱등)</li>
 * </ul>
 */
@RestController
@RequestMapping("/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public List<StockResponse> list(@AuthenticationPrincipal String userId) {
        return watchlistService.list(userId);
    }

    @GetMapping("/{ticker}/contains")
    public ContainsResponse contains(@AuthenticationPrincipal String userId,
                                     @PathVariable String ticker) {
        return new ContainsResponse(ticker, watchlistService.contains(userId, ticker));
    }

    @PostMapping("/{ticker}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@AuthenticationPrincipal String userId, @PathVariable String ticker) {
        watchlistService.add(userId, ticker);
    }

    @DeleteMapping("/{ticker}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal String userId, @PathVariable String ticker) {
        watchlistService.remove(userId, ticker);
    }

    public record ContainsResponse(String ticker, boolean watching) {}
}
