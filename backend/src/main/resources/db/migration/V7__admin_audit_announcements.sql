-- V7: 관리자 감사 로그 + 공지사항
CREATE TABLE ADMIN_AUDIT_LOGS (
    audit_id         NUMBER         GENERATED ALWAYS AS IDENTITY,
    admin_user_id    VARCHAR2(50)   NOT NULL,
    action           VARCHAR2(50)   NOT NULL,
    target_type      VARCHAR2(50)   NOT NULL,
    target_id        VARCHAR2(100),
    before_value     CLOB,
    after_value      CLOB,
    reason           VARCHAR2(500),
    ip_address       VARCHAR2(45),
    user_agent       VARCHAR2(500),
    created_at       TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_admin_audit_logs       PRIMARY KEY (audit_id),
    CONSTRAINT fk_admin_audit_logs_user  FOREIGN KEY (admin_user_id) REFERENCES USERS(user_id)
);
CREATE INDEX idx_aal_admin_created ON ADMIN_AUDIT_LOGS(admin_user_id, created_at DESC);
CREATE INDEX idx_aal_target        ON ADMIN_AUDIT_LOGS(target_type, target_id);

CREATE TABLE ANNOUNCEMENTS (
    announcement_id  NUMBER         GENERATED ALWAYS AS IDENTITY,
    admin_user_id    VARCHAR2(50)   NOT NULL,
    title            VARCHAR2(200)  NOT NULL,
    content          CLOB           NOT NULL,
    -- Oracle에서 LEVEL은 CONNECT BY 의사컬럼이라 CHECK 식에서 모호 → severity 사용
    severity         VARCHAR2(20)   DEFAULT 'INFO' NOT NULL,
    is_active        NUMBER(1)      DEFAULT 1 NOT NULL,
    starts_at        TIMESTAMP,
    ends_at          TIMESTAMP,
    created_at       TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_announcements          PRIMARY KEY (announcement_id),
    CONSTRAINT fk_announcements_user     FOREIGN KEY (admin_user_id) REFERENCES USERS(user_id),
    CONSTRAINT chk_announcement_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT chk_announcement_active CHECK (is_active IN (0, 1))
);
CREATE INDEX idx_ann_active_range ON ANNOUNCEMENTS(is_active, starts_at, ends_at);
