<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="100" alt="Howscat" />

# Howscat

**AI 기반 고양이 건강 종합 관리 — Android + Spring Boot 풀스택 개인 프로젝트**

[![Android](https://img.shields.io/badge/Android-Java-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io)
[![Railway](https://img.shields.io/badge/Live_Deploy-Railway-0B0D0E?style=flat-square&logo=railway&logoColor=white)](https://railway.app)
[![Gemini](https://img.shields.io/badge/Gemini_2.0_Flash-4285F4?style=flat-square&logo=google&logoColor=white)](https://deepmind.google/technologies/gemini/)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com)
[![minSdk](https://img.shields.io/badge/minSdk_24-Android_7.0+-green?style=flat-square)](#)

<br>

> 토사물 사진 한 장으로 AI 분석 · 체중·먹이·건강검진·투약·화장실까지 통합 관리
> Spring Boot 백엔드 직접 설계 · Railway 실배포 · Gemini Vision API 연동
> 기획 → 설계 → 구현 → 배포 **1인 풀스택 개발**

<br>

<img src="https://github.com/user-attachments/assets/bea689e4-343b-48a9-8c4d-2e648eca52d5" width="18%" />
<img src="https://github.com/user-attachments/assets/1620327d-c003-4957-af9b-18b74a056837" width="18%" />
<img src="https://github.com/user-attachments/assets/7dfa1ab5-5985-4272-9277-0c3062902a08" width="18%" />
<img src="https://github.com/user-attachments/assets/9f21a426-b202-405f-86f4-9a88144216c3" width="18%" />
<img src="https://github.com/user-attachments/assets/828d095a-d441-4fcb-b47d-a78bc8cb66a4" width="18%" />

</div>

---

## 왜 만들었나

고양이를 키우면서 건강 기록을 제대로 관리할 앱이 없었다.
단순 기록 앱이 아닌 **AI 분석 + 실알림 + 실배포**까지 끝낸 프로젝트를 목표로 삼았다.
백엔드 설계부터 Android UI, Railway 배포까지 전 과정을 혼자 다뤘다.

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| 🤖 **AI 토사물 분석** | 사진 찍으면 색상·형태·위험도 자동 분석 (Gemini Vision) |
| 📊 **AI 건강 요약** | 최근 7일 기록을 종합해 한 줄 건강 조언 생성 |
| ⚖️ **체중 & 사료 계산** | 몸무게 입력 시 적정 물·사료량 자동 계산 및 서버 저장 |
| 📏 **비만도 검사** | 허리·뒷다리 치수로 체지방률 추정 및 레벨 판정 |
| 📅 **통합 캘린더** | 메모·체중·구토·건강검진·예방접종을 한 화면에서 관리 |
| 🔔 **정확 알림** | 건강검진·예방접종 D-7 / D-1 / D-Day 3단계 + 스누즈 |
| 🏥 **주변 병원 검색** | GPS 기반 반경 내 동물병원 검색 + 즐겨찾기 |
| 💊 **투약 기록** | 투약 일정 등록·알림·이력 관리 |
| 🚽 **화장실 기록** | 배변 횟수·상태 기록 및 이력 관리 |
| 🐱 **다중 고양이** | 고양이 여러 마리 전환 지원 |

---

## 기술 스택

### Android 클라이언트

| 항목 | 내용 |
|------|------|
| 언어 | Java 17 |
| 빌드 | Gradle 8.x |
| 네트워크 | Retrofit 2 + OkHttp3 |
| 인증 처리 | AuthInterceptor (토큰 자동 첨부) + AuthAuthenticator (401 시 자동 갱신) |
| 알림 | AlarmManager — Android 12+ 정확 알람 권한 분기 |
| 로컬 저장 | SharedPreferences — 인증 / 프로필 / 케어 결과 / 알람 파일 분리 |
| 이미지 | Glide · Camera/Gallery Intent → Base64 인코딩 |
| Min / Target SDK | 24 / 36 |

### Spring Boot 백엔드

| 항목 | 내용 |
|------|------|
| 프레임워크 | Spring Boot 3 |
| 언어 | Java 17 |
| 인증 | JWT — Access Token + Refresh Token |
| DB 접근 | JdbcTemplate (ORM 미사용, 쿼리 직접 제어) |
| AI | Google Gemini 2.0 Flash — Vision (이미지 분석) + Text (건강 요약) |
| 외부 API | Kakao Local (병원 검색 프록시) |
| 스키마 관리 | ApplicationRunner `@Order(1)` — 배포 시 DDL 자동 실행 |
| 배포 | Railway (MySQL + Spring Boot 컨테이너) |

---

## 아키텍처

<table>
<tr>
<td width="100%">

**📱 Android App**
- SharedPreferences(`auth`) — accessToken / refreshToken / lastViewedCatId
- Retrofit2 → AuthInterceptor로 모든 요청에 Bearer 토큰 자동 첨부
- 401 수신 시 AuthAuthenticator가 토큰 갱신 후 원 요청 자동 재시도

↓ &nbsp;&nbsp; HTTPS + JWT

**⚙️ Spring Boot — Railway 배포**
- 인증 API `/api/users/` — login · signup · refresh
- 케어 API `/cats/{catId}/` — 40개 이상 엔드포인트, catId 소유권 검증
- Gemini Vision — 토사물 이미지 Base64 → 색상·형태·위험도 JSON 분석
- Gemini Text — 7일 케어 데이터 집계 → 한 줄 건강 조언 (6시간 캐시)
- Kakao Local 프록시 — GPS 좌표 기반 주변 병원 검색
- SchemaInitializer — 앱 시작 시 16개 테이블 자동 생성 · 컬럼 마이그레이션

↓ &nbsp;&nbsp; JdbcTemplate

**🗄️ MySQL — Railway**
- 16개 테이블 (앱 시작 시 자동 생성, 수동 마이그레이션 불필요)

</td>
</tr>
</table>

---

## 구현 포인트

### JWT 자동 갱신
모든 요청에 토큰을 자동 첨부하고, 401 응답 시 `AuthAuthenticator`가 토큰을 갱신한 뒤 원 요청을 투명하게 재시도한다. 갱신 실패 시에만 로그인 화면으로 이동.

### 고양이별 알림 격리
고양이 여러 마리 시 알람 ID 충돌을 방지하기 위해 고양이별 SharedPreferences 키를 분리. D-7 / D-1 / D-Day 3단계 알림, 스누즈 1시간 재알림 지원.

### Railway 무중단 스키마
Railway는 빈 DB를 제공한다. `SchemaInitializer`(ApplicationRunner)로 앱 시작 시 16개 테이블을 자동 생성하고, 기존 테이블에 컬럼이 없을 경우 `INFORMATION_SCHEMA`를 조회해 ALTER만 실행 — 재배포 시 수동 마이그레이션 0건.

### 캘린더 단일 쿼리
메모·건강일정·체중·구토 5개 테이블을 `UNION ALL`로 묶어 날짜순 단일 응답으로 반환. 각 이벤트 타입(MEMO / HEALTH_CHECKUP / HEALTH_VACCINE / WEIGHT / VOMIT)을 클라이언트에서 구분해 아이콘·색상을 다르게 표시.

### 케어 결과 서버 복원
물·사료 계산 결과를 서버 DB(`weight_record`)에도 저장해두어, 앱 재설치·재로그인 후에도 홈 화면에서 마지막 계산값을 자동으로 복원.

---

## DB 스키마

| 테이블 | 설명 |
|--------|------|
| `users` | 사용자 계정 |
| `cat` | 고양이 정보 |
| `health_type` | 건강 검진 유형 시드 |
| `health_schedule` | 건강검진·예방접종 일정 |
| `weight_record` | 체중 + 추천 물·사료량 |
| `obesity_check_record` | 비만도 검사 결과 |
| `vomit_status` | 구토 상태 기준 (7색 × 8형태 = 56행 시드) |
| `vomit_record` | 구토 기록 + AI 분석 결과 |
| `calendar_memo` | 캘린더 메모 |
| `medication` | 투약 기록 |
| `litter_box_record` | 화장실 기록 |
| `vet_visit` | 진료 기록 |
| `hospital` | 병원 정보 |
| `favorite_hospital` | 즐겨찾기 병원 |
| `notification` | 알림 기록 |
| `ai_health_summary_cache` | AI 건강 요약 캐시 (6h TTL) |

---

## 프로젝트 구조

**Android 클라이언트** (`howscat/app/src/main/java/com/example/howscat/`)

| 파일 | 역할 |
|------|------|
| `HomeActivity` | 멀티캣 전환 다이얼로그 · 상단 바 |
| `HomeFragment` | AI 요약 · 건강 일정 카드 · 케어 요약 복원 |
| `CatFragment` | 체중·비만도·사료 계산 · 투약·화장실·진료 기록 |
| `CalendarFragment` | 통합 이벤트 캘린더 · 건강 일정 등록 |
| `HospitalFragment` | Kakao 반경 병원 검색 · 즐겨찾기 |
| `VomitAnalyzeActivity` | 토사물 AI 분석 화면 |
| `CareResultPrefs` | 케어 요약 고양이별 SharedPreferences 저장 |
| `HealthScheduleAlarmScheduler` | D-7 / D-1 / D-Day 알람 등록 |
| `HealthScheduleAlarmReceiver` | 알람 수신 + 스누즈 처리 |
| `MedicationAlarmScheduler` | 투약 알람 |
| `network/AuthInterceptor` | 모든 요청에 Bearer 토큰 자동 첨부 |
| `network/AuthAuthenticator` | 401 → 자동 토큰 갱신 |
| `network/ApiService` | Retrofit 인터페이스 (40개 엔드포인트) |

**Spring Boot 백엔드** (`springserver/src/main/java/com/example/howscat/`)

| 파일 | 역할 |
|------|------|
| `config/SchemaInitializer` | 앱 시작 시 DDL 자동 실행 · 컬럼 마이그레이션 |
| `config/SecurityConfig` | JWT 필터 체인 |
| `service/AiHealthSummaryService` | Gemini Text 건강 요약 · 캐시 관리 |
| `service/VomitAnalysisService` | Gemini Vision 구토 분석 |
| `service/CalendarService` | UNION ALL 통합 캘린더 쿼리 |
| `service/ObesityCheckService` | 비만도·체중 계산 · 이력 조회 |
| `service/HospitalService` | Kakao Local API 프록시 |

---

## REST API 요약

| 영역 | 주요 엔드포인트 |
|------|----------------|
| 인증 | POST `/api/users/login` · `/signup` · `/logout` · `/refresh` |
| 고양이 | GET/POST `/cats` · `/cats/{id}` · `/cats/user` |
| AI | POST `/cats/{id}/vomit` · GET `/cats/{id}/ai-summary` |
| 케어 | POST `/cats/{id}/care-weight` · `/obesity-check` |
| 기록 | GET/POST/PUT/DELETE `/cats/{id}/medications` · `/litter-records` · `/vet-visits` |
| 캘린더 | GET `/cats/{id}/calendar?from=&to=` · POST/PUT/DELETE `/calendar-memos` |
| 병원 | GET `/hospitals/nearby` · `/hospitals/favorites` |

<details>
<summary>전체 엔드포인트 목록 펼치기</summary>

### 인증
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/users/login` | 로그인 → JWT 발급 |
| POST | `/api/users/signup` | 회원가입 |
| POST | `/api/users/logout` | 로그아웃 |
| POST | `/api/users/refresh` | Access Token 갱신 |

### 고양이
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/cats` | 고양이 등록 |
| GET | `/cats/{id}` | 정보 조회 |
| GET | `/cats/user` | 내 고양이 목록 |
| POST | `/cats/select/{catId}` | 활성 고양이 전환 |

### AI & 건강 분석
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/cats/{id}/vomit` | 토사물 Gemini Vision 분석 + 저장 |
| DELETE | `/cats/{id}/vomit/{vomitId}` | 구토 기록 삭제 |
| GET | `/cats/{id}/ai-summary` | Gemini 건강 요약 (6h 캐시) |
| POST | `/cats/{id}/care-weight` | 체중·사료량 계산 및 저장 |
| POST | `/cats/{id}/obesity-check` | 비만도 검사 |
| GET | `/cats/{id}/weight-history` | 체중 이력 (추천 물·사료량 포함) |
| GET | `/cats/{id}/obesity-history` | 비만도 이력 |

### 건강 일정
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/cats/{id}/health-schedules` | 건강 일정 목록 |
| POST | `/cats/{id}/health-schedules` | 일정 등록 |
| PUT | `/cats/{id}/health-schedules/{scheduleId}` | 일정 수정 |
| DELETE | `/cats/{id}/health-schedules/{scheduleId}` | 일정 삭제 |

### 케어 기록 (투약 · 화장실 · 진료)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET/POST | `/cats/{id}/medications` | 투약 목록·등록 |
| PUT/DELETE | `/cats/{id}/medications/{mid}` | 수정·삭제 |
| GET/POST | `/cats/{id}/litter-records` | 화장실 기록 |
| PUT/DELETE | `/cats/{id}/litter-records/{rid}` | 수정·삭제 |
| GET/POST | `/cats/{id}/vet-visits` | 진료 기록 |
| PUT/DELETE | `/cats/{id}/vet-visits/{vid}` | 수정·삭제 |

### 캘린더
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/cats/{id}/calendar?from=&to=` | 기간별 통합 이벤트 (UNION ALL) |
| POST | `/cats/{id}/calendar-memos` | 메모 작성 |
| PUT | `/cats/{id}/calendar-memos/{memoId}` | 메모 수정 |
| DELETE | `/cats/{id}/calendar-memos/{memoId}` | 메모 삭제 |

### 병원
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/hospitals/nearby?lat=&lng=&radius=` | 반경 내 병원 검색 |
| GET | `/hospitals/favorites` | 즐겨찾기 목록 |
| POST | `/hospitals/{id}/favorite` | 즐겨찾기 등록 |
| DELETE | `/hospitals/{id}/favorite` | 즐겨찾기 해제 |

</details>

---

## 로컬 실행

### 요구사항

| 항목 | 버전 |
|------|------|
| Android Studio | Giraffe 이상 |
| JDK | 17 |
| 실기기 | API 24 이상 |

> 카메라 · GPS · AlarmManager 기능은 실기기 필수 (에뮬레이터 일부 제한)

### Android 실행 순서

1. `local.properties`에 `api.base.url`과 `kakao.rest.api.key` 추가
2. Android Studio → **Sync Project with Gradle Files**
3. 실기기 연결 후 **Run ▶**

배포 서버(`https://howscatbackend-production.up.railway.app/`)가 이미 운영 중이므로 백엔드 별도 실행 없이 바로 앱 테스트 가능.

### 백엔드 로컬 실행

`springserver/` 디렉터리에서 환경 변수 설정 후 `./gradlew bootRun` 실행.
테이블은 시작 시 자동 생성되므로 별도 DDL 실행 불필요.

---

## 환경 변수

### Android — `local.properties`

| 키 | 설명 |
|----|------|
| `api.base.url` | 배포 서버 URL (끝에 `/` 필수) |
| `kakao.rest.api.key` | Kakao Local REST API 키 |

### Spring Boot — Railway Variables

| 변수 | 설명 |
|------|------|
| `DATABASE_URL` | Railway MySQL JDBC URL |
| `DATABASE_USERNAME` | DB 사용자명 |
| `DATABASE_PASSWORD` | DB 비밀번호 |
| `GEMINI_API_KEY` | Google AI Studio API 키 |
| `JWT_SECRET` | JWT 서명 키 (256bit 이상 권장) |
| `KAKAO_REST_API_KEY` | Kakao Local API 키 |

---

<div align="center">

**Spring Boot · Railway 실배포 · Gemini Vision AI · 1인 풀스택 개발**

</div>
