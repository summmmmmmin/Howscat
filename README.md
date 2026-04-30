<div align="center">

<img src="https://github.com/user-attachments/assets/effb89ac-a9a1-420d-b4f8-406f97cca8d6" width="100" alt="Howscat" />

# Howscat

**AI 기반 고양이 건강 종합 관리 — Android + Spring Boot 풀스택 개인 프로젝트**

[![Android](https://img.shields.io/badge/Android-Java-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_4-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io)
[![AWS](https://img.shields.io/badge/Live_Deploy-AWS_EC2-FF9900?style=flat-square&logo=amazonaws&logoColor=white)](https://aws.amazon.com)
[![Gemini](https://img.shields.io/badge/Gemini_2.5_Flash-4285F4?style=flat-square&logo=google&logoColor=white)](https://deepmind.google/technologies/gemini/)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com)
[![minSdk](https://img.shields.io/badge/minSdk_24-Android_7.0+-green?style=flat-square)](#)

<br>

> 토사물 사진 한 장으로 AI 분석 · 체중·먹이·건강검진·투약·화장실까지 통합 관리
> Spring Boot 백엔드 직접 설계 · AWS EC2 실배포 · Gemini Vision API 연동
> 기획 → 설계 → 구현 → 배포 **1인 풀스택 개발**

<br>

<img src="https://github.com/user-attachments/assets/3bb22c20-2fc8-4ac6-8614-15b5fef036a3" width="18%" />
<img src="https://github.com/user-attachments/assets/5984e81f-96cd-4dc8-8462-6f0158cfe864" width="18%" />
<img src="https://github.com/user-attachments/assets/e6bb90cb-b1de-4576-94ac-7cca03b43f01" width="18%" />

<br>

<img src="https://github.com/user-attachments/assets/c9160ce7-c5e0-4f71-88e0-d26c81179b89" width="18%" />
<img src="https://github.com/user-attachments/assets/b53b835c-26ff-4520-838d-fa487a56a6b2" width="18%" />
<img src="https://github.com/user-attachments/assets/b9e667ec-97ea-4787-839c-6cfae208e7dd" width="18%" />

</div>

---

## 왜 만들었나

고양이를 키우면서 건강 기록을 제대로 관리할 앱이 없었다.
단순 기록 앱이 아닌 **AI 분석 + 실알림 + 실배포**까지 끝낸 프로젝트를 목표로 삼았다.
백엔드 설계부터 Android UI, AWS EC2 배포까지 전 과정을 혼자 다뤘다.

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| **AI 토사물 분석** | 사진 찍으면 색상·형태·위험도 자동 분석 (Gemini Vision) |
| **AI 건강 요약** | 최근 7일 기록을 종합해 한 줄 건강 조언 생성 |
| **체중 & 사료 계산** | 몸무게 입력 시 적정 물·사료량 자동 계산 및 서버 저장 |
| **비만도 검사** | 허리·뒷다리 치수로 체지방률 추정 및 레벨 판정 |
| **통합 캘린더** | 메모·체중·구토·건강검진·예방접종을 한 화면에서 관리 |
| **정확 알림** | 건강검진·예방접종 D-7 / D-1 / D-Day 3단계 + 스누즈 |
| **주변 병원 검색** | GPS 기반 반경 내 동물병원 검색 + 즐겨찾기 |
| **투약 기록** | 투약 일정 등록·알림·이력 관리 |
| **화장실 기록** | 배변 횟수·상태 기록 및 이력 관리 |
| **다중 고양이** | 고양이 여러 마리 전환 지원 |

---

## 기술 스택

### Android 클라이언트

| 항목 | 내용 |
|------|------|
| 언어 | Java 11 |
| 빌드 | Gradle 8.x |
| 네트워크 | Retrofit 2 + OkHttp3 |
| 인증 처리 | AuthInterceptor (토큰 자동 첨부) + AuthAuthenticator (401 시 자동 갱신) |
| 알림 | AlarmManager — Android 12+ 정확 알람 권한 분기 |
| 로컬 저장 | SharedPreferences — 인증 / 프로필 / 케어 결과 / 알람 파일 분리 |
| 이미지 | BitmapFactory (inSampleSize=4 직접 다운샘플링) · Camera/Gallery Intent → Base64 인코딩 |
| 커스텀 View | SimpleLineChartView — 외부 라이브러리 없이 Canvas API로 직접 구현 (체중·비만도·구토 7일 그래프) |

### Spring Boot 백엔드

| 항목 | 내용 |
|------|------|
| 프레임워크 | Spring Boot 4.0.2 |
| 언어 | Java 17 |
| 인증 | JWT — Access Token + Refresh Token |
| DB 접근 | Spring Data JPA + JdbcTemplate (단순 CRUD는 JPA, 복잡 집계·UNION 쿼리는 JdbcTemplate) |
| Cache / 인증 저장 | Redis — Refresh Token 저장 / Blacklist / AI 요약 캐시 |
| AI | Google Gemini 2.5 Flash — Vision (이미지 분석) + Text (건강 요약) |
| 외부 API | Kakao Local (병원 검색 프록시) |
| 스키마 관리 | ApplicationRunner `@Order(1)` — 배포 시 DDL 자동 실행 |
| 배포 | AWS EC2 — Docker 컨테이너 · GitHub Actions CI/CD |

---

## 아키텍처

<table>
<tr>
<td width="100%">

**Android App**
- SharedPreferences(`auth`) — accessToken / refreshToken / lastViewedCatId
- Retrofit2 → AuthInterceptor로 모든 요청에 Bearer 토큰 자동 첨부
- 401 수신 시 AuthAuthenticator가 토큰 갱신 후 원 요청 자동 재시도

↓ &nbsp;&nbsp; HTTPS + JWT

**Spring Boot — AWS EC2 배포**
- 인증 API `/api/users/` — login · signup · refresh
- 케어 API `/cats/{catId}/` — 40개 이상 엔드포인트, catId 소유권 검증
- Gemini Vision — 토사물 이미지 Base64 → 색상·형태·위험도 JSON 분석
- Gemini Text — 7일 케어 데이터 집계 → 한 줄 건강 조언 (6시간 캐시)
- Kakao Local 프록시 — GPS 좌표 기반 주변 병원 검색
- SchemaInitializer — 앱 시작 시 16개 테이블 자동 생성 · 컬럼 마이그레이션

↓ &nbsp;&nbsp; Spring Data JPA / JdbcTemplate

**MySQL — AWS RDS**
- 16개 테이블 (앱 시작 시 자동 생성, 수동 마이그레이션 불필요)

</td>
</tr>
</table>

---

## 구현 포인트

### JWT 자동 갱신
모든 요청에 토큰을 자동 첨부하고, 401 응답 시 `AuthAuthenticator`가 토큰을 갱신한 뒤 원 요청을 투명하게 재시도한다. 갱신 실패 시에만 로그인 화면으로 이동.

### 고양이별 알림 격리 & 신뢰성
`setInexactRepeating` 대신 `setExactAndAllowWhileIdle` 1회 예약 + Receiver 자기 재예약 패턴을 적용해 Doze 모드에서도 정시 발송을 보장한다. Request Code는 스케줄러별로 공식이 다르다 — 투약: `catId × 1,000,000 + medicationId × 10 + slot`, 건강일정: `scheduleId × 10 + offsetDay`. 고양이·일정·슬롯 간 충돌을 방지한다. D-7 / D-1 / D-Day 3단계 알림, 스누즈 1시간 재알림 지원.

### 재부팅 후 알람 자동 복구
AlarmManager 알람은 재부팅 시 OS가 전부 삭제한다. `BootReceiver`가 `BOOT_COMPLETED` / `LOCKED_BOOT_COMPLETED`를 수신하면 SharedPreferences StringSet에 저장된 모든 catId를 복원해 건강검진·투약·급여 알람을 전부 재등록한다. 마지막으로 조회한 고양이 ID도 이중 안전장치로 추가해 알람 소실 케이스를 원천 차단했다.

### Railway 무중단 스키마
Railway는 빈 DB를 제공한다. `SchemaInitializer`(ApplicationRunner)로 앱 시작 시 16개 테이블을 자동 생성하고, 기존 테이블에 컬럼이 없을 경우 `INFORMATION_SCHEMA`를 조회해 ALTER만 실행 — 재배포 시 수동 마이그레이션 0건.

### 캘린더 단일 쿼리
메모·건강일정·체중·구토 5개 소스를 `UNION ALL`로 묶고, 투약·화장실·진료 3개는 별도 쿼리로 합산해 총 8개 데이터 소스를 날짜순 단일 응답으로 반환. 각 이벤트 타입(MEMO / HEALTH_CHECKUP / HEALTH_VACCINE / WEIGHT / VOMIT / MEDICATION / LITTER / VET_VISIT)을 클라이언트에서 구분해 아이콘·색상을 다르게 표시.

### RER 기반 영양 계산
안정 시 에너지 요구량 RER = (체중 × 30) + 70 kcal에 연령 계수를 곱해 DER를 산출하고, 사료 kcal/g으로 나눠 1일 권장 사료량을 계산한다. 체중 × 50ml로 하루 권장 음수량도 함께 제공한다.

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
| `HospitalFragment` | 서버 API 통해 주변 병원 검색 (Kakao Local 프록시) · 즐겨찾기 서버 저장 |
| `VomitAnalyzeActivity` | 토사물 AI 분석 화면 |
| `CareResultPrefs` | 케어 요약 고양이별 SharedPreferences 저장 |
| `HealthScheduleAlarmScheduler` | D-7 / D-1 / D-Day 알람 등록 |
| `HealthScheduleAlarmReceiver` | 알람 수신 + 스누즈 처리 |
| `MedicationAlarmScheduler` | 투약 알람 |
| `network/AuthInterceptor` | 모든 요청에 Bearer 토큰 자동 첨부 |
| `network/AuthAuthenticator` | 401 → 자동 토큰 갱신 |
| `network/ApiService` | Retrofit 인터페이스 (44개 엔드포인트) |

---

<div align="center">

**Spring Boot · AWS EC2 실배포 · Gemini 2.5 Flash · 1인 풀스택 개발**

</div>
