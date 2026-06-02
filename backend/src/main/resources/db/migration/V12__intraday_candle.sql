-- V12: 분봉(intraday) 캔들 — MarketPollingScheduler 가 분당 시세를 분 단위 버킷에 누적
-- UNIQUE(ticker, bucket_ts) 가 (ticker, bucket_ts) 조회 인덱스를 겸함 → 별도 인덱스 불필요.
CREATE TABLE INTRADAY_CANDLE (
    candle_id    NUMBER         GENERATED ALWAYS AS IDENTITY,
    ticker       VARCHAR2(20)   NOT NULL,
    bucket_ts    TIMESTAMP      NOT NULL,
    open_price   NUMBER(18, 4)  NOT NULL,
    high_price   NUMBER(18, 4)  NOT NULL,
    low_price    NUMBER(18, 4)  NOT NULL,
    close_price  NUMBER(18, 4)  NOT NULL,
    volume       NUMBER(20)     DEFAULT 0 NOT NULL,
    CONSTRAINT pk_intraday_candle    PRIMARY KEY (candle_id),
    CONSTRAINT uk_intraday_t_b       UNIQUE (ticker, bucket_ts),
    CONSTRAINT fk_intraday_stock     FOREIGN KEY (ticker) REFERENCES STOCKS(ticker),
    CONSTRAINT chk_intraday_hl       CHECK (high_price >= low_price)
);

COMMENT ON TABLE  INTRADAY_CANDLE           IS '분봉 — 분당 폴링 시세 누적 (보관 3일, 일일 purge)';
COMMENT ON COLUMN INTRADAY_CANDLE.bucket_ts IS '분 단위로 truncate된 시각';
