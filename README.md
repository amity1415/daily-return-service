# daily-return-service

A Spring Boot REST service that calculates a portfolio's **daily return summary** and decides
whether the result falls within acceptable tolerance.

> Status: the endpoint is wired end-to-end (controller → service) and returns a **placeholder**
> response. The return calculation and tolerance decision are not implemented yet.

## Tech stack

- Java 21 (compiled bytecode level; runs on newer JDKs too)
- Spring Boot 3.4.1 — `web`, `validation`, `actuator`
- springdoc-openapi (Swagger UI)
- JUnit 5 + Spring MockMvc

## Project layout

```
src/main/java/com/portfolio/performance
├── DailyReturnServiceApplication.java   # boot entry point
├── config/        ClockConfig           # injectable Clock for testable timestamps
├── controller/    PerformanceController  # HTTP boundary
├── service/       PerformanceService     # business layer (placeholder)
├── dto/           DailyReturnRequest / DailyReturnResponse
└── domain/        ReturnStatus           # tolerance decision enum
```

## Run

```bash
./mvnw spring-boot:run
```

The app starts on port `8080`.

## The endpoint

`POST /api/performance/daily-return`

### Example request

```bash
curl -i -X POST http://localhost:8080/api/performance/daily-return \
  -H "Content-Type: application/json" \
  -d '{
    "portfolioId": "PORT-001",
    "valuationDate": "2026-06-29",
    "beginMarketValue": 1000000.00,
    "endMarketValue": 1012500.00,
    "netCashFlow": 0.00,
    "benchmarkReturnPct": 1.10,
    "currency": "USD",
    "requestedBy": "analyst.jane"
  }'
```

### Example response (placeholder)

```json
{
  "portfolioId": "PORT-001",
  "benchmarkReturnPct": 1.10,
  "status": "WITHIN_TOLERANCE",
  "message": "Endpoint reachable. Calculation not yet implemented.",
  "processedAt": "2026-06-29T12:00:00Z"
}
```

## Useful URLs

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

## Test

```bash
./mvnw test
```
