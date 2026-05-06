-- SCHEDULE 테이블에 campus_building_id, floor 컬럼 추가
ALTER TABLE `SCHEDULE`
    ADD COLUMN campus_building_id BINARY(16) NULL AFTER place,
    ADD COLUMN floor INT NULL AFTER campus_building_id;

ALTER TABLE `SCHEDULE`
    ADD CONSTRAINT fk_schedule_campus_building
        FOREIGN KEY (campus_building_id) REFERENCES CAMPUS_BUILDING(id);
