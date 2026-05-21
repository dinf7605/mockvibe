-- V4: 일별 OHLC 시계열 (백테스트·리스크 계산용)
CREATE TABLE PRICE_HISTORY (
    history_id   NUMBER         GENERATED ALWAYS AS IDENTITY,
    ticker       VARCHAR2(20)   NOT NULL,
    trade_date   DATE           NOT NULL,
    open_price   NUMBER(18, 4)  NOT NULL,
    high_price   NUMBER(18, 4)  NOT NULL,
    low_price    NUMBER(18, 4)  NOT NULL,
    close_price  NUMBER(18, 4)  NOT NULL,
    volume       NUMBER(20)     DEFAULT 0 NOT NULL,
    CONSTRAINT pk_price_history          PRIMARY KEY (history_id),
    CONSTRAINT uk_price_history_t_d      UNIQUE (ticker, trade_date),
    CONSTRAINT fk_price_history_stock    FOREIGN KEY (ticker) REFERENCES STOCKS(ticker),
    CONSTRAINT chk_ph_high_low           CHECK (high_price >= low_price)
);
COMMENT ON TABLE PRICE_HISTORY IS '일별 OHLC (D26 적재, D27~D29 리스크 계산, D31~ 백테스트)';
