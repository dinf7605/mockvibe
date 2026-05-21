-- gvenzl/oracle-xe 이미지는 APP_USER(simulator)를 XEPDB1에 자동 생성하고
-- CREATE SESSION / CREATE TABLE / UNLIMITED TABLESPACE 기본 권한을 부여한다.
-- init 스크립트는 PDB(XEPDB1) 컨텍스트에서 sqlplus로 실행되므로
-- ALTER SESSION SET CONTAINER 같은 작업은 필요하지 않다.
-- 여기서는 매매·시퀀스·뷰 작업에 필요한 추가 권한만 부여한다 (idempotent).

GRANT CREATE SEQUENCE  TO simulator;
GRANT CREATE PROCEDURE TO simulator;
GRANT CREATE TRIGGER   TO simulator;
GRANT CREATE VIEW      TO simulator;

EXIT;
