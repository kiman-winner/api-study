# 💳 간편결제 API (Payment API)

PG사 연동 방식의 간편결제 흐름을 구현한 백엔드 API 프로젝트입니다.
결제 요청 → PG사 승인 → 취소 흐름을 RESTful API로 구현하였으며, Redis 캐싱, 중복 결제 방지, 페이징 조회를 포함합니다.

---

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.11 |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL 8.0 |
| Cache | Redis 7 |
| Build | Gradle |
| Test | JUnit5 / Mockito |
| API Docs | Swagger (springdoc-openapi) |
| Container | Docker |

---

## 📌 주요 기능

- **결제 요청** — Mock PG사 승인 처리 및 트랜잭션 ID 발급
- **결제 취소** — 승인된 결제에 한해 취소 처리
- **결제 단건 조회** — Redis 캐싱 적용 (TTL 10분)
- **결제 목록 조회** — 페이징 + 상태 필터링
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
│   └── RedisConfig.java              # Redis 캐시 설정
├── controller
│   └── PaymentController.java        # REST API 엔드포인트
├── service
│   └── PaymentService.java           # 비즈니스 로직
├── repository
│   └── PaymentRepository.java        # JPA Repository
├── entity
│   └── Payment.java                  # DB 테이블 매핑
├── dto
│   ├── PaymentRequest.java           # 요청 DTO
│   ├── PaymentResponse.java          # 응답 DTO
│   └── PaymentPageResponse.java      # 페이징 응답 DTO
├── enums
│   └── PaymentStatus.java            # 결제 상태 Enum
└── exception
    └── GlobalExceptionHandler.java   # 전역 예외 처리
```

---

## 🔗 API 명세

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/v1/payments` | 결제 요청 |
| POST | `/api/v1/payments/{id}/cancel` | 결제 취소 |
| GET | `/api/v1/payments/{id}` | 결제 단건 조회 (캐싱) |
| GET | `/api/v1/payments` | 결제 목록 조회 (페이징) |

### 결제 요청 예시

**Request**
```json
POST /api/v1/payments
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
  "createdAt": "2026-03-18T10:00:00",
  "updatedAt": "2026-03-18T10:00:00"
}
```

### 에러 응답 포맷

```json
{
  "code": "CONFLICT",
  "message": "이미 처리된 주문입니다: ORDER-001",
  "detail": null,
  "timestamp": "2026-03-18T10:00:00"
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

### 1. Redis 실행 (Docker)
```bash
docker run -d --name payment-redis -p 6379:6379 redis:7
```

### 2. MySQL 데이터베이스 생성
```sql
CREATE DATABASE payment_db;
```

### 3. application.yaml 설정
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/payment_db
    username: root
    password: {비밀번호}
```

### 4. 실행
```bash
./gradlew bootRun
```

### 5. Swagger UI 접속
```
http://localhost:8080/swagger-ui/index.html
```

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
