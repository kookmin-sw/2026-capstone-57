# Flyway 마이그레이션 컨벤션

## 버전 네이밍 규칙

마이그레이션 파일 생성 시 **타임스탬프 기반 버전**을 사용한다.

### 형식

```
V{YYYYMMDD}{3자리 시퀀스}__{설명}.sql
```

### 예시

```
V20260506001__create_campus_building_table.sql
V20260506002__add_interaction_column.sql
V20260507001__create_notification_table.sql
```

### 규칙

- `YYYYMMDD`: 마이그레이션 생성 날짜
- `3자리 시퀀스`: 같은 날 여러 개 생성 시 001, 002, 003 순서로 증가
- `__` (언더스코어 2개) 뒤에 설명을 snake_case로 작성
- 기존 V1~V10 마이그레이션은 그대로 유지 (수정 금지)
- 마이그레이션 파일 위치: `backend/src/main/resources/db/migration/`
