-- V10: 일별 포트폴리오 자산 스냅샷 — 수익률 랭킹 + 내 자산 추이의 단일 소스
-- 배치가 하루 1회 사용자별 총 자산을 기록. UNIQUE(user_id, snapshot_date) UPSERT.
CREATE TABLE PORTFOLIO_SNAPSHOT (
    snapshot_id      NUMBER         GENERATED ALWAYS AS IDENTITY,
    user_id          VARCHAR2(50)   NOT NULL,
    snapshot_date    DATE           NOT NULL,
    total_asset_krw  NUMBER(20, 2)  NOT NULL,
    cash_krw         NUMBER(20, 2)  NOT NULL,
    holding_krw      NUMBER(20, 2)  NOT NULL,
    pnl_krw          NUMBER(20, 2)  NOT NULL,
    return_pct       NUMBER(12, 4)  NOT NULL,   -- 시드머니 대비 수익률 %
    created_at       TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_portfolio_snapshot   PRIMARY KEY (snapshot_id),
    CONSTRAINT uk_ps_user_date         UNIQUE (user_id, snapshot_date),
    CONSTRAINT fk_ps_user              FOREIGN KEY (user_id) REFERENCES USERS(user_id)
);

COMMENT ON TABLE  PORTFOLIO_SNAPSHOT             IS '일별 자산 스냅샷 (랭킹 + 자산 추이)';
COMMENT ON COLUMN PORTFOLIO_SNAPSHOT.return_pct  IS '시드머니 대비 수익률 % = (total-seed)/seed*100';

-- 내 추이 조회 (user 필터 + 날짜 정렬)
CREATE INDEX idx_ps_user_date  ON PORTFOLIO_SNAPSHOT(user_id, snapshot_date);
-- 랭킹 조회 (특정 날짜의 수익률 내림차순)
CREATE INDEX idx_ps_date_return ON PORTFOLIO_SNAPSHOT(snapshot_date, return_pct);
