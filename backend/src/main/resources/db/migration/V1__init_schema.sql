-- ============================================================================
-- V1: 핵심 도메인 스키마
-- 대상 테이블: USERS, WALLET, STOCKS, HOLDINGS, ORDERS, FX_RATES
-- - LIMIT_ORDERS, PRICE_HISTORY, BACKTEST_RUNS, AI_REPORTS, ADMIN_AUDIT_LOGS,
--   ANNOUNCEMENTS, WATCHLIST는 해당 Phase에서 V2~ 마이그레이션으로 추가한다.
-- - DDL은 트랜잭션 묶음이 아니라 각 문장이 즉시 commit 되므로,
--   세미콜론 단위로 작성. Oracle 21c XE 기준.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- USERS: 회원
-- user_id는 UUID 문자열을 권장 (애플리케이션에서 생성)
-- ----------------------------------------------------------------------------
CREATE TABLE USERS (
    user_id         VARCHAR2(50)  NOT NULL,
    password        VARCHAR2(100) NOT NULL,
    username        VARCHAR2(50)  NOT NULL,
    email           VARCHAR2(100) NOT NULL,
    role            VARCHAR2(10)  DEFAULT 'USER'   NOT NULL,
    status          VARCHAR2(10)  DEFAULT 'ACTIVE' NOT NULL,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_users           PRIMARY KEY (user_id),
    CONSTRAINT uk_users_email     UNIQUE (email),
    CONSTRAINT chk_users_role     CHECK (role   IN ('USER', 'ADMIN')),
    CONSTRAINT chk_users_status   CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

COMMENT ON TABLE  USERS               IS '회원 정보';
COMMENT ON COLUMN USERS.user_id       IS 'UUID 형태 식별자';
COMMENT ON COLUMN USERS.password      IS 'BCrypt 해시 (cost 12)';
COMMENT ON COLUMN USERS.role          IS 'USER | ADMIN';
COMMENT ON COLUMN USERS.status        IS 'ACTIVE | SUSPENDED';

CREATE INDEX idx_users_role   ON USERS(role);
CREATE INDEX idx_users_status ON USERS(status);

-- ----------------------------------------------------------------------------
-- WALLET: 예수금 지갑 (1 user : 1 wallet)
-- 비관적 락(@Lock PESSIMISTIC_WRITE) 대상
-- ----------------------------------------------------------------------------
CREATE TABLE WALLET (
    wallet_id       NUMBER         GENERATED ALWAYS AS IDENTITY,
    user_id         VARCHAR2(50)   NOT NULL,
    cash_balance    NUMBER(18, 2)  DEFAULT 0 NOT NULL,
    updated_at      TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_wallet          PRIMARY KEY (wallet_id),
    CONSTRAINT uk_wallet_user     UNIQUE (user_id),
    CONSTRAINT fk_wallet_user     FOREIGN KEY (user_id) REFERENCES USERS(user_id),
    CONSTRAINT chk_wallet_balance CHECK (cash_balance >= 0)
);

COMMENT ON TABLE  WALLET              IS '사용자 예수금 (KRW 단일 지갑)';
COMMENT ON COLUMN WALLET.cash_balance IS '예수금 KRW';

-- ----------------------------------------------------------------------------
-- STOCKS: 종목 마스터
-- 한국 30 + 미국 30 = 60종목 (D08에서 시드)
-- ----------------------------------------------------------------------------
CREATE TABLE STOCKS (
    ticker          VARCHAR2(20)   NOT NULL,
    market          VARCHAR2(10)   NOT NULL,
    currency        VARCHAR2(10)   NOT NULL,
    company_name    VARCHAR2(100)  NOT NULL,
    sector          VARCHAR2(50),
    region          VARCHAR2(10),
    current_price   NUMBER(18, 4),
    tick_size       NUMBER(18, 4)  DEFAULT 0.01 NOT NULL,
    is_active       NUMBER(1)      DEFAULT 1    NOT NULL,
    updated_at      TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_stocks            PRIMARY KEY (ticker),
    CONSTRAINT chk_stocks_market    CHECK (market   IN ('KRX', 'NASDAQ', 'NYSE')),
    CONSTRAINT chk_stocks_currency  CHECK (currency IN ('KRW', 'USD')),
    CONSTRAINT chk_stocks_active    CHECK (is_active IN (0, 1))
);

COMMENT ON TABLE  STOCKS              IS '종목 마스터';
COMMENT ON COLUMN STOCKS.ticker       IS '예: 005930, AAPL';
COMMENT ON COLUMN STOCKS.region       IS 'KR | US (집중도 계산용)';
COMMENT ON COLUMN STOCKS.is_active    IS '0=비활성(매매 차단) / 1=활성';

CREATE INDEX idx_stocks_market   ON STOCKS(market);
CREATE INDEX idx_stocks_sector   ON STOCKS(sector);
CREATE INDEX idx_stocks_active   ON STOCKS(is_active);

-- ----------------------------------------------------------------------------
-- HOLDINGS: 보유 종목 (user × ticker 유일)
-- 낙관적 락(@Version) 대상 — version 컬럼
-- ----------------------------------------------------------------------------
CREATE TABLE HOLDINGS (
    holding_id          NUMBER         GENERATED ALWAYS AS IDENTITY,
    user_id             VARCHAR2(50)   NOT NULL,
    ticker              VARCHAR2(20)   NOT NULL,
    quantity            NUMBER(18, 4)  DEFAULT 0 NOT NULL,
    average_price_krw   NUMBER(18, 2)  DEFAULT 0 NOT NULL,
    version             NUMBER(10)     DEFAULT 0 NOT NULL,
    updated_at          TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_holdings              PRIMARY KEY (holding_id),
    CONSTRAINT uk_holdings_user_ticker  UNIQUE (user_id, ticker),
    CONSTRAINT fk_holdings_user         FOREIGN KEY (user_id) REFERENCES USERS(user_id),
    CONSTRAINT fk_holdings_stock        FOREIGN KEY (ticker)  REFERENCES STOCKS(ticker),
    CONSTRAINT chk_holdings_quantity    CHECK (quantity >= 0),
    CONSTRAINT chk_holdings_avg         CHECK (average_price_krw >= 0)
);

COMMENT ON TABLE  HOLDINGS                    IS '보유 종목';
COMMENT ON COLUMN HOLDINGS.average_price_krw  IS '매수 평균단가(KRW 환산) — 미국 종목도 KRW 기준';
COMMENT ON COLUMN HOLDINGS.version            IS '낙관적 락 @Version';

-- ----------------------------------------------------------------------------
-- ORDERS: 거래 내역 (체결 완료)
-- ----------------------------------------------------------------------------
CREATE TABLE ORDERS (
    order_id          NUMBER         GENERATED ALWAYS AS IDENTITY,
    user_id           VARCHAR2(50)   NOT NULL,
    ticker            VARCHAR2(20)   NOT NULL,
    order_type        VARCHAR2(10)   NOT NULL,
    order_method      VARCHAR2(10)   NOT NULL,
    price             NUMBER(18, 4)  NOT NULL,
    quantity          NUMBER(18, 4)  NOT NULL,
    fx_rate           NUMBER(18, 6)  DEFAULT 1 NOT NULL,
    fee               NUMBER(18, 4)  DEFAULT 0 NOT NULL,
    total_amount_krw  NUMBER(18, 2)  NOT NULL,
    created_at        TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_orders            PRIMARY KEY (order_id),
    CONSTRAINT fk_orders_user       FOREIGN KEY (user_id) REFERENCES USERS(user_id),
    CONSTRAINT fk_orders_stock      FOREIGN KEY (ticker)  REFERENCES STOCKS(ticker),
    CONSTRAINT chk_orders_type      CHECK (order_type   IN ('BUY', 'SELL')),
    CONSTRAINT chk_orders_method    CHECK (order_method IN ('MARKET', 'LIMIT')),
    CONSTRAINT chk_orders_quantity  CHECK (quantity > 0),
    CONSTRAINT chk_orders_price     CHECK (price > 0)
);

COMMENT ON TABLE  ORDERS                   IS '체결된 거래 내역 (불변, 정정/취소 없음)';
COMMENT ON COLUMN ORDERS.price             IS '체결가 (종목 통화 단위)';
COMMENT ON COLUMN ORDERS.fx_rate           IS '체결 시점의 통화 환율 (USD→KRW)';
COMMENT ON COLUMN ORDERS.total_amount_krw  IS 'price * quantity * fx_rate + fee (KRW 환산)';

CREATE INDEX idx_orders_user_created   ON ORDERS(user_id, created_at DESC);
CREATE INDEX idx_orders_ticker_created ON ORDERS(ticker,  created_at DESC);

-- ----------------------------------------------------------------------------
-- FX_RATES: 환율 시계열
-- 최신 환율은 (base, quote, fetched_at DESC) 인덱스 1건 조회로 해결
-- ----------------------------------------------------------------------------
CREATE TABLE FX_RATES (
    fx_id           NUMBER         GENERATED ALWAYS AS IDENTITY,
    base_currency   VARCHAR2(10)   NOT NULL,
    quote_currency  VARCHAR2(10)   NOT NULL,
    rate            NUMBER(18, 6)  NOT NULL,
    fetched_at      TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_fx_rates       PRIMARY KEY (fx_id),
    CONSTRAINT chk_fx_rate       CHECK (rate > 0)
);

COMMENT ON TABLE FX_RATES IS '환율 시계열 캐시 (1분마다 갱신)';

CREATE INDEX idx_fx_lookup ON FX_RATES(base_currency, quote_currency, fetched_at DESC);
