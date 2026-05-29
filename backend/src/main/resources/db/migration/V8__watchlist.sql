-- V8: 관심종목 (워치리스트) — 사용자별 관심 종목 즐겨찾기
-- 한 사용자가 같은 종목을 중복 등록 못 하도록 UNIQUE(user_id, ticker).
CREATE TABLE WATCHLIST (
    watchlist_id  NUMBER         GENERATED ALWAYS AS IDENTITY,
    user_id       VARCHAR2(50)   NOT NULL,
    ticker        VARCHAR2(20)   NOT NULL,
    created_at    TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_watchlist        PRIMARY KEY (watchlist_id),
    CONSTRAINT uk_watchlist_u_t    UNIQUE (user_id, ticker),
    CONSTRAINT fk_watchlist_user   FOREIGN KEY (user_id) REFERENCES USERS(user_id),
    CONSTRAINT fk_watchlist_stock  FOREIGN KEY (ticker)  REFERENCES STOCKS(ticker)
);

COMMENT ON TABLE  WATCHLIST            IS '사용자별 관심종목 즐겨찾기';
COMMENT ON COLUMN WATCHLIST.user_id    IS 'USERS.user_id 참조';
COMMENT ON COLUMN WATCHLIST.ticker     IS 'STOCKS.ticker 참조';

-- 목록 조회는 user_id 로 필터하므로 인덱스
CREATE INDEX idx_watchlist_user ON WATCHLIST(user_id);
