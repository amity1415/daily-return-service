# daily-return-service

A Spring Boot REST service that calculates a portfolio's **daily return summary** for a given
valuation date. Given the day's begin/end market values, net cash flow, and a benchmark return, it
computes the portfolio return and excess return, then classifies the result as **VALID**,
**REVIEW_REQUIRED**, or **INVALID_INPUT**. Requests are idempotent on `portfolioId + valuationDate`.

## Tech stack

- Java 21 (bytecode level; builds and runs on newer JDKs too)
- Spring Boot 3.4.1 — `web`, `validation`, `actuator`
- springdoc-openapi (Swagger UI)
- JUnit 5 + Spring MockMvc, AssertJ

## How to run locally

A Maven wrapper is included, so no local Maven install is needed:

```bash
./mvnw spring-boot:run
```

The HTTP port is defined in [`src/main/resources/application.yml`](src/main/resources/application.yml)
— it is currently **`8081`** (`server.port`). All examples below use that port; adjust if you change it.

Run the tests:

```bash
./mvnw test
```

## The endpoint

`POST /api/performance/daily-return` — always returns **HTTP 200**; the outcome is in the body.

### Example request

```bash
curl -s -X POST http://localhost:8081/api/performance/daily-return \
  -H "Content-Type: application/json" \
  -d '{
    "portfolioId": "PORT-001",
    "valuationDate": "2026-06-29",
    "beginMarketValue": 1000000,
    "endMarketValue": 1035000,
    "netCashFlow": 10000,
    "benchmarkReturnPct": 1.8,
    "currency": "USD",
    "requestedBy": "analyst.jane"
  }'
```

### Example response

```json
{
  "portfolioId": "PORT-001",
  "valuationDate": "2026-06-29",
  "portfolioReturnPct": 2.5,
  "benchmarkReturnPct": 1.8,
  "excessReturnPct": 0.7,
  "status": "VALID",
  "reasons": [],
  "processedAt": "2026-06-29T12:00:00Z"
}
```

### Business rules

- `portfolioReturnPct = ((endMarketValue − beginMarketValue − netCashFlow) / beginMarketValue) × 100`
  (and `0` when `beginMarketValue` is `0`).
- `excessReturnPct = portfolioReturnPct − benchmarkReturnPct`.
- **REVIEW_REQUIRED** if either: benchmark drift `|portfolioReturnPct − benchmarkReturnPct| > 5`,
  or cash flow `|netCashFlow| > 20%` of `beginMarketValue`. Both reasons are reported if both fire.
- **INVALID_INPUT** if: a market value is negative, `currency` is missing/blank, `beginMarketValue`
  is `0` while `endMarketValue` is non-zero, or the request body is malformed/unparseable.

## Swagger UI / OpenAPI

With the app running:

- **Swagger UI:** http://localhost:8081/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8081/v3/api-docs

The endpoint and request/response models are documented with examples, so the API is explorable
from the browser without reading the code.

## Other endpoints

- Health: http://localhost:8081/actuator/health

## Assumptions

- **Always HTTP 200.** Every outcome — including invalid input — is returned as 200 with a `status`
  and `reasons`, rather than a 4xx. This is an intentional contract choice so clients branch on
  `status`, not HTTP codes.
- **Idempotency is in-memory and single-instance.** The dedup cache is a `ConcurrentHashMap`, scoped
  to the JVM lifetime, with no eviction. This satisfies the "no database needed" requirement; a
  multi-instance deployment would back the same `DeduplicationStore` interface with a shared store
  (e.g. Redis). A duplicate returns the *original* response verbatim (same `processedAt`), regardless
  of whether it was VALID, REVIEW_REQUIRED, or INVALID_INPUT.
- **Thresholds are exclusive.** Exactly 5% drift or exactly 20% cash flow does **not** trigger
  REVIEW_REQUIRED (the rule is strictly "greater than").
- **`benchmarkReturnPct` is optional and treated as `0`** when absent for the purpose of computing
  excess return and drift. (It is the one numeric field with no presence check.)
- **Money/percentages use `BigDecimal`** to avoid floating-point drift; results are normalized so
  trailing zeros are trimmed (e.g. `2.50` → `2.5`).
- **Logging excludes sensitive data** — market values, cash flow, and `requestedBy` are never logged;
  only identifiers (`portfolioId`, `valuationDate`), a `correlationId`, status, and reasons are.
- **Timestamps are UTC** ISO-8601 with offset (`processedAt`).
