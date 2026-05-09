CREATE TABLE `SCHEDULE` (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    started_at TIME NOT NULL,
    ended_at TIME NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_schedule_user_id (user_id),
    CONSTRAINT fk_schedule_user FOREIGN KEY (user_id) REFERENCES `USER`(id)
);
