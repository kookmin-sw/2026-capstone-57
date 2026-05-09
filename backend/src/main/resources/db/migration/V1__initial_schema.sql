-- =============================================
-- V1: Initial Schema
-- =============================================

-- -----------------------------------------------
-- USER
-- -----------------------------------------------
CREATE TABLE `USER` (
    id BINARY(16) NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    name VARCHAR(100),
    university VARCHAR(255) NOT NULL,
    major VARCHAR(255),
    student_id VARCHAR(50),
    birth_date DATE,
    gender VARCHAR(10),
    hobbies JSON,
    interests JSON,
    personality_type JSON,
    ideal_type_preferences JSON,
    total_exp INT NOT NULL DEFAULT 0,
    current_level INT NOT NULL DEFAULT 1,
    is_suspended BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- CAMPUS_BUILDING
-- -----------------------------------------------
CREATE TABLE CAMPUS_BUILDING (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- CAMPUS_BUILDING_PLACE
-- -----------------------------------------------
CREATE TABLE CAMPUS_BUILDING_PLACE (
    id BINARY(16) NOT NULL PRIMARY KEY,
    building_id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    floor INT NOT NULL,
    type VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_place_building FOREIGN KEY (building_id) REFERENCES CAMPUS_BUILDING(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- CAMPUS_VENUE
-- -----------------------------------------------
CREATE TABLE CAMPUS_VENUE (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- CAMPUS_PATH
-- -----------------------------------------------
CREATE TABLE CAMPUS_PATH (
    id BINARY(16) NOT NULL PRIMARY KEY,
    from_building_id BINARY(16) NOT NULL,
    to_building_id BINARY(16) NOT NULL,
    venue_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_path_from_building FOREIGN KEY (from_building_id) REFERENCES CAMPUS_BUILDING(id),
    CONSTRAINT fk_path_to_building FOREIGN KEY (to_building_id) REFERENCES CAMPUS_BUILDING(id),
    CONSTRAINT fk_path_venue FOREIGN KEY (venue_id) REFERENCES CAMPUS_VENUE(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- SCHEDULE
-- -----------------------------------------------
CREATE TABLE `SCHEDULE` (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    started_at TIME NOT NULL,
    ended_at TIME NOT NULL,
    place VARCHAR(255) NOT NULL,
    campus_building_id BINARY(16),
    floor INT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_schedule_user FOREIGN KEY (user_id) REFERENCES `USER`(id),
    CONSTRAINT fk_schedule_building FOREIGN KEY (campus_building_id) REFERENCES CAMPUS_BUILDING(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- SLOT
-- -----------------------------------------------
CREATE TABLE SLOT (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'HOBBY',
    current_match_id BINARY(16),
    is_quick_match BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'EMPTY',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_slot_user FOREIGN KEY (user_id) REFERENCES `USER`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- MATCH
-- -----------------------------------------------
CREATE TABLE `MATCH` (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_a_id BINARY(16) NOT NULL,
    user_b_id BINARY(16) NOT NULL,
    slot_a_id BINARY(16) NOT NULL,
    slot_b_id BINARY(16) NOT NULL,
    is_quick_match BOOLEAN NOT NULL DEFAULT FALSE,
    cycle_start_date DATE NOT NULL,
    cycle_end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_match_user_a FOREIGN KEY (user_a_id) REFERENCES `USER`(id),
    CONSTRAINT fk_match_user_b FOREIGN KEY (user_b_id) REFERENCES `USER`(id),
    CONSTRAINT fk_match_slot_a FOREIGN KEY (slot_a_id) REFERENCES SLOT(id),
    CONSTRAINT fk_match_slot_b FOREIGN KEY (slot_b_id) REFERENCES SLOT(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- SLOT의 current_match_id FK (MATCH 테이블 생성 후 추가)
ALTER TABLE SLOT
    ADD CONSTRAINT fk_slot_current_match FOREIGN KEY (current_match_id) REFERENCES `MATCH`(id);

-- -----------------------------------------------
-- MISSION
-- -----------------------------------------------
CREATE TABLE MISSION (
    id BINARY(16) NOT NULL PRIMARY KEY,
    match_id BINARY(16) NOT NULL,
    location VARCHAR(255) NOT NULL,
    activity VARCHAR(255) NOT NULL,
    description TEXT,
    deadline TIMESTAMP NOT NULL,
    confirmed_by JSON,
    extended BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_mission_match FOREIGN KEY (match_id) REFERENCES `MATCH`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- INTERACTION
-- -----------------------------------------------
CREATE TABLE INTERACTION (
    id BINARY(16) NOT NULL PRIMARY KEY,
    match_id BINARY(16) NOT NULL,
    current_stage INT NOT NULL DEFAULT 1,
    stage_status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    quiz_completed_by JSON,
    quiz_data JSON,
    chat_start_time TIMESTAMP NULL,
    chat_end_time TIMESTAMP NULL,
    game_type VARCHAR(100),
    game_completed BOOLEAN NOT NULL DEFAULT FALSE,
    mission_id BINARY(16),
    mission_confirmed_by JSON,
    mission_extended BOOLEAN NOT NULL DEFAULT FALSE,
    review_completed_by JSON,
    termination_reason VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_interaction_match FOREIGN KEY (match_id) REFERENCES `MATCH`(id),
    CONSTRAINT fk_interaction_mission FOREIGN KEY (mission_id) REFERENCES MISSION(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- BLOCK
-- -----------------------------------------------
CREATE TABLE `BLOCK` (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    blocked_user_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_block_user FOREIGN KEY (user_id) REFERENCES `USER`(id),
    CONSTRAINT fk_block_blocked_user FOREIGN KEY (blocked_user_id) REFERENCES `USER`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- REPORT
-- -----------------------------------------------
CREATE TABLE REPORT (
    id BINARY(16) NOT NULL PRIMARY KEY,
    reporter_id BINARY(16) NOT NULL,
    target_id BINARY(16) NOT NULL,
    match_id BINARY(16) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    details TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES `USER`(id),
    CONSTRAINT fk_report_target FOREIGN KEY (target_id) REFERENCES `USER`(id),
    CONSTRAINT fk_report_match FOREIGN KEY (match_id) REFERENCES `MATCH`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- REVIEW_SESSION
-- -----------------------------------------------
CREATE TABLE REVIEW_SESSION (
    id BINARY(16) NOT NULL PRIMARY KEY,
    interaction_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    mode VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_review_session_interaction FOREIGN KEY (interaction_id) REFERENCES INTERACTION(id),
    CONSTRAINT fk_review_session_user FOREIGN KEY (user_id) REFERENCES `USER`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- REVIEW
-- -----------------------------------------------
CREATE TABLE REVIEW (
    id BINARY(16) NOT NULL PRIMARY KEY,
    interaction_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    mode VARCHAR(20) NOT NULL,
    satisfaction INT NOT NULL,
    reflection TEXT NOT NULL,
    want_to_meet_again BOOLEAN NOT NULL,
    ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_review_interaction FOREIGN KEY (interaction_id) REFERENCES INTERACTION(id),
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES `USER`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- PLAN_ENTRY
-- -----------------------------------------------
CREATE TABLE PLAN_ENTRY (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    day_of_week INT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    location VARCHAR(255),
    name VARCHAR(255),
    type VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_plan_entry_user FOREIGN KEY (user_id) REFERENCES `USER`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- NOTIFICATION_SETTING
-- -----------------------------------------------
CREATE TABLE NOTIFICATION_SETTING (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    match_notification BOOLEAN NOT NULL DEFAULT TRUE,
    stage_notification BOOLEAN NOT NULL DEFAULT TRUE,
    mission_reminder BOOLEAN NOT NULL DEFAULT TRUE,
    planner_reminder BOOLEAN NOT NULL DEFAULT TRUE,
    level_up_notification BOOLEAN NOT NULL DEFAULT TRUE,
    hint_question_notification BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_notification_setting_user FOREIGN KEY (user_id) REFERENCES `USER`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- EXP_HISTORY
-- -----------------------------------------------
CREATE TABLE EXP_HISTORY (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    activity VARCHAR(50) NOT NULL,
    amount INT NOT NULL,
    bonus_amount INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_exp_history_user FOREIGN KEY (user_id) REFERENCES `USER`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- DIARY_ENTRY
-- -----------------------------------------------
CREATE TABLE DIARY_ENTRY (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    entry_date DATE NOT NULL,
    content TEXT NOT NULL,
    emotion_tag VARCHAR(20),
    streak_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_diary_entry_user FOREIGN KEY (user_id) REFERENCES `USER`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- CHAT_SESSION
-- -----------------------------------------------
CREATE TABLE CHAT_SESSION (
    id BINARY(16) NOT NULL PRIMARY KEY,
    match_id BINARY(16) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_chat_session_match FOREIGN KEY (match_id) REFERENCES `MATCH`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- CHAT_MESSAGE
-- -----------------------------------------------
CREATE TABLE CHAT_MESSAGE (
    id BINARY(16) NOT NULL PRIMARY KEY,
    session_id BINARY(16) NOT NULL,
    sender_id BINARY(16) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES CHAT_SESSION(id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES `USER`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------
-- GAME_SESSION
-- -----------------------------------------------
CREATE TABLE GAME_SESSION (
    id BINARY(16) NOT NULL PRIMARY KEY,
    match_id BINARY(16) NOT NULL,
    game_type VARCHAR(100) NOT NULL,
    state JSON,
    intimacy_points INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    CONSTRAINT fk_game_session_match FOREIGN KEY (match_id) REFERENCES `MATCH`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
