# SafePark Backend API

AI 기반 주정차 과태료 예측 및 주차 대안 추천 서비스 - 백엔드

## 기술 스택

- Spring Boot 3.5.13
- Java 17
- PostgreSQL 16.13
- JWT Authentication
- Gradle

---

## 완성된 API

### 인증 API (담당: 선태웅)
- `POST /api/auth/register` - 회원가입
- `POST /api/auth/login` - 로그인
- `GET /api/auth/test` - API 테스트

### 관리자 API (담당: 선태웅)
- `GET /api/admin/users` - 사용자 목록 조회
- `GET /api/admin/users/{id}` - 사용자 상세 조회
- `GET /api/admin/stats` - 통계 조회
- `POST /api/admin/parking-lots` - 주차장 등록
- `PUT /api/admin/parking-lots/{id}` - 주차장 수정
- `DELETE /api/admin/parking-lots/{id}` - 주차장 삭제



---

## 실행 방법

```bash
# 1. PostgreSQL 데이터베이스 생성
CREATE DATABASE safepark_db;

# 2. application.properties 설정
spring.datasource.url=jdbc:postgresql://localhost:5432/safepark_db
spring.datasource.username=postgres
spring.datasource.password=your_password

# 3. 실행
./gradlew bootRun
```

서버: `http://localhost:8080`

---

## 진행 상황

-  2026.04.11 - 인증 API 완성
-  2026.04.11 - 관리자 API 완성
-  2026.04.12 - 주차장 API 완성
