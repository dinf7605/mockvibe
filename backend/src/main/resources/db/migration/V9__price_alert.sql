-- V9: 가격 알림 — 사용자가 종목별 목표가를 걸어두고 도달 시 알림 받기
-- direction: ABOVE(목표가 이상 도달) / BELOW(목표가 이하 도달)
-- status   : ACTIVE(감시중) / TRIGGERED(도달) / CANCELLED(취소)
CREATE TABLE PRICE_ALERT (
    alert_id        NUMBER         GENERATED ALWAYS AS IDENTITY,
    user_id         VARCHAR2(50)   NOT NULL,
    ticker          VARCHAR2(20)   NOT NULL,
    direction       VARCHAR2(10)   NOT NULL,
    target_price    NUMBER(18, 4)  NOT NULL,
    status          VARCHAR2(10)   DEFAULT 'ACTIVE' NOT NULL,
    triggered_price NUMBER(18, 4),
    created_at      TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    triggered_at    TIMESTAMP,
    CONSTRAINT pk_price_alert        PRIMARY KEY (alert_id),
    CONSTRAINT fk_price_alert_user   FOREIGN KEY (user_id) REFERENCES USERS(user_id),
    CONSTRAINT fk_price_alert_stock  FOREIGN KEY (ticker)  REFERENCES STOCKS(ticker),
    CONSTRAINT chk_pa_direction      CHECK (direction IN ('ABOVE', 'BELOW')),
    CONSTRAINT chk_pa_status         CHECK (status    IN ('ACTIVE', 'TRIGGERED', 'CANCELLED')),
    CONSTRAINT chk_pa_target_pos     CHECK (target_price > 0)
);

COMMENT ON TABLE  PRICE_ALERT             IS '사용자 가격 알림 (목표가 도달 감시)';
COMMENT ON COLUMN PRICE_ALERT.direction   IS 'ABOVE | BELOW';
COMMENT ON COLUMN PRICE_ALERT.status      IS 'ACTIVE | TRIGGERED | CANCELLED';

-- 시세 갱신마다 (ticker, ACTIVE) 로 후보 조회 → 복합 인덱스
CREATE INDEX idx_price_alert_t_s  ON PRICE_ALERT(ticker, status);
-- 사용자 목록 조회
CREATE INDEX idx_price_alert_user ON PRICE_ALERT(user_id);
