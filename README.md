<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="110" alt="Howscat" />

# Howscat

**AI 기반 고양이 건강 종합 관리 — Android + Spring Boot 풀스택 개인 프로젝트**

[![Android](https://img.shields.io/badge/Android-Java-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io)
[![Railway](https://img.shields.io/badge/Live_Deploy-Railway-0B0D0E?style=flat-square&logo=railway&logoColor=white)](https://railway.app)
[![Gemini](https://img.shields.io/badge/Gemini_2.0_Flash-Vision_·_Text-4285F4?style=flat-square&logo=google&logoColor=white)](https://deepmind.google/technologies/gemini/)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com)
[![minSdk](https://img.shields.io/badge/minSdk_24-Android_7.0+-green?style=flat-square)](#)

<br>

> 토사물 사진 한 장으로 AI 분석 · 체중·먹이·건강검진·투약·화장실 기록 통합 관리
> **Spring Boot 백엔드 직접 설계 · Railway 실배포 · Gemini Vision API 연동**
> 기획 → 설계 → 구현 → 배포까지 **1인 풀스택 개발**

---
<br>

<img src="https://github.com/user-attachments/assets/bea689e4-343b-48a9-8c4d-2e648eca52d5" width="18%" />
<img src="https://github.com/user-attachments/assets/1620327d-c003-4957-af9b-18b74a056837" width="18%" />
<img src="https://github.com/user-attachments/assets/7dfa1ab5-5985-4272-9277-0c3062902a08" width="18%" />
<img src="https://github.com/user-attachments/assets/9f21a426-b202-405f-86f4-9a88144216c3" width="18%" />
<img src="https://github.com/user-attachments/assets/828d095a-d441-4fcb-b47d-a78bc8cb66a4" width="18%" />


</div>

---

## 왜 만들었나

> 고양이를 키우면서 건강 기록을 관리할 앱이 없었다.
> 그냥 쓸 수 있는 앱보다 **직접 만들어보는 게 낫겠다고 생각했고**, 그게 이 프로젝트다.

단순 기록 앱이 아닌 **AI 분석 + 실알림 + 실배포**까지 끝낸 프로젝트를 목표로 삼았다.
백엔드 설계부터 Android UI, Railway 배포까지 전 과정을 혼자 다뤘다.

---

## 목차

1. [기술 스택](#기술-스택)
2. [아키텍처](#아키텍처)
3. [주요 기능 & 구현 포인트](#주요-기능--구현-포인트)
4. [로컬 실행](#로컬-실행)
5. [환경 변수](#환경-변수)
6. [REST API](#rest-api)
7. [프로젝트 구조](#프로젝트-구조)

---

## 기술 스택

### Android (Client)

| 분류 | 기술 |
|---|---|
| 언어 | Java 17 |
| 빌드 | Gradle 8.x |
| 네트워크 | Retrofit 2 + OkHttp3 · `AuthInterceptor` + `AuthAuthenticator` |
| 알림 | AlarmManager (`SCHEDULE_EXACT_ALARM`) · Android 12+ `canScheduleExactAlarms()` 분기 |
| 로컬 저장소 | SharedPreferences — `"auth"` / `"profile"` / `"health_schedule_alarm"` 파일 분리 |
| 이미지 | Glide · Camera/Gallery Intent → Base64 인코딩 |
| Min / Target SDK | 24 / 36 |

### Spring Boot (Server)

| 분류 | 기술 |
|---|---|
| 프레임워크 | Spring Boot 3 |
| 인증 | JWT (Access + Refresh Token) |
| DB 접근 | JdbcTemplate (ORM 미사용 — 쿼리 전 범위 직접 제어) |
| AI | Google Gemini 2.0 Flash — Vision (이미지 분석) + Text (건강 요약) |
| 스키마 자동화 | `ApplicationRunner` + `@Order(1)` — 배포 시 DDL 자동 실행 |
| 배포 | Railway (MySQL + Spring Boot 컨테이너) |

---

## 아키텍처

```
┌──────────────────────────────────────────────────────────────────┐
│                       📱 Android App                             │
│                                                                  │
│  SharedPreferences("auth")                                       │
│  ├── accessToken / refreshToken                                  │
│  ├── lastViewedCatId     ← 멀티캣 전환 핵심 상태                     │
│  └── lastViewedCatName                                           │
│                                                                  │
│  RetrofitClient                                                  │
│  └── OkHttpClient                                                │
│       ├── AuthInterceptor     모든 요청에 Bearer 토큰 자동 첨부       │
│       └── AuthAuthenticator   401 수신 시 자동 토큰 갱신 → 재시도     │
└──────────────────────────────┬───────────────────────────────────┘
                               │ HTTPS + JWT
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│              ⚙️ Spring Boot — Railway 배포                        │
│  https://howscatbackend-production.up.railway.app/               │
│                                                                  │
│  ┌─────────────────┐  ┌─────────────────────────────────────┐    │
│  │ Auth API        │  │ Cat / Care / Calendar (40+ 엔드포인트)│    │
│  │ /api/users/     │  │ /cats/{catId}/...                   │    │
│  │ login · signup  │  │ 모든 요청 catId 기반 소유권 검증        │     │
│  └─────────────────┘  └─────────────────────────────────────┘    │
│  ┌─────────────────┐  ┌─────────────────────────────────────┐    │
│  │ VomitAnalysis   │  │ AiHealthSummary                     │    │
│  │ Gemini Vision   │  │ Gemini Text                         │    │
│  │ Base64→JSON     │  │ 7일 케어 데이터 종합 → 1줄 조언         │    │
│  └─────────────────┘  └─────────────────────────────────────┘    │
│  ┌─────────────────┐  ┌─────────────────────────────────────┐    │
│  │ Hospital API    │  │ SchemaInitializer @Order(1)         │    │
│  │ Kakao Local 프록시│ │ 앱 시작 시 14개 테이블 자동 생성         │    │
│  └─────────────────┘  └─────────────────────────────────────┘    │
│                         JdbcTemplate                             │
└──────────────────────────────┬───────────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│                     🗄️ MySQL (Railway)                           │
│  users · cat · health_type · health_schedule                     │
│  weight_record · obesity_check_record                            │
│  vomit_status (7색×8형태=56행 시드) · vomit_record                 │
│  calendar_memo · medication · litter_box_record · vet_visit      │
│  hospital · favorite_hospital · notification                     │
└──────────────────────────────────────────────────────────────────┘
```

---

## 주요 기능 & 구현 포인트

### 1. Gemini Vision — 토사물 AI 분석

Android에서 이미지를 Base64로 인코딩해 서버로 전송하고, 서버가 Gemini Vision을 호출한 뒤 결과를 구조화해서 저장한다.

```java
// VomitAnalyzeActivity.java — 이미지 → Base64 → 서버
Bitmap bitmap = BitmapFactory.decodeStream(
    getContentResolver().openInputStream(selectedImageUri));
ByteArrayOutputStream baos = new ByteArrayOutputStream();
bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

api.analyzeVomit(catId, new VomitAnalysisRequest(base64, memo)).enqueue(...);
```

서버에서 Gemini에 보내는 프롬프트는 JSON 형식만 응답하도록 강제한다:

```java
// VomitAnalysisService.java
String prompt =
    "이 고양이 구토 사진을 분석해서 아래 JSON 형식으로만 답하세요. 다른 설명 없이 JSON만 출력하세요.\n" +
    "{\n" +
    "  \"color\": \"WHITE|YELLOW|GREEN|RED|BROWN|BLACK|UNKNOWN 중 하나\",\n" +
    "  \"hasFoam\": true 또는 false,\n" +
    "  \"hasFood\": true 또는 false,\n" +
    "  \"hasForeign\": true 또는 false,\n" +
    "  \"foreignObjectType\": \"없으면 null, 있으면 종류 (헤어볼·풀·비닐 등 10자 이내)\",\n" +
    "  \"aiGuide\": \"집사에게 한 줄 건강 조언 (30자 이내, 이모지 1개 포함)\"\n" +
    "}";
```

분석 결과는 `"노란색 · 거품 · 이물질(헤어볼)"` 형태로 구조화해 `vomit_record.ai_result`에 저장하고, 캘린더에도 자동 기록된다.

---

### 2. JWT 자동 갱신 플로우

모든 API 요청에 토큰을 첨부하고, 만료 시 자동으로 갱신해 사용자가 로그아웃되는 일이 없도록 구현했다.

```
모든 API 요청
  └── AuthInterceptor → Authorization: Bearer {accessToken}
        │
        ▼ 401 Unauthorized
  AuthAuthenticator 자동 실행
        │
        ├── POST /api/users/refresh
        │     body: { "refreshToken": "..." }
        │
        ├── 성공 → 새 accessToken 저장 → 원 요청 재시도 (투명 처리)
        └── 실패 → SharedPreferences 초기화 → LoginActivity 이동
```

```java
// AuthAuthenticator.java — 401 수신 시 자동 토큰 갱신
@Override
public Request authenticate(Route route, Response response) throws IOException {
    // 이미 refresh 시도한 경우 루프 방지
    if (response.request().header("X-Retry-After-Refresh") != null) return null;

    String refresh = prefs.getString("refreshToken", null);
    if (refresh == null) return null;

    // 동기식 refresh 호출 (인터셉터 내부이므로 runBlocking 패턴)
    retrofit2.Response<TokenResponse> r = api.refreshToken(
        new RefreshRequest(refresh)).execute();

    if (r.isSuccessful() && r.body() != null) {
        prefs.edit().putString("accessToken", r.body().getAccessToken()).apply();
        return response.request().newBuilder()
            .header("Authorization", "Bearer " + r.body().getAccessToken())
            .header("X-Retry-After-Refresh", "true")
            .build();
    }
    // refresh 실패 → 로그아웃 처리
    prefs.edit().clear().apply();
    return null;
}
```

---

### 3. AlarmManager — 고양이별 3단계 정확 알림

건강검진·예방접종을 D-7 / D-1 / D-Day 3단계로 알린다.
**핵심 설계 결정:** 고양이가 여러 마리일 경우 알람 ID가 충돌하지 않도록 고양이별 SharedPreferences 키를 분리했다.

```java
// HealthScheduleAlarmScheduler.java
private static final int[] OFFSETS_DAYS = {7, 1, 0};  // D-7, D-1, D-Day

for (int offset : OFFSETS_DAYS) {
    Calendar trigger = Calendar.getInstance();
    trigger.setTime(scheduleDate);
    trigger.set(Calendar.HOUR_OF_DAY, 9);
    trigger.add(Calendar.DAY_OF_MONTH, -offset);

    // 이미 지난 날짜면 다음 주기로 롤링 (최대 3회 시도)
    int guard = 0;
    while (trigger.getTimeInMillis() <= System.currentTimeMillis() && guard < 3) {
        trigger.add(Calendar.MONTH, cycleMonths);
        guard++;
    }

    // Android 12+ SCHEDULE_EXACT_ALARM 권한 분기
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.getTimeInMillis(), pi);
    } else {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.getTimeInMillis(), pi);
    }
}

// 고양이별 알람 ID 격리
private static String prefKey(long catId) {
    return "health_schedule_alarm_schedule_ids_cat_" + catId;
}
```

알림 수신 후 스누즈 버튼을 누르면 1시간 뒤 재알림:

```java
// HealthScheduleAlarmReceiver.java
private void scheduleSnooze(Context ctx, long scheduleId) {
    long triggerAt = System.currentTimeMillis() + 60L * 60L * 1000L;
    // 동일한 Android 12 분기 처리 적용
}
```

---

### 4. Railway DB 자동 스키마 초기화

Railway는 빈 DB를 제공한다. Spring Boot가 올라올 때 테이블이 없으면 모든 API가 500을 낸다.
이를 해결하기 위해 `ApplicationRunner`로 앱 시작 시 DDL을 자동 실행한다.

```java
// SchemaInitializer.java — @Component, @Order(1), ApplicationRunner
@Override
public void run(ApplicationArguments args) {
    // 14개 테이블 CREATE TABLE IF NOT EXISTS (멱등성 보장)
    createHealthTypeTable();
    createVomitRecordTable();
    // ...

    // 기존 배포된 테이블에 신규 컬럼이 없으면 ALTER 추가
    // INFORMATION_SCHEMA 조회로 컬럼 존재 여부 확인 후 실행
    for (String[] col : new String[][]{
        {"memo",       "ALTER TABLE vomit_record ADD COLUMN memo TEXT"},
        {"image_path", "ALTER TABLE vomit_record ADD COLUMN image_path VARCHAR(500)"},
    }) {
        Integer cnt = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vomit_record' AND COLUMN_NAME = ?",
            Integer.class, col[0]);
        if (cnt == null || cnt == 0) jdbcTemplate.execute(col[1]);
    }

    // vomit_status 시드: 7색상 × 8형태 = 56행, INSERT IGNORE + WHERE NOT EXISTS
    seedVomitStatus();
    seedHealthType();
}
```

재배포 시 수동 마이그레이션 작업 **0건**.

---

### 5. 캘린더 UNION ALL 통합 쿼리

5개 테이블(메모·건강일정·체중·구토·케어)의 이벤트를 날짜순으로 단일 API로 반환한다.
`COALESCE` 패턴으로 NULL 안전 처리, 테이블 부재 시 Java 레이어에서 try-catch로 빈 리스트를 반환한다.

```sql
SELECT * FROM (
  -- 메모
  SELECT cm.calendar_memo_id AS event_id, 'MEMO' AS event_type, ...
  FROM calendar_memo cm
  WHERE cm.cat_id = ? AND DATE(cm.memo_date) BETWEEN ? AND ?
  UNION ALL
  -- 건강 일정
  SELECT hs.health_schedule_id AS event_id, 'HEALTH_CHECKUP' AS event_type, ...
  FROM health_schedule hs
  WHERE hs.cat_id = ? AND hs.next_date BETWEEN ? AND ?
  UNION ALL
  -- 체중
  SELECT wr.weight_record_id AS event_id, 'WEIGHT' AS event_type, ...
  FROM weight_record wr WHERE wr.cat_id = ?
  UNION ALL
  -- 구토
  SELECT vr.vomit_record_id AS event_id, 'VOMIT' AS event_type,
         COALESCE(vr.ai_result, CONCAT('위험도 ', vs.severity_level)) AS subtitle, ...
  FROM vomit_record vr
  LEFT JOIN vomit_status vs ON vs.vomit_status_id = vr.vomit_status_id
  WHERE vr.cat_id = ?
) t ORDER BY t.event_date ASC, t.event_time ASC
```

---

## 로컬 실행

### 사전 요구사항

| | 버전 |
|---|---|
| Android Studio | Giraffe 이상 |
| JDK | 17 |
| Android 실기기 | API 24 이상 |

> **에뮬레이터 불가** — 카메라·GPS·AlarmManager 기능은 실기기 필수

### 1. 저장소 클론

```bash
git clone https://github.com/YOUR_ID/howscat.git
cd howscat
```

### 2. `local.properties` 작성

```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk

# 배포 서버 (끝에 / 필수)
api.base.url=https://howscatbackend-production.up.railway.app/
api.base.url.release=https://howscatbackend-production.up.railway.app/

# 로컬 서버로 개발 시
# api.base.url=http://10.0.2.2:8080/       ← 에뮬레이터
# api.base.url=http://192.168.X.X:8080/    ← 실기기 (같은 Wi-Fi)

# https://developers.kakao.com → 내 애플리케이션 → REST API 키
kakao.rest.api.key=YOUR_KAKAO_KEY
```

### 3. 빌드 & 실행

```
File → Sync Project with Gradle Files
Build → Make Project  (Ctrl+F9)
Run → Run 'app'       (Shift+F10)
```

### 백엔드 로컬 실행

```bash
cd springserver
# application.properties 또는 환경 변수 설정 후
./gradlew bootRun
```

---

## 환경 변수

### Android (`local.properties` → `BuildConfig`)

```groovy
// app/build.gradle
buildConfigField "String", "API_BASE_URL",       "\"${apiBaseUrl}\""
buildConfigField "String", "KAKAO_REST_API_KEY",  "\"${kakaoRestKey}\""
```

### Spring Boot (Railway Variables 탭)

| 변수 | 설명 |
|---|---|
| `DATABASE_URL` | Railway MySQL JDBC URL |
| `DATABASE_USERNAME` | DB 사용자명 |
| `DATABASE_PASSWORD` | DB 비밀번호 |
| `GEMINI_API_KEY` | Google AI Studio API 키 |
| `JWT_SECRET` | JWT 서명 키 (256bit 이상 권장) |

---

## REST API

<details>
<summary>전체 40개 엔드포인트 펼쳐보기</summary>

### 인증

| 메서드 | 엔드포인트 | 설명 |
|---|---|---|
| POST | `/api/users/login` | 로그인 → JWT 발급 |
| POST | `/api/users/signup` | 회원가입 |
| POST | `/api/users/logout` | 로그아웃 |
| POST | `/api/users/refresh` | Access Token 갱신 |

**POST `/api/users/login`**
```json
// Request
{ "email": "user@example.com", "password": "pass1234!" }

// Response 200
{
  "accessToken":  "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### 고양이

| 메서드 | 엔드포인트 | 설명 |
|---|---|---|
| POST | `/cats` | 고양이 등록 |
| GET | `/cats/{id}` | 고양이 정보 조회 |
| GET | `/cats/user` | 내 고양이 목록 |
| POST | `/cats/select/{catId}` | 활성 고양이 전환 |

### AI

| 메서드 | 엔드포인트 | 설명 |
|---|---|---|
| POST | `/cats/{catId}/vomit` | 토사물 Gemini Vision 분석 |
| DELETE | `/cats/{catId}/vomit/{vomitId}` | 구토 기록 삭제 |
| GET | `/cats/{catId}/ai-summary` | Gemini Text 건강 요약 |

**POST `/cats/{catId}/vomit`**
```json
// Request
{
  "imageBase64": "/9j/4AAQSkZJRgAB...",
  "memo": "아침에 한 번 토했어요"
}

// Response 200
{
  "vomitStatusId": 9,
  "severityLevel": "MEDIUM",
  "guideText": "노란 담즙 구토는 공복 상태일 때 자주 발생합니다.",
  "riskLevel": "MEDIUM",
  "urgent": false,
  "aiGuide": "🟡 공복 시간 줄이고 소량씩 자주 주세요."
}
```

**GET `/cats/{catId}/ai-summary`**
```json
// Response 200
{
  "summary": "🐾 최근 7일 구토 2회, 체중은 전주 대비 +0.1kg로 안정적이에요."
}
```

### 케어 & 기록

| 메서드 | 엔드포인트 | 설명 |
|---|---|---|
| POST | `/cats/{catId}/care-weight` | 체중·사료량 계산 |
| POST | `/cats/{catId}/obesity-check` | 비만도 검사 |
| GET | `/cats/{catId}/weight-history` | 체중 이력 |
| GET | `/cats/{catId}/obesity-history` | 비만도 이력 |
| GET | `/cats/{catId}/health-schedules` | 건강 일정 목록 |
| PUT | `/cats/{catId}/health-schedules/{id}` | 일정 수정 |
| GET/POST | `/cats/{catId}/medications` | 투약 목록·등록 |
| PUT/DELETE | `/cats/{catId}/medications/{id}` | 투약 수정·삭제 |
| GET/POST | `/cats/{catId}/litter-records` | 화장실 기록 |
| PUT/DELETE | `/cats/{catId}/litter-records/{id}` | 수정·삭제 |
| GET/POST | `/cats/{catId}/vet-visits` | 진료 기록 |
| PUT/DELETE | `/cats/{catId}/vet-visits/{id}` | 수정·삭제 |

### 캘린더

| 메서드 | 엔드포인트 | 설명 |
|---|---|---|
| GET | `/cats/{catId}/calendar?from=&to=` | 기간별 통합 이벤트 |
| POST | `/cats/{catId}/calendar-memos` | 메모 작성 |
| PUT | `/cats/{catId}/calendar-memos/{memoId}` | 메모 수정 |
| DELETE | `/cats/{catId}/calendar-memos/{memoId}` | 메모 삭제 |
| DELETE | `/cats/{catId}/calendar-memos/all` | 전체 삭제 |

**GET `/cats/1/calendar?from=2025-04-01&to=2025-04-30`**
```json
[
  {
    "eventId": 12,
    "eventType": "VOMIT",
    "date": "2025-04-03",
    "time": "08:22",
    "title": "토 분석 기록",
    "subtitle": "노란색 · 거품 · 위험도 MEDIUM",
    "imagePath": "/uploads/vomit/cat1_20250403.jpg",
    "riskLevel": "MEDIUM"
  },
  {
    "eventId": 7,
    "eventType": "WEIGHT",
    "date": "2025-04-05",
    "time": "19:11",
    "title": "몸무게 기록",
    "subtitle": "4.30kg"
  }
]
```

### 병원

| 메서드 | 엔드포인트 | 설명 |
|---|---|---|
| GET | `/hospitals/nearby?lat=&lng=&radius=&only24h=` | Kakao Local API 병원 검색 |
| GET | `/hospitals/favorites` | 즐겨찾기 목록 |
| POST | `/hospitals/{id}/favorite` | 즐겨찾기 등록 |
| DELETE | `/hospitals/{id}/favorite` | 즐겨찾기 해제 |

</details>

---

## 프로젝트 구조

```
howscat/                               ← Android 앱
├── local.properties                   # API 키 (git 제외)
├── app/build.gradle                   # BuildConfig 키 주입
└── app/src/main/
    ├── AndroidManifest.xml            # SCHEDULE_EXACT_ALARM 권한
    └── java/com/example/howscat/
        ├── HomeActivity.java          # 멀티캣 전환 다이얼로그
        ├── HomeFragment.java          # AI 요약 · 건강 일정 카드
        ├── CatFragment.java           # 체중·비만도·사료 계산
        ├── CalendarFragment.java      # 통합 이벤트 캘린더
        ├── HospitalFragment.java      # Kakao 반경 병원 검색
        ├── VomitAnalyzeActivity.java  # 토사물 AI 분석
        ├── CareResultPrefs.java       # 케어 요약 고양이별 저장
        ├── CatRegisterActivity.java   # 고양이 등록
        ├── HealthScheduleAlarmScheduler.java  # D-7/D-1/D-Day 알람
        ├── HealthScheduleAlarmReceiver.java   # 알람 수신 + 스누즈
        ├── MedicationAlarmScheduler.java      # 투약 알람
        ├── FeedingAlarmScheduler.java         # 급식 알람
        ├── network/
        │   ├── ApiService.java        # Retrofit 인터페이스 (40 endpoints)
        │   ├── RetrofitClient.java    # OkHttp 클라이언트 조립
        │   ├── AuthInterceptor.java   # Bearer 토큰 자동 첨부
        │   └── AuthAuthenticator.java # 401 → 자동 토큰 갱신
        └── dto/                       # 요청/응답 모델 28종

springserver/                          ← Spring Boot 백엔드
└── src/main/java/com/example/howscat/
    ├── config/
    │   ├── SchemaInitializer.java     # 앱 시작 시 DDL 자동 실행
    │   └── SecurityConfig.java        # JWT 필터 체인
    ├── service/
    │   ├── AiHealthSummaryService.java  # Gemini Text 건강 요약
    │   ├── VomitAnalysisService.java    # Gemini Vision 구토 분석
    │   ├── CalendarService.java         # UNION ALL 통합 쿼리
    │   ├── WeightService.java
    │   └── HospitalService.java         # Kakao Local API 프록시
    └── controller/                      # 40개 REST 엔드포인트
```

---

<div align="center">

**Spring Boot + Railway 실배포 · Gemini Vision AI · 1인 풀스택 개발**

</div>
