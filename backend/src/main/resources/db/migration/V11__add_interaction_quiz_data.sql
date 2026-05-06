-- 퀴즈 데이터를 InteractionEntity에 JSON으로 저장
ALTER TABLE INTERACTION ADD COLUMN quiz_data JSON NULL AFTER quiz_completed_by;
