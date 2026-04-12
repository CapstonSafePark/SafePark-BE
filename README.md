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
- `POST /api/auth/logout` - 로그아웃
- `POST /api/auth/refresh` - 토큰 갱신
- `GET /api/auth/test` - API 테스트

### 관리자 API (담당: 선태웅)
- `GET /api/admin/users` - 사용자 목록 조회 (Pagination + 검색)
- `GET /api/admin/users/{id}` - 사용자 상세 조회 (Statistics 포함)
- `GET /api/admin/stats` - 전체 통계 조회
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

## 주요 기능

### JWT 인증
- Access Token (1시간)
- Refresh Token (7일)
- BCrypt 비밀번호 암호화

### Pagination
- 사용자 목록 페이징 (page, limit 지원)
- 검색 기능 (username, email)

### Statistics
- 사용자별 분석 통계
- 위험도별 분석 통계 (HIGH/MEDIUM/LOW)
- 오늘 활성 사용자 수

### CORS 설정
프론트엔드 연동을 위한 CORS 설정 완료

**허용된 Origin:**
- `http://localhost:3000` (React 개발 서버)
- `http://localhost:3001` (추가 포트)

**허용된 메서드:** GET, POST, PUT, DELETE  
**허용된 헤더:** 모두 허용

---

## 데이터베이스 ERD

**테이블:**
- `user` - 사용자 정보
- `refresh_token` - JWT Refresh Token
- `parking_lot` - 주차장 정보 (ERD 일치)
- `analysis_log` - 분석 기록 (ERD 일치)

---

## 진행 상황

- 2026.04.11 - 인증 API 완성
- 2026.04.11 - 관리자 API 완성
- 2026.04.12 - 주차장 API 완성
- 2026.04.12 - Pagination + 검색 기능 추가
- 2026.04.12 - Statistics 상세 구현
- 2026.04.12 - 로그아웃/토큰갱신 API 추가
- 2026.04.12 - ERD/명세서 일치 완료
