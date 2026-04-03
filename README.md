<div align="center">

<img src="https://github.com/user-attachments/assets/67246f78-c630-4e92-bde9-d976159038cd" width="100" alt="Howscat" />

# Howscat

**AI 기반 고양이 건강 종합 관리 — Android + Spring Boot 풀스택 개인 프로젝트**

[![Android](https://img.shields.io/badge/Android-Java-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io)
[![Railway](https://img.shields.io/badge/Live_Deploy-Railway-0B0D0E?style=flat-square&logo=railway&logoColor=white)](https://railway.app)
[![Gemini](https://img.shields.io/badge/Gemini_2.5_Flash-4285F4?style=flat-square&logo=google&logoColor=white)](https://deepmind.google/technologies/gemini/)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com)
[![minSdk](https://img.shields.io/badge/minSdk_24-Android_7.0+-green?style=flat-square)](#)

<br>

> 토사물 사진 한 장으로 AI 분석 · 체중·먹이·건강검진·투약·화장실까지 통합 관리
> Spring Boot 백엔드 직접 설계 · Railway 실배포 · Gemini Vision API 연동
> 기획 → 설계 → 구현 → 배포 **1인 풀스택 개발**

<br>

<img src="https://github.com/user-attachments/assets/0c07a90a-2a99-4068-912b-b83336b9665d" width="18%" />
<img src="https://github.com/user-attachments/assets/aca692e2-2de2-4b3b-aba5-7030b32bf6fe" width="18%" />
<img src="https://github.com/user-attachments/assets/be43fb1b-f53e-4df3-abd1-4014b98de1f1" width="18%" />
<img src="https://github.com/user-attachments/assets/50be5eb1-9df7-436c-a3bc-6967a8e24dda" width="18%" />
<img src="https://github.com/user-attachments/assets/ae3a940e-1308-431d-b402-c5313743c461" width="18%" />

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
| AI | Google Gemini 2.5 Flash — Vision (이미지 분석) + Text (건강 요약) |
| 외부 API | Kakao Local (병원 검색 프록시) |
| 스키마 관리 | ApplicationRunner `@Order(1)` — 배포 시 DDL 자동 실행 |
| 배포 | Railway (MySQL + Redis + Spring Boot 컨테이너) |

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

---

<div align="center">

**Spring Boot · Railway 실배포 · Gemini 2.5 Flash · 1인 풀스택 개발**

</div>
