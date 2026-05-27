package com.fintech.simulator.market.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 거래소 장 시간 판별. 백그라운드 폴링이 "지금 어느 시장이 열려있나" 를 보고
 * 해당 시장 종목만 호출하도록 한다 (불필요한 외부 호출·rate limit 절약).
 *
 * - KRX: 평일 09:00 ~ 15:30 KST
 * - US : 평일 09:30 ~ 16:00 ET (America/New_York — 서머타임 자동)
 *
 * 공휴일은 고려하지 않는다 (모의투자 — 휴장일 호출해도 직전 종가 반환).
 */
@Service
public class MarketHoursService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final ZoneId ET  = ZoneId.of("America/New_York");

    public boolean isKrxOpen() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        if (isWeekend(now)) return false;
        LocalTime t = now.toLocalTime();
        return !t.isBefore(LocalTime.of(9, 0)) && !t.isAfter(LocalTime.of(15, 30));
    }

    public boolean isUsOpen() {
        ZonedDateTime now = ZonedDateTime.now(ET);
        if (isWeekend(now)) return false;
        LocalTime t = now.toLocalTime();
        return !t.isBefore(LocalTime.of(9, 30)) && !t.isAfter(LocalTime.of(16, 0));
    }

    private boolean isWeekend(ZonedDateTime z) {
        DayOfWeek d = z.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }
}
