package com.fintech.simulator.market.provider.kis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * KIS REST `inquire-daily-itemchartprice` 응답.
 *
 * <pre>
 * GET /uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice
 *   tr_id=FHKST03010100
 *   FID_COND_MRKT_DIV_CODE=J
 *   FID_INPUT_ISCD=005930
 *   FID_INPUT_DATE_1=20250101
 *   FID_INPUT_DATE_2=20260101
 *   FID_PERIOD_DIV_CODE=D
 *   FID_ORG_ADJ_PRC=0
 * </pre>
 *
 * output2 가 일봉 배열. 페이지당 최대 100건. 최신 → 과거 순.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KisDailyChartResponse(
        @JsonProperty("rt_cd")  String rtCd,
        @JsonProperty("msg_cd") String msgCd,
        @JsonProperty("msg1")   String msg1,
        @JsonProperty("output2") List<KisDailyCandleItem> output2
) {

    public boolean isSuccess() { return "0".equals(rtCd); }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KisDailyCandleItem(
            @JsonProperty("stck_bsop_date") String tradeDate,   // YYYYMMDD
            @JsonProperty("stck_oprc")      String openPrice,
            @JsonProperty("stck_hgpr")      String highPrice,
            @JsonProperty("stck_lwpr")      String lowPrice,
            @JsonProperty("stck_clpr")      String closePrice,
            @JsonProperty("acml_vol")       String volume
    ) {}
}
