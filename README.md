# 💳 간편결제 API (Payment API)

PG사 연동 방식의 간편결제 흐름을 구현한 백엔드 API 프로젝트입니다.
JWT 인증, Redis 캐싱, 페이징 조회, Docker 컨테이너화를 포함합니다.

---

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.11 |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL 8.0 |
| Cache | Redis 7 |
| Security | Spring Security + JWT |
| Query | QueryDSL |
| Build | Gradle |
| Test | JUnit5 / Mockito |
| API Docs | Swagger (springdoc-openapi) |
| Container | Docker |

---

## 📌 주요 기능

- **회원가입 / 로그인** — BCrypt 암호화, JWT 토큰 발급
- **JWT 인증** — 모든 결제 API 토큰 인증 필요
- **결제 요청** — Mock PG사 승인 처리 및 트랜잭션 ID 발급
- **결제 취소** — 승인된 결제에 한해 취소 처리
- **결제 단건 조회** — Redis 캐싱 적용 (TTL 10분)
- **결제 목록 조회** — 페이징 + 상태 필터링
- **결제 검색** — QueryDSL 동적 쿼리 (상태, 금액 범위, 날짜 범위, 고객명 검색)
- **중복 결제 방지** — orderId unique 제약으로 이중 결제 차단
- **결제 상태 관리** — PENDING → APPROVED → CANCELLED / FAILED
- **전역 예외 처리** — 일관된 에러 응답 포맷

---

## 💡 결제 상태 흐름

```
PENDING (결제 요청)
    ↓ PG사 승인 성공
APPROVED (결제 승인)
    ↓ 취소 요청
CANCELLED (결제 취소)

PENDING
    ↓ PG사 승인 실패
FAILED (결제 실패)
```

---

## 📂 프로젝트 구조

```
src/main/java/com/payment/payment_api
├── config
│   ├── QueryDslConfig.java           # QueryDSL 설정
│   ├── RedisConfig.java              # Redis 캐시 설정
│   ├── SecurityConfig.java           # Spring Security 설정
│   └── SwaggerConfig.java            # Swagger JWT 설정
├── security
│   ├── JwtTokenProvider.java         # JWT 생성/검증
│   └── JwtAuthenticationFilter.java  # 요청마다 JWT 검증 필터
├── controller
│   ├── AuthController.java           # 회원가입/로그인 API
│   └── PaymentController.java        # 결제 API
├── service
│   ├── AuthService.java              # 인증 비즈니스 로직
│   └── PaymentService.java           # 결제 비즈니스 로직
├── repository
│   ├── MemberRepository.java         # 회원 JPA Repository
│   ├── PaymentRepository.java        # 결제 JPA Repository
│   └── PaymentQueryRepository.java   # 결제 QueryDSL Repository
├── entity
│   ├── Member.java                   # 회원 테이블 매핑
│   └── Payment.java                  # 결제 테이블 매핑
├── dto
│   ├── LoginRequest.java             # 로그인/회원가입 요청
│   ├── TokenResponse.java            # JWT 토큰 응답
│   ├── PaymentRequest.java           # 결제 요청 DTO
│   ├── PaymentResponse.java          # 결제 응답 DTO
│   ├── PaymentPageResponse.java      # 페이징 응답 DTO
│   └── PaymentSearchCondition.java   # 결제 검색 조건 DTO
├── enums
│   └── PaymentStatus.java            # 결제 상태 Enum
└── exception
    └── GlobalExceptionHandler.java   # 전역 예외 처리
```

---

## 🔗 API 명세

### 인증 API (토큰 불필요)

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/v1/auth/sign-up` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인 (JWT 발급) |

### 결제 API (JWT 토큰 필요)

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/v1/payments` | 결제 요청 |
| POST | `/api/v1/payments/{id}/cancel` | 결제 취소 |
| GET | `/api/v1/payments/{id}` | 결제 단건 조회 (캐싱) |
| GET | `/api/v1/payments` | 결제 목록 조회 (페이징) |
| GET | `/api/v1/payments/search` | 결제 검색 (QueryDSL) |

### 로그인 예시

**Request**
```json
POST /api/v1/auth/login
{
  "username": "user1",
  "password": "1234"
}
```

**Response**
```json
{
  "accessToken": "eyJhbGci...",
  "tokenType": "Bearer"
}
```

### 결제 요청 예시

**Request**
```json
POST /api/v1/payments
Authorization: Bearer eyJhbGci...
{
  "orderId": "ORDER-001",
  "customerName": "홍길동",
  "amount": 10000
}
```

**Response**
```json
{
  "id": 1,
  "orderId": "ORDER-001",
  "customerName": "홍길동",
  "amount": 10000,
  "status": "APPROVED",
  "pgTransactionId": "PG-A1B2C3D4",
  "createdAt": "2026-03-19T10:00:00",
  "updatedAt": "2026-03-19T10:00:00"
}
```

### 에러 응답 포맷

```json
{
  "code": "CONFLICT",
  "message": "이미 처리된 주문입니다: ORDER-001",
  "detail": null,
  "timestamp": "2026-03-19T10:00:00"
}
```

### 목록 조회 예시

```
GET /api/v1/payments                          → 전체 목록 (기본 0페이지, 10개)
GET /api/v1/payments?page=1&size=5            → 2번째 페이지, 5개씩
GET /api/v1/payments?status=APPROVED          → APPROVED 상태 필터링
GET /api/v1/payments?status=APPROVED&page=0   → 필터 + 페이징
```

---

## ⚙️ 실행 방법

### 방법 1. Docker Compose (권장)

```bash
# .env 파일 생성
cp .env.example .env

# 전체 실행 (MySQL + Redis + App)
docker-compose up --build

# 백그라운드 실행
docker-compose up -d --build

# 종료
docker-compose down
```

### 방법 2. 로컬 직접 실행

**1. Redis 실행**
```bash
docker run -d --name payment-redis -p 6379:6379 redis:7
```

**2. MySQL 데이터베이스 생성**
```sql
CREATE DATABASE payment_db;
```

**3. .env 파일 생성**
```
DB_USERNAME=root
DB_PASSWORD=비밀번호
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=32자리_이상_시크릿키
```

**4. 실행**
```bash
./gradlew bootRun
```

### Swagger UI 접속
```
http://localhost:8080/swagger-ui/index.html
```

> Swagger 우측 상단 **Authorize 🔒** 버튼으로 JWT 토큰 입력 후 테스트 가능

---

## ✅ 테스트 실행

```bash
./gradlew test
```

Mockito 기반 단위 테스트로 DB 없이 비즈니스 로직 검증
- 결제 요청 성공
- 중복 결제 방지
- 결제 취소 성공/실패
- 결제 조회 성공/실패
