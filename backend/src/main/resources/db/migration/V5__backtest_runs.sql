-- V5: 백테스트 실행 결과 영구 저장
CREATE TABLE BACKTEST_RUNS (
    run_id            NUMBER         GENERATED ALWAYS AS IDENTITY,
    user_id           VARCHAR2(50)   NOT NULL,
    strategy_name     VARCHAR2(50)   NOT NULL,
    strategy_params   CLOB,
    ticker            VARCHAR2(20)   NOT NULL,
    start_date        DATE           NOT NULL,
    end_date          DATE           NOT NULL,
    initial_capital   NUMBER(18, 2)  NOT NULL,
    final_value       NUMBER(18, 2)  NOT NULL,
    total_return      NUMBER(10, 6)  NOT NULL,
    mdd               NUMBER(10, 6)  NOT NULL,
    sharpe            NUMBER(10, 6)  NOT NULL,
    trade_count       NUMBER(10)     NOT NULL,
    win_rate          NUMBER(5, 4)   NOT NULL,
    result_detail     CLOB,
    created_at        TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_backtest_runs       PRIMARY KEY (run_id),
    CONSTRAINT fk_backtest_runs_user  FOREIGN KEY (user_id) REFERENCES USERS(user_id),
    CONSTRAINT fk_backtest_runs_stock FOREIGN KEY (ticker)  REFERENCES STOCKS(ticker)
);
CREATE INDEX idx_bt_user_created ON BACKTEST_RUNS(user_id, created_at DESC);
COMMENT ON TABLE BACKTEST_RUNS IS '백테스트 실행 결과 (result_detail에 자산 곡선 + 매매 시점 JSON 저장)';
