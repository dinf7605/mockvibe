-- V10: 일별 포트폴리오 자산 스냅샷 — 수익률 랭킹 + 내 자산 추이의 단일 소스
-- 배치가 하루 1회 사용자별 총 자산을 기록. UNIQUE(user_id, snapshot_date) UPSERT.
--
-- 자가복구: 직전 시도에서 부분 생성(ORA-01408)된 테이블이 있으면 드롭 후 재생성.
-- (DDL 자동커밋이라 실패해도 테이블이 남을 수 있음. 실패 마이그레이션은 repair 로 이력 정리됨)
DECLARE
  table_missing EXCEPTION;
  PRAGMA EXCEPTION_INIT(table_missing, -942);
BEGIN
  EXECUTE IMMEDIATE 'DROP TABLE PORTFOLIO_SNAPSHOT CASCADE CONSTRAINTS';
EXCEPTION WHEN table_missing THEN NULL;
END;
/

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

-- 랭킹 조회 (특정 날짜의 수익률 내림차순). 내 추이(user_id 선두) 조회는 uk_ps_user_date 인덱스가 커버.
CREATE INDEX idx_ps_date_return ON PORTFOLIO_SNAPSHOT(snapshot_date, return_pct);
