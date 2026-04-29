-- =============================================
-- V1: 일기예보 (ilgi-yebo) 전체 스키마 생성
-- MySQL 문법, UUID는 BINARY(16), 배열은 JSON
-- =============================================

-- 1. USER
CREATE TABLE `USER` (
    id BINARY(16) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    university VARCHAR(255) NOT NULL,
    hobbies JSON,
    interests JSON,
    personality_type VARCHAR(50),
    ideal_type_preferences JSON,
    total_exp INT NOT NULL DEFAULT 0,
    current_level INT NOT NULL DEFAULT 1,
    is_suspended BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. SLOT
CREATE TABLE SLOT (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    hobbies JSON,
    interests JSON,
    ideal_type JSON,
    current_match_id BINARY(16) NULL,
    is_quick_match BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('empty','active','completed') NOT NULL DEFAULT 'empty',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_slot_user_status (user_id, status),
    CONSTRAINT fk_slot_user FOREIGN KEY (user_id) REFERENCES `USER`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. MATCH
CREATE TABLE `MATCH` (
    id BINARY(16) NOT NULL,
    user_a_id BINARY(16) NOT NULL,
    user_b_id BINARY(16) NOT NULL,
    slot_a_id BINARY(16) NOT NULL,
    slot_b_id BINARY(16) NOT NULL,
    is_quick_match BOOLEAN NOT NULL DEFAULT FALSE,
    cycle_start_date DATE NOT NULL,
    cycle_end_date DATE NOT NULL,
    status ENUM('active','completed','terminated') NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_match_status_cycle_end (status, cycle_end_date),
    INDEX idx_match_user_a_status (user_a_id, status),
    INDEX idx_match_user_b_status (user_b_id, status),
    CONSTRAINT fk_match_user_a FOREIGN KEY (user_a_id) REFERENCES `USER`(id),
    CONSTRAINT fk_match_user_b FOREIGN KEY (user_b_id) REFERENCES `USER`(id),
    CONSTRAINT fk_match_slot_a FOREIGN KEY (slot_a_id) REFERENCES SLOT(id),
    CONSTRAINT fk_match_slot_b FOREIGN KEY (slot_b_id) REFERENCES SLOT(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE SLOT ADD CONSTRAINT fk_slot_current_match FOREIGN KEY (current_match_id) REFERENCES `MATCH`(id) ON DELETE SET NULL;

-- 4. INTERACTION
CREATE TABLE INTERACTION (
    id BINARY(16) NOT NULL,
    match_id BINARY(16) NOT NULL,
    current_stage INT NOT NULL DEFAULT 1,
    stage_status ENUM('in_progress','waiting','completed','terminated') NOT NULL DEFAULT 'in_progress',
    quiz_completed_by JSON,
    chat_start_time TIMESTAMP NULL,
    chat_end_time TIMESTAMP NULL,
    game_type VARCHAR(100) NULL,
    game_completed BOOLEAN NOT NULL DEFAULT FALSE,
    mission_id BINARY(16) NULL,
    mission_confirmed_by JSON,
    mission_extended BOOLEAN NOT NULL DEFAULT FALSE,
    review_completed_by JSON,
    termination_reason VARCHAR(255) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_interaction_match FOREIGN KEY (match_id) REFERENCES `MATCH`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. DIARY_ENTRY
CREATE TABLE DIARY_ENTRY (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    entry_date DATE NOT NULL,
    content TEXT NOT NULL,
    emotion_tag ENUM('happy','sad','angry','anxious','calm','excited','tired') NULL,
    streak_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_diary_user_date (user_id, entry_date),
    CONSTRAINT fk_diary_user FOREIGN KEY (user_id) REFERENCES `USER`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. PLAN_ENTRY
CREATE TABLE PLAN_ENTRY (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    entry_date DATE NOT NULL,
    entries JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_plan_user_date (user_id, entry_date),
    CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES `USER`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. TIMETABLE_ENTRY
CREATE TABLE TIMETABLE_ENTRY (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    day_of_week INT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    location VARCHAR(255) NULL,
    course_name VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_timetable_user FOREIGN KEY (user_id) REFERENCES `USER`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. CHAT_SESSION
CREATE TABLE CHAT_SESSION (
    id BINARY(16) NOT NULL,
    match_id BINARY(16) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status ENUM('active','ended') NOT NULL DEFAULT 'active',
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_session_match FOREIGN KEY (match_id) REFERENCES `MATCH`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. CHAT_MESSAGE
CREATE TABLE CHAT_MESSAGE (
    id BINARY(16) NOT NULL,
    session_id BINARY(16) NOT NULL,
    sender_id BINARY(16) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES CHAT_SESSION(id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES `USER`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. GAME_SESSION
CREATE TABLE GAME_SESSION (
    id BINARY(16) NOT NULL,
    match_id BINARY(16) NOT NULL,
    game_type VARCHAR(100) NOT NULL,
    state JSON,
    intimacy_points INT NOT NULL DEFAULT 0,
    status ENUM('waiting','in_progress','completed') NOT NULL DEFAULT 'waiting',
    PRIMARY KEY (id),
    CONSTRAINT fk_game_session_match FOREIGN KEY (match_id) REFERENCES `MATCH`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. MISSION
CREATE TABLE MISSION (
    id BINARY(16) NOT NULL,
    match_id BINARY(16) NOT NULL,
    location VARCHAR(255) NOT NULL,
    activity VARCHAR(255) NOT NULL,
    description TEXT,
    deadline TIMESTAMP NOT NULL,
    confirmed_by JSON,
    extended BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('pending','confirmed','expired') NOT NULL DEFAULT 'pending',
    PRIMARY KEY (id),
    CONSTRAINT fk_mission_match FOREIGN KEY (match_id) REFERENCES `MATCH`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE INTERACTION ADD CONSTRAINT fk_interaction_mission FOREIGN KEY (mission_id) REFERENCES MISSION(id) ON DELETE SET NULL;

-- 12. REVIEW
CREATE TABLE REVIEW (
    id BINARY(16) NOT NULL,
    interaction_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    mode ENUM('ai_assisted','direct') NOT NULL,
    satisfaction INT NOT NULL,
    reflection TEXT NOT NULL,
    want_to_meet_again BOOLEAN NOT NULL,
    ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_review_interaction FOREIGN KEY (interaction_id) REFERENCES INTERACTION(id),
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES `USER`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. REVIEW_SESSION
CREATE TABLE REVIEW_SESSION (
    id BINARY(16) NOT NULL,
    interaction_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    mode ENUM('ai_assisted','direct') NOT NULL,
    status ENUM('in_progress','generated','completed') NOT NULL DEFAULT 'in_progress',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_session_interaction_user (interaction_id, user_id),
    CONSTRAINT fk_review_session_interaction FOREIGN KEY (interaction_id) REFERENCES INTERACTION(id),
    CONSTRAINT fk_review_session_user FOREIGN KEY (user_id) REFERENCES `USER`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. AI_REVIEW_QUESTION
CREATE TABLE AI_REVIEW_QUESTION (
    id BINARY(16) NOT NULL,
    session_id BINARY(16) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NULL,
    question_order INT NOT NULL,
    answered_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    INDEX idx_ai_review_question_session_order (session_id, question_order),
    CONSTRAINT fk_ai_review_question_session FOREIGN KEY (session_id) REFERENCES REVIEW_SESSION(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. HINT_QUESTION
CREATE TABLE HINT_QUESTION (
    id BINARY(16) NOT NULL,
    match_id BINARY(16) NOT NULL,
    sender_id BINARY(16) NOT NULL,
    responder_id BINARY(16) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NULL,
    status ENUM('pending','answered') NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    answered_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    INDEX idx_hint_question_match_status (match_id, status),
    INDEX idx_hint_question_responder_status (responder_id, status),
    CONSTRAINT fk_hint_question_match FOREIGN KEY (match_id) REFERENCES `MATCH`(id),
    CONSTRAINT fk_hint_question_sender FOREIGN KEY (sender_id) REFERENCES `USER`(id),
    CONSTRAINT fk_hint_question_responder FOREIGN KEY (responder_id) REFERENCES `USER`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. EXP_HISTORY
CREATE TABLE EXP_HISTORY (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    activity ENUM('diary_write','diary_streak_bonus','planner_write','quiz_complete','chat_participate','game_complete','mission_complete','review_write') NOT NULL,
    amount INT NOT NULL,
    bonus_amount INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_exp_history_user_created (user_id, created_at),
    CONSTRAINT fk_exp_history_user FOREIGN KEY (user_id) REFERENCES `USER`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. REPORT
CREATE TABLE REPORT (
    id BINARY(16) NOT NULL,
    reporter_id BINARY(16) NOT NULL,
    target_id BINARY(16) NOT NULL,
    match_id BINARY(16) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    details TEXT,
    status ENUM('pending','reviewed','resolved') NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_report_target_status (target_id, status),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES `USER`(id),
    CONSTRAINT fk_report_target FOREIGN KEY (target_id) REFERENCES `USER`(id),
    CONSTRAINT fk_report_match FOREIGN KEY (match_id) REFERENCES `MATCH`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. BLOCK
CREATE TABLE `BLOCK` (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    blocked_user_id BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_block_user_blocked (user_id, blocked_user_id),
    CONSTRAINT fk_block_user FOREIGN KEY (user_id) REFERENCES `USER`(id) ON DELETE CASCADE,
    CONSTRAINT fk_block_blocked_user FOREIGN KEY (blocked_user_id) REFERENCES `USER`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. NOTIFICATION_SETTING
CREATE TABLE NOTIFICATION_SETTING (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    match_notification BOOLEAN NOT NULL DEFAULT TRUE,
    stage_notification BOOLEAN NOT NULL DEFAULT TRUE,
    mission_reminder BOOLEAN NOT NULL DEFAULT TRUE,
    planner_reminder BOOLEAN NOT NULL DEFAULT TRUE,
    level_up_notification BOOLEAN NOT NULL DEFAULT TRUE,
    hint_question_notification BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_setting_user FOREIGN KEY (user_id) REFERENCES `USER`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 20. CAMPUS_BUILDING
CREATE TABLE CAMPUS_BUILDING (
    id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    purpose ENUM('lecture','restaurant','cafe','library','gym','admin','dormitory','other') NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_campus_building_purpose (purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 21. CAMPUS_PATH
CREATE TABLE CAMPUS_PATH (
    id BINARY(16) NOT NULL,
    from_building_id BINARY(16) NOT NULL,
    to_building_id BINARY(16) NOT NULL,
    walking_time_minutes INT NOT NULL,
    passing_venue_ids JSON,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_campus_path_from_to (from_building_id, to_building_id),
    CONSTRAINT fk_campus_path_from FOREIGN KEY (from_building_id) REFERENCES CAMPUS_BUILDING(id),
    CONSTRAINT fk_campus_path_to FOREIGN KEY (to_building_id) REFERENCES CAMPUS_BUILDING(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 22. CAMPUS_VENUE
CREATE TABLE CAMPUS_VENUE (
    id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    building_id BINARY(16) NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    type ENUM('cafe','convenience_store','bench','plaza','park','food_court','study_room','other') NOT NULL,
    meeting_suitability INT NOT NULL DEFAULT 3,
    operating_hours JSON,
    characteristics JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_campus_venue_type_suitability (type, meeting_suitability),
    INDEX idx_campus_venue_building (building_id),
    CONSTRAINT fk_campus_venue_building FOREIGN KEY (building_id) REFERENCES CAMPUS_BUILDING(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
