-- V3: 지정가 주문
CREATE TABLE LIMIT_ORDERS (
    limit_order_id   NUMBER         GENERATED ALWAYS AS IDENTITY,
    user_id          VARCHAR2(50)   NOT NULL,
    ticker           VARCHAR2(20)   NOT NULL,
    order_type       VARCHAR2(10)   NOT NULL,
    target_price     NUMBER(18, 4)  NOT NULL,
    quantity         NUMBER(18, 4)  NOT NULL,
    status           VARCHAR2(20)   DEFAULT 'PENDING' NOT NULL,
    expires_at       TIMESTAMP      NOT NULL,
    filled_at        TIMESTAMP,
    cancelled_at     TIMESTAMP,
    filled_order_id  NUMBER,
    created_at       TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_limit_orders       PRIMARY KEY (limit_order_id),
    CONSTRAINT fk_limit_orders_user  FOREIGN KEY (user_id) REFERENCES USERS(user_id),
    CONSTRAINT fk_limit_orders_stock FOREIGN KEY (ticker)  REFERENCES STOCKS(ticker),
    CONSTRAINT chk_lo_type           CHECK (order_type IN ('BUY', 'SELL')),
    CONSTRAINT chk_lo_status         CHECK (status     IN ('PENDING', 'FILLED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_lo_qty            CHECK (quantity > 0),
    CONSTRAINT chk_lo_price          CHECK (target_price > 0)
);

COMMENT ON TABLE  LIMIT_ORDERS              IS '지정가 예약 주문';
COMMENT ON COLUMN LIMIT_ORDERS.status       IS 'PENDING | FILLED | CANCELLED | EXPIRED';
COMMENT ON COLUMN LIMIT_ORDERS.filled_order_id IS '체결 시 생성된 ORDERS.order_id 참조';

-- 이벤트 기반 체결 후보 조회용 (D22 LimitOrderProcessor)
CREATE INDEX idx_lo_ticker_status_price ON LIMIT_ORDERS(ticker, status, target_price);
-- 사용자 목록 조회
CREATE INDEX idx_lo_user_created        ON LIMIT_ORDERS(user_id, created_at DESC);
-- 만료 배치 (D24)
CREATE INDEX idx_lo_status_expires      ON LIMIT_ORDERS(status, expires_at);
