-- V2: SLOT 테이블에 match_preferences 컬럼 추가
ALTER TABLE SLOT ADD COLUMN match_preferences JSON NULL AFTER ideal_type;
