-- V2: USER 테이블에 전공(major), 학번(student_id) 컬럼 추가
ALTER TABLE `USER` ADD COLUMN major VARCHAR(255) NULL AFTER university;
ALTER TABLE `USER` ADD COLUMN student_id VARCHAR(50) NULL AFTER major;
