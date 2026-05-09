-- =============================================
-- Mock Data for Development
-- Hibernate가 테이블 생성 후 자동 실행됨
-- =============================================

-- 건물 (campus_building)
INSERT IGNORE INTO campus_building (id, name, created_at, updated_at) VALUES
    (UUID_TO_BIN(UUID()), '미래관', NOW(), NOW()),
    (UUID_TO_BIN(UUID()), '북악관', NOW(), NOW());

-- 장소 (campus_venue)
INSERT IGNORE INTO campus_venue (id, name, created_at, updated_at) VALUES
    (UUID_TO_BIN(UUID()), '용두리', NOW(), NOW()),
    (UUID_TO_BIN(UUID()), '농구장', NOW(), NOW()),
    (UUID_TO_BIN(UUID()), '운동장', NOW(), NOW()),
    (UUID_TO_BIN(UUID()), '예대 매점', NOW(), NOW());

-- 경로 (campus_path)
INSERT IGNORE INTO campus_path (id, from_building_id, to_building_id, venue_id, created_at, updated_at) VALUES
    (UUID_TO_BIN(UUID()),
     (SELECT id FROM campus_building WHERE name = '미래관'),
     (SELECT id FROM campus_building WHERE name = '북악관'),
     (SELECT id FROM campus_venue WHERE name = '용두리'),
     NOW(), NOW()),
    (UUID_TO_BIN(UUID()),
     (SELECT id FROM campus_building WHERE name = '미래관'),
     (SELECT id FROM campus_building WHERE name = '북악관'),
     (SELECT id FROM campus_venue WHERE name = '예대 매점'),
     NOW(), NOW());

-- 건물 내 장소 (campus_building_place)
INSERT IGNORE INTO campus_building_place (id, building_id, name, floor, type, created_at, updated_at) VALUES
    (UUID_TO_BIN(UUID()),
     (SELECT id FROM campus_building WHERE name = '미래관'),
     '자주스', 4, 'STUDY_ROOM', NOW(), NOW()),
    (UUID_TO_BIN(UUID()),
     (SELECT id FROM campus_building WHERE name = '미래관'),
     '과방', 3, 'MEETING_ROOM', NOW(), NOW()),
    (UUID_TO_BIN(UUID()),
     (SELECT id FROM campus_building WHERE name = '북악관'),
     '편의점', 1, 'CONVENIENCE_STORE', NOW(), NOW());

-- 유저 (user) - 10명
-- 모든 유저 비밀번호: password123
INSERT IGNORE INTO `user` (id, email, password_hash, nickname, name, university, major, student_id, birth_date, gender, hobbies, interests, personality_type, ideal_type_preferences, total_exp, current_level, is_suspended, created_at, updated_at) VALUES
    (UUID_TO_BIN(UUID()), 'user1@kookmin.ac.kr', '$2a$10$1jNkCCorydAd9TyBXBUnbuNUB9RQR4uP.w4diACrmS/Ialn9A3uAW',
     '하늘이', '김하늘', '국민대학교', '소프트웨어학부', '20210001', '2002-03-15', 'FEMALE',
     '["READING", "YOGA", "CAFE_HOPPING"]', '["AI_ML", "DESIGN"]', '["INFP"]', '["KIND", "FUNNY"]',
     0, 1, FALSE, NOW(), NOW()),
    (UUID_TO_BIN(UUID()), 'user2@kookmin.ac.kr', '$2a$10$1jNkCCorydAd9TyBXBUnbuNUB9RQR4uP.w4diACrmS/Ialn9A3uAW',
     '민수', '이민수', '국민대학교', '경영학부', '20200042', '2001-07-22', 'MALE',
     '["FITNESS", "GAMING", "CAFE_HOPPING"]', '["STARTUP", "ECONOMICS"]', '["ENTP"]', '["ACTIVE", "SMART"]',
     120, 2, FALSE, NOW(), NOW()),
    (UUID_TO_BIN(UUID()), 'user3@kookmin.ac.kr', '$2a$10$1jNkCCorydAd9TyBXBUnbuNUB9RQR4uP.w4diACrmS/Ialn9A3uAW',
     '서연', '박서연', '국민대학교', '시각디자인학과', '20210103', '2002-11-08', 'FEMALE',
     '["DRAWING", "PHOTOGRAPHY", "MUSIC"]', '["ART", "DESIGN"]', '["ISFP"]', '["ARTISTIC", "QUIET"]',
     45, 1, FALSE, NOW(), NOW()),
    (UUID_TO_BIN(UUID()), 'user4@kookmin.ac.kr', '$2a$10$1jNkCCorydAd9TyBXBUnbuNUB9RQR4uP.w4diACrmS/Ialn9A3uAW',
     '준혁', '최준혁', '국민대학교', '전자공학부', '20190087', '2000-01-30', 'MALE',
     '["FITNESS", "RUNNING", "GAMING"]', '["TECHNOLOGY", "ROBOTICS"]', '["ISTJ"]', '["RESPONSIBLE", "HONEST"]',
     200, 3, FALSE, NOW(), NOW()),
    (UUID_TO_BIN(UUID()), 'user5@kookmin.ac.kr', '$2a$10$1jNkCCorydAd9TyBXBUnbuNUB9RQR4uP.w4diACrmS/Ialn9A3uAW',
     '유진', '정유진', '국민대학교', '영어영문학과', '20220015', '2003-05-19', 'FEMALE',
     '["MOVIE", "TRAVEL", "WRITING"]', '["LITERATURE", "LANGUAGE"]', '["ENFP"]', '["FUNNY", "TRAVELER"]',
     80, 1, FALSE, NOW(), NOW()),
    (UUID_TO_BIN(UUID()), 'user6@kookmin.ac.kr', '$2a$10$1jNkCCorydAd9TyBXBUnbuNUB9RQR4uP.w4diACrmS/Ialn9A3uAW',
     '도윤', '강도윤', '국민대학교', '소프트웨어학부', '20200156', '2001-09-03', 'MALE',
     '["GAMING", "BOARD_GAME", "READING"]', '["TECHNOLOGY", "AI_ML"]', '["INTP"]', '["SMART", "QUIET"]',
     310, 4, FALSE, NOW(), NOW()),
    (UUID_TO_BIN(UUID()), 'user7@kookmin.ac.kr', '$2a$10$1jNkCCorydAd9TyBXBUnbuNUB9RQR4uP.w4diACrmS/Ialn9A3uAW',
     '수빈', '한수빈', '국민대학교', '건축학부', '20210078', '2002-12-25', 'FEMALE',
     '["PHOTOGRAPHY", "CRAFTING", "HIKING"]', '["ARCHITECTURE", "ENVIRONMENT"]', '["INFJ"]', '["KIND", "GOOD_LISTENER"]',
     150, 2, FALSE, NOW(), NOW()),
    (UUID_TO_BIN(UUID()), 'user8@kookmin.ac.kr', '$2a$10$1jNkCCorydAd9TyBXBUnbuNUB9RQR4uP.w4diACrmS/Ialn9A3uAW',
     '재원', '오재원', '국민대학교', '경제학과', '20200201', '2001-04-11', 'MALE',
     '["FITNESS", "COOKING", "MUSIC"]', '["ECONOMICS", "SPORTS"]', '["ESTP"]', '["ACTIVE", "HONEST"]',
     95, 1, FALSE, NOW(), NOW()),
    (UUID_TO_BIN(UUID()), 'user9@kookmin.ac.kr', '$2a$10$1jNkCCorydAd9TyBXBUnbuNUB9RQR4uP.w4diACrmS/Ialn9A3uAW',
     '지우', '윤지우', '국민대학교', '미디어학부', '20220044', '2003-08-07', 'FEMALE',
     '["CAFE_HOPPING", "MOVIE", "FASHION"]', '["SOCIAL_MEDIA", "FILM"]', '["ESFJ"]', '["POSITIVE", "FASHIONABLE"]',
     60, 1, FALSE, NOW(), NOW()),
    (UUID_TO_BIN(UUID()), 'user10@kookmin.ac.kr', '$2a$10$1jNkCCorydAd9TyBXBUnbuNUB9RQR4uP.w4diACrmS/Ialn9A3uAW',
     '시우', '임시우', '국민대학교', '소프트웨어학부', '20210199', '2002-06-14', 'MALE',
     '["HIKING", "CAMPING", "GAMING"]', '["TECHNOLOGY", "STARTUP"]', '["ENTJ"]', '["AMBITIOUS", "CONFIDENT"]',
     175, 2, FALSE, NOW(), NOW());

-- 슬롯 (slot) - 유저당 2개 미만으로 생성
INSERT INTO slot (id, user_id, priority, current_match_id, is_quick_match, status, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), u.id, 'HOBBY', NULL, FALSE, 'EMPTY', NOW(), NOW()
FROM `user` u
WHERE (SELECT COUNT(*) FROM slot s WHERE s.user_id = u.id) < 2;

-- 시간표 (schedule) - 유저당 2~3개 수업
-- user1: 수치해석(화/목), 머신러닝기초(화/목)
INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '수치해석', 'TUESDAY', '13:30:00', '15:00:00', '미래관2층31호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 2,
       (SELECT id FROM `user` WHERE email = 'user1@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '수치해석' AND day_of_week = 'TUESDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user1@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '수치해석', 'THURSDAY', '13:30:00', '15:00:00', '미래관2층31호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 2,
       (SELECT id FROM `user` WHERE email = 'user1@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '수치해석' AND day_of_week = 'THURSDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user1@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '머신러닝기초', 'TUESDAY', '12:00:00', '13:30:00', '미래관4층45호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 4,
       (SELECT id FROM `user` WHERE email = 'user1@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '머신러닝기초' AND day_of_week = 'TUESDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user1@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '머신러닝기초', 'THURSDAY', '12:00:00', '13:30:00', '미래관4층45호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 4,
       (SELECT id FROM `user` WHERE email = 'user1@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '머신러닝기초' AND day_of_week = 'THURSDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user1@kookmin.ac.kr'));

-- user2: 경영학원론(월/수), 마케팅전략(화)
INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '경영학원론', 'MONDAY', '10:30:00', '12:00:00', '북악관3층15호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 3,
       (SELECT id FROM `user` WHERE email = 'user2@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '경영학원론' AND day_of_week = 'MONDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user2@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '경영학원론', 'WEDNESDAY', '10:30:00', '12:00:00', '북악관3층15호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 3,
       (SELECT id FROM `user` WHERE email = 'user2@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '경영학원론' AND day_of_week = 'WEDNESDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user2@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '마케팅전략', 'TUESDAY', '15:00:00', '16:30:00', '북악관2층11호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 2,
       (SELECT id FROM `user` WHERE email = 'user2@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '마케팅전략' AND day_of_week = 'TUESDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user2@kookmin.ac.kr'));

-- user3: 시각디자인론(월/수), 타이포그래피(금)
INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '시각디자인론', 'MONDAY', '13:30:00', '15:00:00', '미래관3층33호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 3,
       (SELECT id FROM `user` WHERE email = 'user3@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '시각디자인론' AND day_of_week = 'MONDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user3@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '시각디자인론', 'WEDNESDAY', '13:30:00', '15:00:00', '미래관3층33호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 3,
       (SELECT id FROM `user` WHERE email = 'user3@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '시각디자인론' AND day_of_week = 'WEDNESDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user3@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '타이포그래피', 'FRIDAY', '10:30:00', '12:00:00', '미래관4층42호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 4,
       (SELECT id FROM `user` WHERE email = 'user3@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '타이포그래피' AND day_of_week = 'FRIDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user3@kookmin.ac.kr'));

-- user4: 회로이론(화/목), 임베디드시스템(월)
INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '회로이론', 'TUESDAY', '09:00:00', '10:30:00', '미래관2층21호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 2,
       (SELECT id FROM `user` WHERE email = 'user4@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '회로이론' AND day_of_week = 'TUESDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user4@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '회로이론', 'THURSDAY', '09:00:00', '10:30:00', '미래관2층21호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 2,
       (SELECT id FROM `user` WHERE email = 'user4@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '회로이론' AND day_of_week = 'THURSDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user4@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '임베디드시스템', 'MONDAY', '15:00:00', '16:30:00', '미래관3층35호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 3,
       (SELECT id FROM `user` WHERE email = 'user4@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '임베디드시스템' AND day_of_week = 'MONDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user4@kookmin.ac.kr'));

-- user5: 영미문학개론(월/수), 영어회화(화/목)
INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '영미문학개론', 'MONDAY', '12:00:00', '13:30:00', '북악관4층22호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 4,
       (SELECT id FROM `user` WHERE email = 'user5@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '영미문학개론' AND day_of_week = 'MONDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user5@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '영미문학개론', 'WEDNESDAY', '12:00:00', '13:30:00', '북악관4층22호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 4,
       (SELECT id FROM `user` WHERE email = 'user5@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '영미문학개론' AND day_of_week = 'WEDNESDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user5@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '영어회화', 'TUESDAY', '10:30:00', '12:00:00', '북악관2층13호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 2,
       (SELECT id FROM `user` WHERE email = 'user5@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '영어회화' AND day_of_week = 'TUESDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user5@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '영어회화', 'THURSDAY', '10:30:00', '12:00:00', '북악관2층13호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 2,
       (SELECT id FROM `user` WHERE email = 'user5@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '영어회화' AND day_of_week = 'THURSDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user5@kookmin.ac.kr'));

-- user6: 알고리즘(월/수), 운영체제(화/목)
INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '알고리즘', 'MONDAY', '09:00:00', '10:30:00', '미래관4층45호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 4,
       (SELECT id FROM `user` WHERE email = 'user6@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '알고리즘' AND day_of_week = 'MONDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user6@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '알고리즘', 'WEDNESDAY', '09:00:00', '10:30:00', '미래관4층45호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 4,
       (SELECT id FROM `user` WHERE email = 'user6@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '알고리즘' AND day_of_week = 'WEDNESDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user6@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '운영체제', 'TUESDAY', '10:30:00', '12:00:00', '미래관2층32호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 2,
       (SELECT id FROM `user` WHERE email = 'user6@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '운영체제' AND day_of_week = 'TUESDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user6@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '운영체제', 'THURSDAY', '10:30:00', '12:00:00', '미래관2층32호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 2,
       (SELECT id FROM `user` WHERE email = 'user6@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '운영체제' AND day_of_week = 'THURSDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user6@kookmin.ac.kr'));

-- user7: 건축설계(월/수/금)
INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '건축설계', 'MONDAY', '13:30:00', '16:30:00', '북악관5층31호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 5,
       (SELECT id FROM `user` WHERE email = 'user7@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '건축설계' AND day_of_week = 'MONDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user7@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '건축설계', 'WEDNESDAY', '13:30:00', '16:30:00', '북악관5층31호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 5,
       (SELECT id FROM `user` WHERE email = 'user7@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '건축설계' AND day_of_week = 'WEDNESDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user7@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '건축설계', 'FRIDAY', '13:30:00', '16:30:00', '북악관5층31호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 5,
       (SELECT id FROM `user` WHERE email = 'user7@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '건축설계' AND day_of_week = 'FRIDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user7@kookmin.ac.kr'));

-- user8: 거시경제학(화/목), 통계학(월/수)
INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '거시경제학', 'TUESDAY', '09:00:00', '10:30:00', '북악관3층17호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 3,
       (SELECT id FROM `user` WHERE email = 'user8@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '거시경제학' AND day_of_week = 'TUESDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user8@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '거시경제학', 'THURSDAY', '09:00:00', '10:30:00', '북악관3층17호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 3,
       (SELECT id FROM `user` WHERE email = 'user8@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '거시경제학' AND day_of_week = 'THURSDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user8@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '통계학', 'MONDAY', '12:00:00', '13:30:00', '북악관2층12호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 2,
       (SELECT id FROM `user` WHERE email = 'user8@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '통계학' AND day_of_week = 'MONDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user8@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '통계학', 'WEDNESDAY', '12:00:00', '13:30:00', '북악관2층12호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 2,
       (SELECT id FROM `user` WHERE email = 'user8@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '통계학' AND day_of_week = 'WEDNESDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user8@kookmin.ac.kr'));

-- user9: 미디어콘텐츠제작(화/목), 영상편집(금)
INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '미디어콘텐츠제작', 'TUESDAY', '13:30:00', '15:00:00', '북악관4층25호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 4,
       (SELECT id FROM `user` WHERE email = 'user9@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '미디어콘텐츠제작' AND day_of_week = 'TUESDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user9@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '미디어콘텐츠제작', 'THURSDAY', '13:30:00', '15:00:00', '북악관4층25호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 4,
       (SELECT id FROM `user` WHERE email = 'user9@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '미디어콘텐츠제작' AND day_of_week = 'THURSDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user9@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '영상편집', 'FRIDAY', '09:00:00', '12:00:00', '북악관4층26호실',
       (SELECT id FROM campus_building WHERE name = '북악관' LIMIT 1), 4,
       (SELECT id FROM `user` WHERE email = 'user9@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '영상편집' AND day_of_week = 'FRIDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user9@kookmin.ac.kr'));

-- user10: 비주얼컴퓨팅최신기술(월/수), 캡스톤디자인(금)
INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '비주얼컴퓨팅최신기술', 'MONDAY', '15:00:00', '16:30:00', '미래관2층32호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 2,
       (SELECT id FROM `user` WHERE email = 'user10@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '비주얼컴퓨팅최신기술' AND day_of_week = 'MONDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user10@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '비주얼컴퓨팅최신기술', 'WEDNESDAY', '15:00:00', '16:30:00', '미래관2층32호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 2,
       (SELECT id FROM `user` WHERE email = 'user10@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '비주얼컴퓨팅최신기술' AND day_of_week = 'WEDNESDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user10@kookmin.ac.kr'));

INSERT INTO schedule (id, name, day_of_week, started_at, ended_at, place, campus_building_id, floor, user_id, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), '캡스톤디자인', 'FRIDAY', '12:00:00', '15:00:00', '미래관4층24호실',
       (SELECT id FROM campus_building WHERE name = '미래관' LIMIT 1), 4,
       (SELECT id FROM `user` WHERE email = 'user10@kookmin.ac.kr'), NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE name = '캡스톤디자인' AND day_of_week = 'FRIDAY' AND user_id = (SELECT id FROM `user` WHERE email = 'user10@kookmin.ac.kr'));
