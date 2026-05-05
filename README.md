<div align="center">

<img src="https://github.com/user-attachments/assets/effb89ac-a9a1-420d-b4f8-406f97cca8d6" width="100" alt="Howscat" />

# Howscat

**AI 기반 고양이 건강 종합 관리 — Android + Spring Boot 풀스택 개인 프로젝트**

[![Android](https://img.shields.io/badge/Android-Java-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_4-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io)
[![AWS](https://img.shields.io/badge/Live_Deploy-AWS_EC2-FF9900?style=flat-square&logo=amazonaws&logoColor=white)](https://aws.amazon.com)
[![Gemini](https://img.shields.io/badge/Gemini_2.5_Flash-4285F4?style=flat-square&logo=google&logoColor=white)](https://deepmind.google/technologies/gemini/)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com)
[![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)](https://redis.io)
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

| 항목 | 내용 |
|------|------|
| 역할 | Android 클라이언트 + Spring Boot 백엔드 + 인프라 전담 |
| 규모 | REST API 44개 · MySQL 16개 테이블 |
| 배포 | AWS EC2 · Docker · GitHub Actions CI/CD |

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| **AI 토사물 분석** | 사진 찍으면 색상·형태·위험도 자동 분석 (Gemini Vision) |
| **AI 건강 요약** | 최근 7일 기록을 종합해 한 줄 건강 조언 생성 |
| **체중 & 사료 계산** | 몸무게 입력 시 RER 기반 적정 물·사료량 자동 계산 및 서버 저장 |
| **비만도 검사** | 허리·뒷다리 치수로 체지방률 추정 및 레벨 판정 |
| **통합 캘린더** | 메모·체중·구토·건강검진·예방접종·투약·화장실·진료 8종을 한 화면에서 관리 |
| **정확 알림** | 건강검진·예방접종 D-7 / D-1 / D-Day 3단계 + 스누즈 |
| **주변 병원 검색** | GPS 기반 반경 내 동물병원 검색 + 즐겨찾기 서버 저장 |
| **투약 기록** | 투약 일정 등록·알림·이력 관리 |
| **화장실 기록** | 배변 횟수·상태 기록 및 이력 관리 |
| **다중 고양이** | 고양이 여러 마리 전환 지원 |

---

## 기술 스택

### Spring Boot 백엔드

| 항목 | 내용 |
|------|------|
| 프레임워크 | Spring Boot 4.0.2 / Java 17 |
| 인증·보안 | JWT Token Rotation · Redis Blacklist · Spring Security (Stateless) · BCrypt |
| DB 접근 | Spring Data JPA (단순 CRUD) + JdbcTemplate (집계·UNION 쿼리 분리) |
| Redis | JWT Blacklist · Refresh Token 저장 (인증 전용) |
| AI 요약 캐시 | MySQL `ai_health_summary_cache` — 6시간 TTL (`DATE_SUB(NOW(), INTERVAL 6 HOUR)`) |
| AI | Gemini 2.5 Flash — Vision (이미지 분석) + Text (건강 요약) |
| 외부 API | Kakao Local 서버 프록시 · AWS S3 (Magic Byte 검증 · 10MB 제한) |
| 보안 | catId 소유권 검증 전 엔드포인트 · Rate Limiting (슬라이딩 윈도우) |
| 예외 처리 | `@RestControllerAdvice` 중앙 처리 — 6가지 예외 타입 HTTP 상태코드 매핑 |
| 스키마 관리 | `ApplicationRunner @Order(1/2)` — INFORMATION_SCHEMA 기반 무중단 마이그레이션 |
| 테스트 | JUnit 5 + Mockito — 단위 테스트 5개 파일 (JWT · 인증 · 소유권 검증 · 알람 공식) |
| 배포 | AWS EC2 · Docker · GitHub Actions CI/CD |

### Android 클라이언트

| 항목 | 내용 |
|------|------|
| 언어 / 빌드 | Java 11 · Gradle 8.x |
| 네트워크 | Retrofit 2 + OkHttp3 · AuthInterceptor · AuthAuthenticator |
| 알림 | AlarmManager `setExactAndAllowWhileIdle` · BootReceiver 재부팅 복구 |
| 이미지 | BitmapFactory `inSampleSize=4` · Camera/Gallery → Base64 인코딩 |
| 커스텀 View | SimpleLineChartView — Canvas API 직접 구현 (외부 라이브러리 없음) |

---

## 시스템 아키텍처

```
[ Android App ]
  SharedPreferences(auth) — accessToken / refreshToken / lastViewedCatId
  Retrofit2 + AuthInterceptor — 모든 요청에 Bearer 자동 첨부
  AuthAuthenticator — 401 수신 시 토큰 갱신 후 원 요청 자동 재시도
        │
        │  HTTPS + JWT
        ▼
[ Spring Boot — AWS EC2 ]
  /api/users/          인증 (login · signup · refresh · logout)
  /cats/{catId}/       케어 API 44개 — CatOwnershipService 소유권 검증
  /api/hospital/       Kakao Local 프록시 (API Key 서버 보관)
  /api/vomit/analyze   Gemini Vision — Base64 → 색상·형태·위험도 JSON
  /api/health/summary  Gemini Text — 7일 집계 → 건강 조언 (MySQL 6h 캐시)
  /api/s3/upload       Magic Byte 검증 → AWS S3 저장
        │
        ├── MySQL  (16개 테이블, 앱 기동 시 DDL 자동 실행)
        ├── Redis  (JWT Blacklist · Refresh Token — 인증 전용)
        └── AWS S3 (이미지 스토리지)
```

**아키텍처 결정 이유**

| 결정 | 이유 |
|------|------|
| JPA + JdbcTemplate 혼용 | 단순 CRUD → JPA, UNION ALL·집계 → JdbcTemplate — 목적에 맞게 분리 |
| Redis는 인증 전용 | AI 캐시는 6시간 TTL — MySQL로 충분. Redis는 JWT 블랙리스트·Refresh Token에 집중 |
| Kakao API 서버 프록시 | APK에 Key 하드코딩 시 리버스 엔지니어링 노출 위험 — 서버에서 프록시로 처리 |

---

## Spring Boot 레이어드 아키텍처

```
controller/     파라미터 검증 · 인가 위임 · HTTP 응답 구성
    │
service/        비즈니스 로직 · CatOwnershipService (크로스 커팅 인가 가드)
    │
repository/     JPA Repository (단순 CRUD) + JdbcTemplate (복잡 쿼리)
    │
domain/         JPA @Entity — User · Cat
dto/            Request / Response 분리
exception/      GlobalExceptionHandler (@RestControllerAdvice)
jwt/            JwtProvider · JwtFilter (OncePerRequestFilter)
filter/         RateLimitFilter (IP별 슬라이딩 윈도우)
client/         KakaoLocalClient (외부 API 분리)
config/         SecurityConfig · RedisConfig 등
```

**CatOwnershipService — 크로스 커팅 인가 가드**
모든 케어 서비스 메서드 진입부에서 호출. JWT `subject(userId)` ↔ `cat.userId` 비교 → 불일치 시 `SecurityException` 발생 → GlobalExceptionHandler가 403 반환.

```java
// 서비스 메서드 공통 진입부
catOwnershipService.validate(catId, userId);  // 403 or continue
```

---

## 인증 & 보안 설계

### JWT Token Rotation

```
로그인   → Access Token (30분) + Refresh Token (14일) → Redis 저장
요청     → JwtFilter: Redis blacklist 확인 → 토큰 최대 2048자 검사 (DoS 방어) → 인증 주입
401 수신 → AuthAuthenticator: responseCount ≥ 2 이면 null (무한 루프 차단)
           /api/users/refresh → 새 토큰으로 원 요청 재시도
로그아웃 → Access Token 잔여 TTL 계산 → Redis blacklist:<token> 등록
           Refresh Token Redis 삭제 → 양방향 무효화
```

**JwtProvider @PostConstruct 검증**
서버 시작 시 JWT secret 32바이트 미만이면 `IllegalStateException` — 짧은 키로 서비스가 기동되는 것 원천 차단.

### API 소유권 검증

44개 엔드포인트 전체에 적용. `GET /cats/1/weight` 요청 시 JWT의 userId와 cat.userId가 다르면 즉시 403.

### 외부 API 보안

| 포인트 | 방식 | 이유 |
|--------|------|------|
| Kakao Local API | 서버 프록시 | API Key APK 하드코딩 방지 |
| AWS S3 업로드 | Magic Byte(파일 앞 4바이트) 검증 + 10MB 제한 | 확장자 위조 차단 |

### Rate Limiting — IP별 슬라이딩 윈도우

```java
// ConcurrentHashMap<String, Deque<Long>> — IP별 요청 시각 덱 관리
// X-Forwarded-For 헤더에서 클라이언트 IP 추출
로그인 · 회원가입    → 10 req/min  (Brute-force 방지)
구토 분석 · AI 요약  →  5 req/min  (외부 API 과금 통제)
초과 시 HTTP 429 반환
```

---

## 예외 처리 전략

### GlobalExceptionHandler — @RestControllerAdvice

```java
MethodArgumentNotValidException  → 400  // @Valid 검증 실패 — 필드별 에러 메시지 수집
DateTimeParseException           → 400  // 날짜 포맷 오류
AuthException                    → 401  // 인증 실패 (커스텀 예외)
IllegalArgumentException         → 400  // 리소스 없음 (cat not found 등)
SecurityException                → 403  // 소유권 위반 (cat does not belong to user)
Exception (catch-all)            → 500  // 스택 트레이스 미노출, 한국어 메시지만 반환
```

스택 트레이스를 클라이언트에 노출하지 않아 서버 내부 구조 정보 유출을 차단한다.
예외 타입별로 HTTP 상태코드를 일관되게 매핑해 클라이언트가 원인을 구분할 수 있도록 설계했다.

---

## DB 설계 & 쿼리 최적화

### 16개 테이블 자동 생성

| 구분 | 담당 | 테이블 |
|------|------|--------|
| `SchemaInitializer` `@Order(1)` | 11개 | health_type · health_schedule · weight_record · calendar_memo · vomit_status · vomit_record · obesity_check_record · hospital · favorite_hospital · notification · ai_health_summary_cache |
| `CareTableInitializer` `@Order(2)` | 3개 | medication · litter_box_record · vet_visit |
| JPA `@Entity` | 2개 | users · cat |

**무중단 컬럼 마이그레이션** — `INFORMATION_SCHEMA` 조회 → 누락 컬럼만 `ALTER TABLE` 자동 실행 → 재배포 시 수동 마이그레이션 **0건**.

### 인덱스 전략

| 패턴 | 적용 대상 | 목적 |
|------|----------|------|
| `cat_id` 단일 인덱스 | 전 케어 테이블 (10개) | 고양이별 조회 — 메인 접근 패턴 최적화 |
| 날짜 컬럼 인덱스 | `recorded_at` · `next_date` · `memo_date` · `scheduled_at` 등 | 날짜 범위 쿼리 · ORDER BY 최적화 |
| 복합 인덱스 | `vomit_status(color, shape)` | 구토 분류 매핑 조회 최적화 |
| 복합 PK | `favorite_hospital(user_id, hospital_id)` | 중복 즐겨찾기 방지 + 조회 최적화 |

### 캘린더 UNION ALL 단일 쿼리

8개 데이터소스를 개별 쿼리로 처리하면 DB 왕복 8회, 애플리케이션에서 병합·정렬 필요.
UNION ALL로 DB 레벨에서 날짜순 정렬된 결과를 한 번에 반환.

```sql
SELECT 'MEMO'           AS type, memo_date           AS event_date FROM calendar_memo   WHERE cat_id = ?
UNION ALL
SELECT 'HEALTH_CHECKUP' AS type, last_date            AS event_date FROM health_schedule WHERE cat_id = ? AND last_date  IS NOT NULL
UNION ALL
SELECT 'HEALTH_VACCINE' AS type, next_date            AS event_date FROM health_schedule WHERE cat_id = ? AND next_date  IS NOT NULL
UNION ALL
SELECT 'WEIGHT'         AS type, CONVERT_TZ(recorded_at,...) AS event_date FROM weight_record WHERE cat_id = ?
UNION ALL
SELECT 'VOMIT'          AS type, DATE(created_at)     AS event_date FROM vomit_record   WHERE cat_id = ?
ORDER BY event_date
-- catId 바인드 파라미터 15개 (소스당 3개)
```

→ DB 왕복 **8→2회**, 응답 약 60% 단축

---

## AI 연동 & 비용 제어

### Gemini Vision — 토사물 분석 + 4단계 fallback

```
Android → inSampleSize=4 + JPEG quality 80 → Base64 → POST /api/vomit/analyze
서버    → Gemini 2.5 Flash Vision → (color, shape, severity) JSON 파싱
        → vomit_status 매핑 4단계 fallback:
            1. (color, shape) 정확 매칭
            2. 동일 color + NORMAL shape
            3. UNKNOWN color + 동일 shape
            4. UNKNOWN + NORMAL (최후 보루)
        → vomit_record 저장
        → ai_health_summary_cache 무효화 (DELETE)  ← 최신 데이터 반영 보장
```

원본 이미지 전송 시 타임아웃 + 응답 잘림 → EC2 로그 raw 응답 직접 확인 → `inSampleSize=4` (payload 4MB→200KB) + `maxOutputTokens=8192` 로 해결.

**AI 캐시 무효화 연계** — 구토 기록 저장 시 AI 건강 요약 캐시를 즉시 삭제한다. 다음 요약 요청 시 방금 저장된 구토 데이터가 반영된 최신 결과를 반환한다.

### Gemini Text — 건강 요약 + 비용 제어

```
요청 → ai_health_summary_cache WHERE cat_id=? AND generated_at >= DATE_SUB(NOW(), INTERVAL 6 HOUR)
캐시 히트 → 즉시 반환 (~5ms)
캐시 미스 → 7일 케어 데이터 집계 → Gemini Text API → INSERT ON DUPLICATE KEY UPDATE → 반환 (2~3초)
```

MySQL 캐시(6시간 TTL) + Rate Limit(5 req/min) 두 레이어로 외부 API 비용과 응답 속도를 동시에 제어.

---

## 테스트 코드

JUnit 5 + Mockito. `@ExtendWith(MockitoExtension.class)` — Spring Context 미적재, 빠른 단위 테스트.

| 파일 | 검증 내용 |
|------|----------|
| `AlarmRequestCodeTest` | `catId × 1,000,000 + medicationId × 10 + slot` 공식의 고양이·일정·슬롯 간 충돌 없음, 음수 없음 검증 |
| `UserServiceTest` | 회원가입 정상 동작 / 중복 ID 예외 / 로그인 성공(Redis `set()` 호출 검증) / 존재하지 않는 유저 / 비밀번호 불일치 |
| `JwtProviderTest` | Access·Refresh 토큰 생성·검증 / userId 추출 / 유효하지 않은 토큰 → false / 짧은 시크릿 → `IllegalStateException` |
| `CalendarServiceTest` | 존재하지 않는 cat → `IllegalArgumentException` / 다른 유저의 cat → `SecurityException` (소유권 검증) |
| `HowscatApplicationTests` | Spring Context 정상 로드 |

```java
// 테스트 메서드명 한국어로 작성 — 의도를 명확하게
@Test void 회원가입_정상동작() { ... }
@Test void 이미_있는_아이디로_가입하면_예외() { ... }
@Test void 다른_유저의_고양이면_SecurityException() { ... }
```

---

## Android 구현 포인트

### JWT 자동 갱신 — AuthAuthenticator

OkHttp `Authenticator` 구현. 401 수신 시 사용자 개입 없이 토큰 갱신 후 원 요청 재시도.

```java
if (responseCount(response) >= 2) return null;          // 무한 루프 차단
new Handler(Looper.getMainLooper()).post(() -> ...);     // UI 스레드 전환
volatile boolean redirecting;                            // 중복 리다이렉트 방지
```

### AlarmManager — Doze 대응 + 고양이별 격리

`setExactAndAllowWhileIdle` 1회 예약 + Receiver 자기 재예약. Request Code 공식으로 충돌 방지.
재부팅 시 `BootReceiver`(`BOOT_COMPLETED` + `LOCKED_BOOT_COMPLETED`)가 전체 알람 재등록.

### 이미지 OOM 해결

`inSampleSize=4` — 12MP 원본 ~48MB → ~3MB (16배 감소), payload 4MB → 200KB.

### SimpleLineChartView

Canvas API로 직접 구현. 외부 차트 라이브러리 없이 APK 크기 절감.

---

## 트러블슈팅

| # | 발견 | 원인 | 해결 | 결과 |
|---|------|------|------|------|
| 1 | **캘린더 응답 지연** | 8개 데이터소스 개별 쿼리 — DB 왕복 8회 | UNION ALL 5개 브랜치 + JdbcTemplate 파라미터 15개 | 왕복 **8→2회**, 응답 ~60% 단축 |
| 2 | **로그아웃 후 JWT 재사용** | Stateless JWT 서버 취소 불가 | 로그아웃 시 잔여 TTL → Redis `blacklist:<token>` | 토큰 즉시 무효화 |
| 3 | **재배포 시 컬럼 누락 중단** | 운영 DB 수동 적용 누락 | `INFORMATION_SCHEMA` 조회 → 누락 컬럼만 `ALTER` | 수동 마이그레이션 **0건** |
| 4 | **Gemini API 비용 급증** | 동일 요청마다 외부 API 호출 | MySQL 캐시 6h TTL + Rate Limit 5 req/min | 2~3초 → **~5ms** (캐시 히트) |
| 5 | **Gemini 응답 잘림·타임아웃** | 이미지 크기 + token 제한 동시 초과 | `inSampleSize=4` + `maxOutputTokens=8192` | 완전 해소 |
| 6 | **재부팅 후 알람 소실** | OS가 재부팅 시 전체 삭제 | `BootReceiver` → catId 목록으로 전체 재등록 | **100% 복구** |
| 7 | **로그아웃 후 이전 알람 재울림** | SharedPreferences만 초기화, PendingIntent 취소 누락 | 로그아웃 3경로 전체에서 `cancelAll()` 선행 | 완전 취소 |
| 8 | **`CalledFromWrongThreadException`** | AuthAuthenticator 백그라운드 스레드에서 `startActivity()` 호출 | `Handler(Looper.getMainLooper()).post(...)` 위임 | 크래시 없이 투명 전환 |
| 9 | **고해상도 이미지 OOM** | 12MP 원본 ~48MB 디코딩 | `inSampleSize=4` + JPEG quality 80 | **16배 감소** (48MB → 3MB) |
| 10 | **즐겨찾기 Jank** | 서버 응답 대기 중 UI 갱신 없음 | `setFavorited(after)` 즉시 선반영, 실패 시 롤백 | Jank **완전 제거** |

---

## 프로젝트 구조

**Spring Boot 백엔드** (`springserver/src/main/java/com/example/howscat/`)

| 패키지 | 역할 |
|--------|------|
| `controller/` | REST 진입점 — 파라미터 검증, 인가 위임 |
| `service/` | 비즈니스 로직, CatOwnershipService 크로스 커팅 |
| `repository/` | JPA Repository |
| `domain/` | JPA @Entity (User, Cat) |
| `dto/` | Request / Response DTO |
| `exception/` | GlobalExceptionHandler |
| `jwt/` | JwtProvider · JwtFilter |
| `filter/` | RateLimitFilter (슬라이딩 윈도우) |
| `client/` | KakaoLocalClient |
| `config/` | SecurityConfig · RedisConfig · SchemaInitializer |

**Android 클라이언트** (`app/src/main/java/com/example/howscat/`)

| 파일 | 역할 |
|------|------|
| `HomeFragment` | AI 요약 · 건강 일정 카드 · 케어 요약 복원 |
| `CalendarFragment` | 통합 이벤트 캘린더 · 건강 일정 등록 |
| `HospitalFragment` | Kakao Local 프록시 통한 주변 병원 검색 · 즐겨찾기 |
| `VomitAnalyzeActivity` | 토사물 AI 분석 화면 |
| `HealthScheduleAlarmScheduler` | D-7 / D-1 / D-Day 알람 등록 |
| `BootReceiver` | 재부팅 시 전체 알람 자동 복구 |
| `network/AuthAuthenticator` | 401 → 자동 토큰 갱신 |
| `network/ApiService` | Retrofit 인터페이스 (44개 엔드포인트) |

---

<div align="center">

**Spring Boot · AWS EC2 실배포 · Gemini 2.5 Flash · 1인 풀스택 개발**

</div>
