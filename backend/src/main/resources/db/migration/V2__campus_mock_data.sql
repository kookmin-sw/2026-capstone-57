-- =============================================
-- V2: Campus Mock Data
-- =============================================

-- 건물
INSERT INTO CAMPUS_BUILDING (id, name, created_at, updated_at) VALUES
    (UUID_TO_BIN(UUID()), '미래관', NOW(), NOW()),
    (UUID_TO_BIN(UUID()), '북악관', NOW(), NOW());

-- 장소 (venue)
INSERT INTO CAMPUS_VENUE (id, name, created_at, updated_at) VALUES
    (UUID_TO_BIN(UUID()), '용두리', NOW(), NOW()),
    (UUID_TO_BIN(UUID()), '농구장', NOW(), NOW()),
    (UUID_TO_BIN(UUID()), '운동장', NOW(), NOW()),
    (UUID_TO_BIN(UUID()), '예대 매점', NOW(), NOW());

-- 경로
INSERT INTO CAMPUS_PATH (id, from_building_id, to_building_id, venue_id, created_at, updated_at) VALUES
    (UUID_TO_BIN(UUID()),
     (SELECT id FROM CAMPUS_BUILDING WHERE name = '미래관'),
     (SELECT id FROM CAMPUS_BUILDING WHERE name = '북악관'),
     (SELECT id FROM CAMPUS_VENUE WHERE name = '용두리'),
     NOW(), NOW()),
    (UUID_TO_BIN(UUID()),
     (SELECT id FROM CAMPUS_BUILDING WHERE name = '미래관'),
     (SELECT id FROM CAMPUS_BUILDING WHERE name = '북악관'),
     (SELECT id FROM CAMPUS_VENUE WHERE name = '예대 매점'),
     NOW(), NOW());

-- 건물 내 장소 (place)
INSERT INTO CAMPUS_BUILDING_PLACE (id, building_id, name, floor, type, created_at, updated_at) VALUES
    (UUID_TO_BIN(UUID()),
     (SELECT id FROM CAMPUS_BUILDING WHERE name = '미래관'),
     '자주스', 4, 'STUDY_ROOM', NOW(), NOW()),
    (UUID_TO_BIN(UUID()),
     (SELECT id FROM CAMPUS_BUILDING WHERE name = '미래관'),
     '과방', 3, 'MEETING_ROOM', NOW(), NOW()),
    (UUID_TO_BIN(UUID()),
     (SELECT id FROM CAMPUS_BUILDING WHERE name = '북악관'),
     '편의점', 1, 'CONVENIENCE_STORE', NOW(), NOW());
