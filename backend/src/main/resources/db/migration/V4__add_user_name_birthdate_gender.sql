-- V4: USER 테이블에 이름(name), 생년월일(birth_date), 성별(gender) 컬럼 추가
ALTER TABLE `USER` ADD COLUMN name VARCHAR(100) NULL AFTER nickname;
ALTER TABLE `USER` ADD COLUMN birth_date DATE NULL AFTER student_id;
ALTER TABLE `USER` ADD COLUMN gender ENUM('MALE','FEMALE','OTHER') NULL AFTER birth_date;
