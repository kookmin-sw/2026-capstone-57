-- V5: personality_type을 VARCHAR에서 JSON으로 변경 (복수 선택 지원)
--     ideal_type_preferences를 단순 JSON 배열로 변경
ALTER TABLE `USER` MODIFY COLUMN personality_type JSON NULL;
-- ideal_type_preferences는 이미 JSON 타입이므로 스키마 변경 불필요 (데이터 구조만 변경)
