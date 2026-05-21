-- V6: AI 매매 코치 리포트 (PRD §8 AI_REPORTS)
CREATE TABLE AI_REPORTS (
    report_id      NUMBER         GENERATED ALWAYS AS IDENTITY,
    user_id        VARCHAR2(50)   NOT NULL,
    report_type    VARCHAR2(20)   NOT NULL,  -- TRADE_COMMENT | WEEKLY | INSTANT
    context_hash   VARCHAR2(64),               -- 응답 캐싱 키 (portfolio hash 등)
    content        CLOB           NOT NULL,
    token_used     NUMBER(10)     DEFAULT 0 NOT NULL,
    created_at     TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_ai_reports          PRIMARY KEY (report_id),
    CONSTRAINT fk_ai_reports_user     FOREIGN KEY (user_id) REFERENCES USERS(user_id),
    CONSTRAINT chk_ai_report_type     CHECK (report_type IN ('TRADE_COMMENT', 'WEEKLY', 'INSTANT'))
);
CREATE INDEX idx_ai_reports_user_type ON AI_REPORTS(user_id, report_type, created_at DESC);
CREATE INDEX idx_ai_reports_hash      ON AI_REPORTS(context_hash);
COMMENT ON TABLE AI_REPORTS IS 'AI 매매 코치 응답 (캐싱 + 토큰 사용량 누적)';
