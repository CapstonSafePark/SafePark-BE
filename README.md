# 🅿️ AI 기반 주정차 과태료 예측 및 주차 대안 추천 서비스

## 서비스 핵심 정의
> 주차 직후 과태료 발생 확률을 예측하고, 주변 주차 대안을 함께 제공하는 AI 기반 주차 의사결정 지원 서비스

## 프로젝트 구조
```
parking-fine-ai/
├── frontend/                  # React Native (Expo)
│   ├── app/
│   │   ├── (tabs)/
│   │   │   ├── index.tsx      # 메인 (지도 + 위치 기반 분석)
│   │   │   ├── camera.tsx     # 도로 촬영
│   │   │   ├── chat.tsx       # 챗봇 인터페이스
│   │   │   ├── parking.tsx    # 주변 주차장 목록/지도
│   │   │   └── resident.tsx   # 거주자 주차 거래 (채팅)
│   │   └── _layout.tsx
│   ├── components/
│   │   ├── ChatBubble.tsx
│   │   ├── MapView.tsx
│   │   ├── ResultGauge.tsx    # 확률 게이지 (원형)
│   │   ├── ParkingCard.tsx    # 주차장 카드 (☀/Free 표시)
│   │   └── StreetViewModal.tsx
│   ├── services/
│   │   └── api.ts
│   ├── package.json
│   └── app.json
│
├── backend/
│   ├── app/
│   │   ├── main.py            # FastAPI 엔트리포인트
│   │   ├── routers/
│   │   │   ├── analyze.py     # POST /analyze (위치+이미지 → 확률)
│   │   │   ├── chat.py        # POST /chat (챗봇 응답)
│   │   │   ├── parking.py     # GET /parking/nearby (주변 주차장)
│   │   │   └── resident.py    # 거주자 주차 거래 API
│   │   ├── services/
│   │   │   ├── location.py    # GPS → 단속구역 매칭
│   │   │   ├── vision.py      # 이미지 분석 (OpenAI Vision)
│   │   │   ├── chatbot.py     # LLM 응답 생성
│   │   │   ├── streetview.py  # 거리뷰 API 연동
│   │   │   ├── parking_lot.py # 공영주차장 조회 (공공데이터)
│   │   │   └── crackdown.py   # 단속 시간/이력 조회
│   │   ├── models/
│   │   │   └── schemas.py     # Pydantic 모델
│   │   └── data/
│   │       ├── zones.json         # 단속구역 데이터
│   │       ├── parking_lots.json  # 공영주차장 데이터
│   │       └── crackdown_log.json # 최근 단속 이력
│   ├── requirements.txt
│   └── Dockerfile
│
├── docs/
│   ├── 기획서.pdf
│   └── 미팅기록.md
├── .gitignore
├── .env.example
└── README.md
```

## 팀원 및 역할
| 역할 | 담당 | 핵심 업무 |
|------|------|-----------|
| A - 프론트엔드 | TBD | 지도/UI/거주자 채팅 화면 |
| B - 백엔드/데이터 | TBD | FastAPI, 공공데이터, DB |
| C - AI/Vision | TBD | 이미지 분석, 거리뷰, 확률 계산 |
| D - 챗봇/통합/발표 | TBD | LLM 연동, 테스트, 문서 |

## 실행 방법
### Backend
```bash
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```
### Frontend
```bash
cd frontend
npm install
npx expo start
```

## 환경변수
`.env.example` 참고

## 마일스톤
- **~4/6**: 개별 파트 기본 구현 완료
- **~4/20**: 통합 + 주요 기능 연동
- **~5/1**: UI 다듬기 + 데모 리허설
- **5/4 17시**: 데모 발표
