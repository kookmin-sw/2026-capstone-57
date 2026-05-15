INSERT INTO campus_building_place (
    id, building_id, name, floor, type_activity,
    description, operating_hours, created_at, updated_at
)
VALUES
(
    UUID_TO_BIN(UUID()),
    (SELECT id FROM campus_building WHERE name = '북악관'),
    '북악관 로비',
    1,
    '["LOUNGE","CONVENIENCE_STORE","RESTAURANT","CAFE"]',
    '북악관(N2동) 1층 로비, 간식과 음료 구매 가능',
    '월-금 08:00-22:00',
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (SELECT id FROM campus_building WHERE name = '복지관'),
    '복지관 학생식당',
    -1,
    '["RESTAURANT"]',
    '종합복지관(S1동) 지하1층 학생식당',
    '월-금 11:00-14:00, 17:00-19:00',
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (SELECT id FROM campus_building WHERE name = '복지관'),
    '복지관 카페',
    -1,
    '["CAFE"]',
    '종합복지관(S1동) 지하1층 카페',
    '월-금 08:00-17:00',
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (SELECT id FROM campus_building WHERE name = '복지관'),
    '복지관 K-BOB',
    -1,
    '["RESTAURANT"]',
    '종합복지관(S1동) 지하1층 분식/간편식 매장',
    '월-금 10:00-19:00',
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (SELECT id FROM campus_building WHERE name = '성곡도서관'),
    '해동 도서관 할리스 카페',
    -1,
    '["CAFE"]',
    '성곡도서관 지하 1층 할리스 카페 (해동도서관), 대화하기 좋은 공간',
    '월-금 08:00-21:00, 토 10:00-18:00',
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (SELECT id FROM campus_building WHERE name = '공학관'),
    '공학관 로비',
    1,
    '["LOUNGE","CONVENIENCE_STORE","CAFE","RESTAURANT"]',
    '공학관(W1동) 로비, 간식과 음료 구매 가능',
    '24시간',
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (SELECT id FROM campus_building WHERE name = '조형관'),
    '조형관 매점',
    1,
    '["CONVENIENCE_STORE","CAFE"]',
    '조형관(예술대학) 1층 매점, 간식과 음료 구매 가능',
    '월-금 09:00-18:00',
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (SELECT id FROM campus_building WHERE name = '미래관'),
    '미래관 자주스',
    4,
    '["STUDY_ROOM","REST_ROOM"]',
    '미래관(S2동) 4층 자율주행스튜디오, 학습과 휴식 공간',
    '월-금 09:00-21:00',
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (SELECT id FROM campus_building WHERE name = '미래관'),
    '미래관 무상실',
    4,
    '["STUDY_ROOM"]',
    '미래관(S2동) 4층 무한상상실',
    '월-금 09:00-21:00',
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (SELECT id FROM campus_building WHERE name = '복지관'),
    '복지관 3층 휴게공간',
    4,
    '["REST_ROOM"]',
    '복지관(S1동) 3층 휴게 공간',
    '월-금 09:00-22:00',
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (SELECT id FROM campus_building WHERE name = '경영관'),
    '경영관 콘서트홀',
    1,
    '["LOUNGE"]',
    '경영관 로비 (콘서트홀)',
    '월-금 09:00-22:00',
    NOW(),
    NOW()
);

INSERT INTO campus_venue (
    id, name, type_activity, description, operating_hours, created_at, updated_at
)
VALUES
(UUID_TO_BIN(UUID()), '북악관 왼쪽 입구', '["ENTRANCE"]', '북악관(N2동) 서쪽 입구', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '북악관 오른쪽 입구', '["ENTRANCE"]', '북악관(N2동) 동쪽 입구', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '북악관 앞 삼거리', '["INTERSECTION"]', '북악관(N2동) 정면에서 다른 건물로 내려가는 삼거리', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '용두리', '["OUTDOOR"]', '용두리, 캠퍼스 내 자연 속 휴식 공간, 산책하며 대화하기 좋은 곳', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '경상관-국제관 사이 계단', '["STAIRWAY"]', '북악관에서 운동장으로 내려가는 주요 계단', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '경영관 후문', '["ENTRANCE"]', '경영관에서 본부관으로 가는 입구', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '경영관 앞문', '["ENTRANCE"]', '경영관에서 조형관으로 가는 입구', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '북서쪽 운동장', '["OUTDOOR"]', '경상관 앞쪽 운동장, 벤치 있음, 가볍게 앉아 대화하기 좋은 공간', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '북동쪽 운동장', '["OUTDOOR"]', '경영관 앞쪽 운동장, 벤치 있음, 가볍게 앉아 대화하기 좋은 공간', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '남서쪽 운동장', '["OUTDOOR"]', '정문 쪽 운동장, 벤치 있음', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '남동쪽 운동장', '["OUTDOOR"]', '미래관 쪽 운동장, 벤치 있음', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '조형관 앞 계단', '["STAIRWAY"]', '조형관에서 경영관 올라가는 계단', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '미래관 앞문 (조형관방향)', '["ENTRANCE"]', '미래관 4층, 조형관쪽으로 가는 입구', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '미래관 뒷문 (복지관방향)', '["ENTRANCE"]', '미래관에서 복지관으로 가는 입구', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '복지관 동쪽 입구', '["ENTRANCE"]', '복지관에서 미래관으로 가는 입구', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '복지관 서쪽 입구', '["ENTRANCE"]', '복지관에서 정문으로 가는 입구', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '정문', '["ENTRANCE"]', '국민대학교 정문, 버스정류장 근처', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '공학관 뒷문', '["ENTRANCE"]', '정문에서 공학관으로 가는 테니스장 옆 길', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '공학관 가운데 입구', '["ENTRANCE"]', '공학관 가운데 입구', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '공학관 동쪽 입구', '["ENTRANCE"]', '공학관 오른쪽 입구, 편의점 근처', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '공학관 서쪽 입구', '["ENTRANCE"]', '공학관 왼쪽 입구, 도서관 앞', '24시간', NOW(), NOW()),
(UUID_TO_BIN(UUID()), '성곡도서관 입구', '["ENTRANCE"]', '성곡도서관 입구', '24시간', NOW(), NOW());

-- 미래관 -> 북악관 경로 생성

INSERT INTO campus_path (
    id, from_building_id, to_building_id, created_at, updated_at
)
VALUES
(
    UUID_TO_BIN(UUID()),
    (SELECT id FROM campus_building WHERE name = '미래관'),
    (SELECT id FROM campus_building WHERE name = '북악관'),
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (SELECT id FROM campus_building WHERE name = '미래관'),
    (SELECT id FROM campus_building WHERE name = '북악관'),
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (SELECT id FROM campus_building WHERE name = '미래관'),
    (SELECT id FROM campus_building WHERE name = '북악관'),
    NOW(),
    NOW()
);

-- 1번 경로
INSERT INTO campus_path_venue (
    id, path_id, venue_id, order_index, created_at, updated_at
)
VALUES
(
    UUID_TO_BIN(UUID()),
    (
        SELECT id
        FROM campus_path
        WHERE from_building_id = (SELECT id FROM campus_building WHERE name = '미래관')
          AND to_building_id = (SELECT id FROM campus_building WHERE name = '북악관')
        ORDER BY created_at
        LIMIT 0, 1
    ),
    (SELECT id FROM campus_venue WHERE name = '미래관 앞문 (조형관방향)'),
    1,
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (
        SELECT id
        FROM campus_path
        WHERE from_building_id = (SELECT id FROM campus_building WHERE name = '미래관')
          AND to_building_id = (SELECT id FROM campus_building WHERE name = '북악관')
        ORDER BY created_at
        LIMIT 0, 1
    ),
    (SELECT id FROM campus_venue WHERE name = '조형관 앞 계단'),
    2,
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (
        SELECT id
        FROM campus_path
        WHERE from_building_id = (SELECT id FROM campus_building WHERE name = '미래관')
          AND to_building_id = (SELECT id FROM campus_building WHERE name = '북악관')
        ORDER BY created_at
        LIMIT 0, 1
    ),
    (SELECT id FROM campus_venue WHERE name = '경영관 앞문'),
    3,
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (
        SELECT id
        FROM campus_path
        WHERE from_building_id = (SELECT id FROM campus_building WHERE name = '미래관')
          AND to_building_id = (SELECT id FROM campus_building WHERE name = '북악관')
        ORDER BY created_at
        LIMIT 0, 1
    ),
    (SELECT id FROM campus_venue WHERE name = '경영관 후문'),
    4,
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (
        SELECT id
        FROM campus_path
        WHERE from_building_id = (SELECT id FROM campus_building WHERE name = '미래관')
          AND to_building_id = (SELECT id FROM campus_building WHERE name = '북악관')
        ORDER BY created_at
        LIMIT 0, 1
    ),
    (SELECT id FROM campus_venue WHERE name = '경상관-국제관 사이 계단'),
    5,
    NOW(),
    NOW()
),
(
    UUID_TO_BIN(UUID()),
    (
        SELECT id
        FROM campus_path
        WHERE from_building_id = (SELECT id FROM campus_building WHERE name = '미래관')
          AND to_building_id = (SELECT id FROM campus_building WHERE name = '북악관')
        ORDER BY created_at
        LIMIT 0, 1
    ),
    (SELECT id FROM campus_venue WHERE name = '북악관 오른쪽 입구'),
    6,
    NOW(),
    NOW()
);