-- V11: 통합 알림 센터 — 가격 알림 도달 / 지정가 체결 / AI 코멘트를 한 피드로
CREATE TABLE NOTIFICATION (
    notification_id  NUMBER         GENERATED ALWAYS AS IDENTITY,
    user_id          VARCHAR2(50)   NOT NULL,
    type             VARCHAR2(20)   NOT NULL,
    title            VARCHAR2(200)  NOT NULL,
    body             VARCHAR2(1000),
    link             VARCHAR2(200),
    is_read          NUMBER(1)      DEFAULT 0 NOT NULL,
    created_at       TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_notification     PRIMARY KEY (notification_id),
    CONSTRAINT fk_notif_user       FOREIGN KEY (user_id) REFERENCES USERS(user_id),
    CONSTRAINT chk_notif_type      CHECK (type IN ('PRICE_ALERT', 'LIMIT_FILL', 'AI_COMMENT')),
    CONSTRAINT chk_notif_read      CHECK (is_read IN (0, 1))
);

COMMENT ON TABLE  NOTIFICATION       IS '사용자 알림 피드 (가격알림/지정가체결/AI코멘트)';
COMMENT ON COLUMN NOTIFICATION.type  IS 'PRICE_ALERT | LIMIT_FILL | AI_COMMENT';
COMMENT ON COLUMN NOTIFICATION.link  IS '클릭 시 이동 경로 (예: /stocks/AAPL)';

-- 목록 조회 (user + 최신순)
CREATE INDEX idx_notif_user_created ON NOTIFICATION(user_id, created_at);
-- 미확인 개수 (user + is_read)
CREATE INDEX idx_notif_user_read    ON NOTIFICATION(user_id, is_read);
