-- =============================================
-- 캠퍼스 공간 데이터 구조 재설계
-- campus_building: 건물 이름만 관리
-- campus_venue: 건물 간 이동 중간 거점 (이름만)
-- campus_path: 건물 간 이동 동선 (from, to, venue FK)
-- place: 건물 내부 장소 (신규)
-- =============================================

-- 1. campus_path 외래키 제거 (campus_building, campus_venue 변경 전)
ALTER TABLE CAMPUS_PATH DROP FOREIGN KEY fk_campus_path_from;
ALTER TABLE CAMPUS_PATH DROP FOREIGN KEY fk_campus_path_to;

-- 2. campus_venue 외래키 제거
ALTER TABLE CAMPUS_VENUE DROP FOREIGN KEY fk_campus_venue_building;

-- 3. campus_building 재설계: name만 남기기
ALTER TABLE CAMPUS_BUILDING
    DROP INDEX idx_campus_building_purpose,
    DROP COLUMN latitude,
    DROP COLUMN longitude,
    DROP COLUMN purpose,
    DROP COLUMN description;

-- 4. campus_venue 재설계: name만 남기기 (건물 간 이동 중간 거점)
ALTER TABLE CAMPUS_VENUE
    DROP INDEX idx_campus_venue_type_suitability,
    DROP INDEX idx_campus_venue_building,
    DROP COLUMN building_id,
    DROP COLUMN latitude,
    DROP COLUMN longitude,
    DROP COLUMN type,
    DROP COLUMN meeting_suitability,
    DROP COLUMN operating_hours,
    DROP COLUMN characteristics;

-- 5. campus_path 재설계: unique key 제거 후 컬럼 변경
ALTER TABLE CAMPUS_PATH DROP INDEX uk_campus_path_from_to;

ALTER TABLE CAMPUS_PATH
    DROP COLUMN walking_time_minutes,
    DROP COLUMN passing_venue_ids,
    DROP COLUMN description;

ALTER TABLE CAMPUS_PATH
    ADD COLUMN venue_id BINARY(16) NOT NULL AFTER to_building_id;

-- 6. campus_path 외래키 재설정
ALTER TABLE CAMPUS_PATH
    ADD CONSTRAINT fk_campus_path_from FOREIGN KEY (from_building_id) REFERENCES CAMPUS_BUILDING(id),
    ADD CONSTRAINT fk_campus_path_to FOREIGN KEY (to_building_id) REFERENCES CAMPUS_BUILDING(id),
    ADD CONSTRAINT fk_campus_path_venue FOREIGN KEY (venue_id) REFERENCES CAMPUS_VENUE(id);

-- 7. place 테이블 생성 (건물 내부 장소)
CREATE TABLE PLACE (
    id BINARY(16) NOT NULL,
    building_id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    floor INT NOT NULL,
    type VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_place_building (building_id),
    INDEX idx_place_type (type),
    CONSTRAINT fk_place_building FOREIGN KEY (building_id) REFERENCES CAMPUS_BUILDING(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
