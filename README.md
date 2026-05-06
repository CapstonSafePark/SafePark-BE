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

### 내 계정 API (담당: 선태웅)
- `GET /api/users/me` - 내 정보 조회
- `PUT /api/users/me` - 내 정보 수정 (name, phone)
- `PUT /api/users/me/password` - 비밀번호 변경
- `DELETE /api/users/me` - 회원 탈퇴

### 지도 및 위치 API (담당: 선태웅)
- `POST /api/location/check-parking` - 현재 위치 주차 가능 여부 및 위험도 분석
- `GET /api/parking-lots/nearby` - 주변 주차장 검색 (param: latitude, longitude, radius)
- `GET /api/parking-lots/{id}` - 주차장 상세 정보

### 이미지 분석 API (담당: 선태웅)
- `POST /api/analysis/upload-image` - 주차위반 이미지 업로드 및 분석
- `GET /api/analysis/{analysisId}` - 분석 결과 상세 조회
- `GET /api/analysis/recent` - 최근 분석 결과 목록 (param: limit)

### 분석 이력 API (담당: 선태웅)
- `GET /api/history` - 분석 이력 목록 조회 (param: page, limit, startDate, endDate)
- `GET /api/history/{historyId}` - 분석 이력 상세 조회
- `DELETE /api/history/{historyId}` - 분석 이력 삭제
- `DELETE /api/history/all` - 전체 분석 이력 삭제

### AI 챗봇 API (담당: 선태웅)
- `POST /api/chatbot/message` - 챗봇 메시지 전송
- `GET /api/chatbot/history` - 대화 내역 조회 (param: limit)
- `DELETE /api/chatbot/history` - 대화 이력 삭제

### 관리자 API (담당: 선태웅)
- `GET /api/admin/users` - 사용자 목록 조회 (param: page, limit, searchKeyword)
- `GET /api/admin/users/{id}` - 사용자 상세 조회
- `GET /api/admin/statistics` - 통계 조회
- `POST /api/admin/parking-lots` - 주차장 등록
- `PUT /api/admin/parking-lots/{id}` - 주차장 수정
- `DELETE /api/admin/parking-lots/{id}` - 주차장 삭제

---

## 서버 주소

- **로컬:** `http://localhost:8080`
- **운영 (AWS EC2):** `http://13.124.185.9:8080`

---

## 실행 방법

```bash
# 1. PostgreSQL 데이터베이스 생성
CREATE DATABASE safepark_db;

# 2. application.properties 설정
spring.datasource.url=jdbc:postgresql://localhost:5432/safepark_db
spring.datasource.username=postgres
spring.datasource.password=your_password

# 3. 로컬 실행
./gradlew bootRun

# 4. 배포용 JAR 빌드
./gradlew clean bootJar
```

---

## CORS 설정

프론트엔드 연동을 위한 CORS 설정 완료 (모든 Origin 허용)

**허용된 메서드:** GET, POST, PUT, DELETE
**허용된 헤더:** 모두 허용

---

## 진행 상황

- 2026.04.11 - 인증 API 완성
- 2026.04.11 - 관리자 API 완성
- 2026.04.12 - 주차장 API 완성
- 2026.04.29 - 분석 이력 API 완성
- 2026.05.06 - 내 계정 / 위치 / 이미지 분석 / 챗봇 API 완성
- 2026.05.06 - JWT 인증 403 오류 수정 (CustomUserDetailsService 추가)
