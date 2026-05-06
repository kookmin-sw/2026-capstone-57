# Flyway 마이그레이션 컨벤션

## 버전 네이밍 규칙

마이그레이션 파일 생성 시 **타임스탬프 기반 버전**을 사용한다.

### 형식

```
V{YYYYMMDDHHmm}__{설명}.sql
```

### 예시

```
V202605061528__create_campus_building_table.sql
V202605061545__add_interaction_column.sql
V202605071030__create_notification_table.sql
```

### 규칙

- `YYYYMMDDHHmm`: 마이그레이션 생성 날짜와 시간 (24시간 형식)
- `__` (언더스코어 2개) 뒤에 설명을 snake_case로 작성
- 기존 V1~V10 마이그레이션은 그대로 유지 (수정 금지)
- 마이그레이션 파일 위치: `backend/src/main/resources/db/migration/`
