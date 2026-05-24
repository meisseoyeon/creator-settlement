# 크리에이터 정산 API (BE 과제 B)

크리에이터의 강의 판매·취소(환불) 내역을 관리하고, 결제/취소 일시 기준으로 월별 정산 금액을 계산·확정하는 API.

## 기술 스택

- Java 21, Spring Boot 3.4.5
- Spring Data JPA (Hibernate)
- PostgreSQL 16 (docker-compose 제공)
- springdoc-openapi (Swagger UI)
- JUnit 5 / Spring MockMvc

## 핵심 도메인 규칙

- **정산 기간 기준**: 판매는 결제 일시(`paidAt`), 취소는 취소 일시(`canceledAt`) 기준으로 각각 해당 월에 귀속.
- **월/기간 경계**: KST 기준 **반열린 구간** `[시작 00:00, 종료+1일 00:00)` 으로 처리해 경계 중복을 방지. (월별은 `[해당월 1일, 다음달 1일)`)
- **순 판매** = 총 판매 − 환불.
- **수수료** = `max(0, 순 판매) × 수수료율`, 소수점은 **버림(RoundingMode.DOWN)**. 순 판매가 음수면 수수료 0.
- **정산 예정 금액(payout)** = 순 판매 − 수수료. (환불만 있는 달은 음수가 될 수 있음)
- **수수료율**은 고정 20%이며 `application.yaml`의 `settlement.commission-rate`로 분리해 변경 가능성을 반영(`CommissionPolicy`).
- **정산 상태**: `PENDING → CONFIRMED → PAID` 단방향 전이. 동일 (크리에이터, 월) 정산은 중복 생성 불가(409).

## 실행 방법

```bash
# 1) PostgreSQL 기동
docker compose up -d

# 2) 애플리케이션 실행 (부팅 시 data.sql 시드 자동 삽입)
./gradlew bootRun
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI 문서: http://localhost:8080/v3/api-docs
- 스키마는 `ddl-auto: create-drop`(부팅마다 재생성) + `data.sql` 시드로 구성됩니다.

## 테스트 실행 방법

```bash
docker compose up -d      # 테스트도 동일 PostgreSQL 사용
./gradlew test
```

- 통합 테스트(`@SpringBootTest` + `@AutoConfigureMockMvc`)는 `@Transactional`로 각 테스트 종료 시 롤백하여 시드 데이터를 보존합니다.
- 단위 테스트(`SettlementCalculatorTest`, `MonthRangeTest`)는 DB 없이 순수 로직만 검증합니다.

---

## API 목록 및 예시

기본 URL: `http://localhost:8080`

### 판매/취소

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | `/api/sales` | 판매 내역 등록 |
| POST | `/api/sales/{saleId}/cancels` | 취소(환불) 내역 등록 |
| GET | `/api/creators/{creatorId}/sales?from&to` | 크리에이터별 판매 목록 (기간 필터 선택) |

### 정산 (크리에이터)

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/creators/{creatorId}/settlements?month=2025-03` | 월별 정산 조회 (확정된 달은 스냅샷, 미확정 달은 즉시 계산) |
| POST | `/api/creators/{creatorId}/settlements` | 월별 정산 스냅샷 생성 (PENDING) |
| POST | `/api/settlements/{id}/confirm` | 정산 확정 (CONFIRMED) |
| POST | `/api/settlements/{id}/pay` | 정산 지급 (PAID) |
| GET | `/api/settlements/{id}` | 정산 스냅샷 단건 조회 |

### 정산 집계 (운영자)

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/admin/settlements?from&to` | 기간 내 전체 크리에이터 정산 집계 + 합계 |

### 예시

**판매 등록**

```bash
curl -X POST http://localhost:8080/api/sales \
  -H 'Content-Type: application/json' \
  -d '{"id":"sale-100","courseId":"course-1","studentId":"student-1","amount":50000,"paidAt":"2025-04-01T10:00:00+09:00"}'
# 201 Created, Location: /api/sales/sale-100
```

**취소 등록** (취소 일시 ≥ 결제 일시, 환불 누적 ≤ 원결제)

```bash
curl -X POST http://localhost:8080/api/sales/sale-4/cancels \
  -H 'Content-Type: application/json' \
  -d '{"id":"cancel-100","refundAmount":30000,"canceledAt":"2025-03-28T15:00:00+09:00"}'
```

**크리에이터 월별 정산 조회**

```bash
curl 'http://localhost:8080/api/creators/creator-1/settlements?month=2025-03'
```

```json
{
  "creatorId": "creator-1",
  "creatorName": "김강사",
  "month": "2025-03",
  "totalSalesAmount": 260000,
  "totalRefundAmount": 110000,
  "netSalesAmount": 150000,
  "commissionAmount": 30000,
  "payoutAmount": 120000,
  "salesCount": 4,
  "cancelCount": 2,
  "commissionRate": 0.20
}
```

**정산 확정 흐름**

```bash
# 생성 (PENDING)
curl -X POST http://localhost:8080/api/creators/creator-1/settlements \
  -H 'Content-Type: application/json' -d '{"month":"2025-03"}'
# → { "id": 1, "status": "PENDING", ... }

curl -X POST http://localhost:8080/api/settlements/1/confirm   # → CONFIRMED
curl -X POST http://localhost:8080/api/settlements/1/pay       # → PAID
```

**운영자 기간 집계**

```bash
curl 'http://localhost:8080/api/admin/settlements?from=2025-03-01&to=2025-03-31'
```

```json
{
  "from": "2025-03-01",
  "to": "2025-03-31",
  "creators": [
    { "creatorId": "creator-1", "creatorName": "김강사", "payoutAmount": 120000, "netSalesAmount": 150000, "commissionAmount": 30000, "salesCount": 4, "cancelCount": 2, "totalSalesAmount": 260000, "totalRefundAmount": 110000 },
    { "creatorId": "creator-2", "creatorName": "이강사", "payoutAmount": 48000,  "netSalesAmount": 60000,  "commissionAmount": 12000, "salesCount": 1, "cancelCount": 0, "totalSalesAmount": 60000,  "totalRefundAmount": 0 }
  ],
  "total": { "creatorCount": 2, "payoutAmount": 168000, "netSalesAmount": 210000, "commissionAmount": 42000, "totalSalesAmount": 320000, "totalRefundAmount": 110000, "salesCount": 5, "cancelCount": 2 }
}
```

### 에러 응답

`BusinessException` 및 검증 실패는 다음 형태로 통일됩니다.

```json
{ "status": 404, "message": "존재하지 않는 크리에이터입니다: creator-x", "timestamp": "2025-03-01T10:00:00+09:00" }
```

| 상황 | 상태 코드 |
| --- | --- |
| 필수 필드 누락/형식 오류, 잘못된 연월·날짜, from > to | 400 |
| 존재하지 않는 크리에이터/강의/판매/정산 | 404 |
| 중복 ID, 환불 누적 초과, 중복 정산, 잘못된 상태 전이 | 409 |

---

## 데이터 모델 설명

```
Creator 1 ── N Course 1 ── N SaleRecord 1 ── N CancelRecord
Creator 1 ── N Settlement
Admin / Student (참조용 마스터)
```

| 엔티티 | 주요 컬럼 | 설명 |
| --- | --- | --- |
| `Creator` | id, name | 크리에이터(강사) |
| `Course` | id, creator_id, title | 강의. 크리에이터에 속함 |
| `SaleRecord` | id, course_id, student_id, amount, paid_at | 판매 내역. ID는 클라이언트 지정 |
| `CancelRecord` | id, sale_id, refund_amount, canceled_at | 취소(환불). 원본 판매 참조, 부분/다회 환불 허용 |
| `Settlement` | id, creator_id, settlement_month, (금액 스냅샷들), commission_rate, status, confirmed_at, paid_at | 확정 정산 스냅샷. (creator_id, settlement_month) 유니크 |
| `Admin`, `Student` | id, name | 운영자/수강생 마스터 |

설계 메모:
- `studentId`는 판매 시점 식별자로만 쓰여 `SaleRecord`에 문자열 컬럼으로 둠(강한 FK 미적용).
- `Settlement`은 확정 시점의 금액을 **스냅샷**으로 저장 → 이후 환불이 추가돼도 확정된 정산 값은 불변.
- **월별 조회 성능**: 확정(CONFIRMED)·지급(PAID)된 달은 재계산 없이 스냅샷을 반환(read-through), 미확정 달만 실시간 계산. 조회 패턴에 맞춰 복합 인덱스(`sale_record(course_id, paid_at)`, `cancel_record(sale_id, canceled_at)`) 적용.
- 조회 성능을 위해 `paid_at`, `canceled_at`, FK 컬럼에 인덱스, 정산은 status 인덱스 + (creator, month) 유니크 제약.
- 정산 계산(`SettlementCalculator`)은 순수 함수로 분리해 단위 테스트 용이.

---

## 검증 시나리오

`data.sql` 시드(샘플 데이터) 기준으로 다음을 자동 테스트(`SettlementApiTest`, `SettlementCalculatorTest`)로 검증합니다.

| 시나리오 | 확인 포인트 |
| --- | --- |
| creator-1 2025-03 정산 | 총 260,000 / 환불 110,000 / 순 150,000 / 수수료 30,000 / 정산 120,000 |
| 부분 환불(sale-4, cancel-2) | 환불 30,000 < 원결제 80,000 → 차액만 순 판매 반영 |
| 월 경계 취소(sale-5/cancel-3) | 1월 판매·2월 취소가 각각 다른 월 정산에 귀속 (creator-2 2월 순 −60,000) |
| 빈 월(creator-3 2025-03) | 판매/취소 없음 → 모든 금액 0 |
| 운영자 집계 2025-03 | creator-1 120,000 + creator-2 48,000 = 168,000 |
| 정산 라이프사이클 | PENDING → CONFIRMED → PAID 전이, confirmedAt/paidAt 기록 |

### 추가 검증 시나리오 (직접 등록, 가산점)

기본 4개 시나리오 외에, 정산 도메인에서 실수가 잦은 지점을 골라 다음 케이스를 직접 추가했습니다. (테스트: `SettlementEdgeCaseTest`, `SettlementApiTest`, `SaleControllerTest`, `CancelConcurrencyTest`)

| 추가 케이스 | 기대 결과 | 왜 추가했나 |
|---|---|---|
| **동일 판매 다회 부분 환불 + 누적 경계** | sale-1(50,000)에 20,000+30,000 환불은 허용(누적=원결제), 1원 더하면 400. 정산 환불 160,000·정산 80,000·취소 4건 반영 | "다회/부분 환불 허용" 요구와 "누적 ≤ 원결제" 불변식의 **경계값(=)**을 동시에 검증 |
| **동시 환불 경합(race)** | 같은 판매에 40,000 환불 2건 동시 요청 → 1건만 성공, 누적 ≤ 원결제 | check-then-act 동시성 버그를 **비관적 락**으로 방지하는지 검증 |
| **미래 월 조회** | creator-1 `2099-12` → 모든 금액 0 | 데이터 없는 월(미래 포함)의 **응답 일관성**(빈 월과 동일하게 0) 확인 |
| **음수/양수 정산 합산(운영자 집계 2025-02)** | creator-2 −60,000 + creator-3 +96,000 = 36,000 | 환불만 있는 달(음수 payout)이 집계 **합계에 정확히 반영**되는지 |
| **잘못된 연월 형식**(`2025-13`) | 400 | 날짜 파싱 실패의 일관된 에러 응답 |
| **존재하지 않는 크리에이터** 조회 | 404 | 리소스 부재 처리 |
| **중복 정산 생성**(creator-1 2025-03 2회) | 409 | 사전검증 + 유니크 제약으로 중복 정산 차단 |
| **잘못된 상태 전이**(CONFIRMED 재확정) | 409 | 상태 머신 불변식(엔티티 캡슐화) |
| **환불 누적 초과 / 취소일시 < 결제일시** | 각각 400 | 환불 정책(`RefundPolicy`) 경계 검증 |

> 참고(설계 한계): 현재 **미래 일시 결제 등록**은 별도로 막지 않습니다(결제 시점 검증은 범위 밖으로 단순화). 필요 시 `paidAt ≤ now` 검증을 추가할 수 있습니다.

#### 추가 시드 데이터 (creator-4 / 2025-04·05) — 기존 데이터에 없던 케이스

기본 시드에는 "한 판매당 취소 1건"·"딱 떨어지는 수수료"만 있어서, 아래 케이스를 위해 `data.sql`에 creator-4와 4·5월 데이터를 직접 추가했습니다. (기존 시나리오/집계 테스트는 2~3월만 보므로 영향 없음)

| 추가 케이스 | 데이터 | 기대 결과 | 왜 추가했나 |
|---|---|---|---|
| **한 판매에 다회 부분 환불** | sale-8(100,000) ← cancel-4(20,000)+cancel-5(30,000) | 누적 50,000 / 취소 2건 반영 | 시드엔 판매당 취소 1건뿐 → 다회 환불의 **시드 레벨** 검증 |
| **수수료 소수점 버림** | creator-4 2025-04, 순 83,333 | 수수료 16,666(=83,333×0.2 버림) / 정산 66,667 | 기존 금액은 ×0.2가 모두 정수라 **버림이 한 번도 안 일어남** → RoundingMode.DOWN 검증 |
| **순 판매 정확히 0인 달** | sale-10(40,000) ← cancel-6(40,000), 둘 다 2025-05 | 판매·취소는 존재하나 순 0 / 정산 0 | 데이터가 **있으면서** 순액이 0인 경우와 "빈 월"의 처리 일관성 구분 |

## AI 활용 범위

본 과제는 AI 코딩 도구(Claude)를 보조 도구로 활용했습니다.

- **활용한 부분**: 정산 계산/조회·운영자 집계 API 구현 초안, 테스트 코드 작성,
  동시성·성능 개선 아이디어 검토, 코드 리뷰, README 문서화.
- **직접 판단·책임진 부분**: 설계 선택(스냅샷 기반 정산 확정, 동시성 처리 방식 등), 최종 코드 검토 및 테스트 검증.

AI가 생성한 코드는 그대로 사용하지 않고 전부 직접 읽고 이해한 뒤 수정·검증했으며,
모든 결과물에 대한 판단과 책임은 본인에게 있습니다.