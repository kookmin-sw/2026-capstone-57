# DB 스키마 관리 컨벤션

## 방식

Flyway 마이그레이션을 사용하지 않는다. JPA `ddl-auto: update` 설정을 통해 엔티티 기반으로 테이블을 자동 생성/수정한다.

## 규칙

- 엔티티 클래스가 스키마의 단일 진실 공급원(Single Source of Truth)이다
- 테이블 변경이 필요하면 엔티티 클래스를 수정한다
- 별도의 SQL 마이그레이션 파일을 생성하지 않는다
- `spring.jpa.hibernate.ddl-auto=update` 설정을 유지한다
