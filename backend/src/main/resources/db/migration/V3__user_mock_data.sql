-- =============================================
-- V3: User Mock Data (10명)
-- =============================================

INSERT INTO `USER` (id, email, password_hash, nickname, name, university, major, student_id, birth_date, gender, hobbies, interests, personality_type, ideal_type_preferences, total_exp, current_level, is_suspended, created_at, updated_at) VALUES
    (UUID_TO_BIN(UUID()), 'user1@kookmin.ac.kr', '$2a$10$dummyhashvalue1234567890abcdefghijklmnopqrstuv', '하늘이', '김하늘', '국민대학교', '소프트웨어학부', '20210001', '2002-03-15', 'FEMALE',
     '["READING", "YOGA", "CAFE_HOPPING"]', '["AI_ML", "DESIGN"]', '["INFP"]', '["KIND", "FUNNY"]',
     0, 1, FALSE, NOW(), NOW()),

    (UUID_TO_BIN(UUID()), 'user2@kookmin.ac.kr', '$2a$10$dummyhashvalue1234567890abcdefghijklmnopqrstuv', '민수', '이민수', '국민대학교', '경영학부', '20200042', '2001-07-22', 'MALE',
     '["FITNESS", "GAMING", "CAFE_HOPPING"]', '["STARTUP", "ECONOMICS"]', '["ENTP"]', '["ACTIVE", "SMART"]',
     120, 2, FALSE, NOW(), NOW()),

    (UUID_TO_BIN(UUID()), 'user3@kookmin.ac.kr', '$2a$10$dummyhashvalue1234567890abcdefghijklmnopqrstuv', '서연', '박서연', '국민대학교', '시각디자인학과', '20210103', '2002-11-08', 'FEMALE',
     '["DRAWING", "PHOTOGRAPHY", "MUSIC"]', '["ART", "DESIGN"]', '["ISFP"]', '["ARTISTIC", "QUIET"]',
     45, 1, FALSE, NOW(), NOW()),

    (UUID_TO_BIN(UUID()), 'user4@kookmin.ac.kr', '$2a$10$dummyhashvalue1234567890abcdefghijklmnopqrstuv', '준혁', '최준혁', '국민대학교', '전자공학부', '20190087', '2000-01-30', 'MALE',
     '["FITNESS", "RUNNING", "GAMING"]', '["TECHNOLOGY", "ROBOTICS"]', '["ISTJ"]', '["RESPONSIBLE", "HONEST"]',
     200, 3, FALSE, NOW(), NOW()),

    (UUID_TO_BIN(UUID()), 'user5@kookmin.ac.kr', '$2a$10$dummyhashvalue1234567890abcdefghijklmnopqrstuv', '유진', '정유진', '국민대학교', '영어영문학과', '20220015', '2003-05-19', 'FEMALE',
     '["MOVIE", "TRAVEL", "WRITING"]', '["LITERATURE", "LANGUAGE"]', '["ENFP"]', '["FUNNY", "TRAVELER"]',
     80, 1, FALSE, NOW(), NOW()),

    (UUID_TO_BIN(UUID()), 'user6@kookmin.ac.kr', '$2a$10$dummyhashvalue1234567890abcdefghijklmnopqrstuv', '도윤', '강도윤', '국민대학교', '소프트웨어학부', '20200156', '2001-09-03', 'MALE',
     '["GAMING", "BOARD_GAME", "READING"]', '["TECHNOLOGY", "AI_ML"]', '["INTP"]', '["SMART", "QUIET"]',
     310, 4, FALSE, NOW(), NOW()),

    (UUID_TO_BIN(UUID()), 'user7@kookmin.ac.kr', '$2a$10$dummyhashvalue1234567890abcdefghijklmnopqrstuv', '수빈', '한수빈', '국민대학교', '건축학부', '20210078', '2002-12-25', 'FEMALE',
     '["PHOTOGRAPHY", "CRAFTING", "HIKING"]', '["ARCHITECTURE", "ENVIRONMENT"]', '["INFJ"]', '["KIND", "GOOD_LISTENER"]',
     150, 2, FALSE, NOW(), NOW()),

    (UUID_TO_BIN(UUID()), 'user8@kookmin.ac.kr', '$2a$10$dummyhashvalue1234567890abcdefghijklmnopqrstuv', '재원', '오재원', '국민대학교', '경제학과', '20200201', '2001-04-11', 'MALE',
     '["FITNESS", "COOKING", "MUSIC"]', '["ECONOMICS", "SPORTS"]', '["ESTP"]', '["ACTIVE", "HONEST"]',
     95, 1, FALSE, NOW(), NOW()),

    (UUID_TO_BIN(UUID()), 'user9@kookmin.ac.kr', '$2a$10$dummyhashvalue1234567890abcdefghijklmnopqrstuv', '지우', '윤지우', '국민대학교', '미디어학부', '20220044', '2003-08-07', 'FEMALE',
     '["CAFE_HOPPING", "MOVIE", "FASHION"]', '["SOCIAL_MEDIA", "FILM"]', '["ESFJ"]', '["POSITIVE", "FASHIONABLE"]',
     60, 1, FALSE, NOW(), NOW()),

    (UUID_TO_BIN(UUID()), 'user10@kookmin.ac.kr', '$2a$10$dummyhashvalue1234567890abcdefghijklmnopqrstuv', '시우', '임시우', '국민대학교', '소프트웨어학부', '20210199', '2002-06-14', 'MALE',
     '["HIKING", "CAMPING", "GAMING"]', '["TECHNOLOGY", "STARTUP"]', '["ENTJ"]', '["AMBITIOUS", "CONFIDENT"]',
     175, 2, FALSE, NOW(), NOW());
