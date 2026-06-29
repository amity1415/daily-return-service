# CLAUDE.md

Reusable guidance for AI assistants (Claude Code, Copilot, Cursor, etc.) working in this repository.
Read this before making changes — it captures the conventions and invariants that are easy to break.

## What this service is

A Spring Boot REST API with a single purpose: calculate a portfolio's **daily return summary** for a
valuation date and classify the result as `VALID`, `REVIEW_REQUIRED`, or `INVALID_INPUT`. One
endpoint: `POST /api/performance/daily-return`.

## Build, test, run

- **Build/test:** `./mvnw clean test` (Maven wrapper is committed; no local Maven needed).
- **Run:** `./mvnw spring-boot:run` — port is in `src/main/resources/application.yml` (currently `8081`).
- **Docs:** Swagger UI at `/swagger-ui.html`, OpenAPI JSON at `/v3/api-docs`, health at `/actuator/health`.
- Java 21 bytecode target (builds/runs on newer JDKs).

## Architecture (layered; keep responsibilities separated)

Base package `com.portfolio.performance`.

| Layer | Class(es) | Responsibility |
|---|---|---|
| `controller` | `PerformanceController` | HTTP boundary only — bind body, delegate, return. **No business logic, no dedup awareness.** |
| `service` | `PerformanceService` | Orchestration: deduplicate → validate → calculate → assemble + lifecycle logging. |
| `service` | `PerformanceCalculator` | Pure return/excess math + REVIEW_REQUIRED rules. No HTTP/framework concerns. |
| `validation` | `DailyReturnValidator` | Input rules; returns field-attributed `FieldError`s. Never throws. |
| `repository` | `DeduplicationStore` / `InMemoryDeduplicationStore` | Idempotency store (`computeIfAbsent`). |
| `domain` | `ReturnStatus`, `CalculationResult` | Value types, no framework deps. |
| `dto` | `DailyReturnRequest`, `DailyReturnResponse`, `FieldError` | API contract (Java records). |
| `exception` | `GlobalExceptionHandler` | Maps malformed/unparseable bodies into the 200 contract. |
| `config` | `ClockConfig`, `OpenApiConfig` | Injectable `Clock`, OpenAPI metadata. |

## Invariants — do not break these

1. **Always HTTP 200.** Every outcome, including bad input, returns 200; the result is carried in the
   body via `status`. Clients branch on `status`, never on HTTP codes. Do not introduce 4xx/5xx for
   business/validation outcomes. (Genuine unexpected server errors may still 500 — that's intended.)
2. **Response always carries all fields.** `DailyReturnResponse` is `@JsonInclude(ALWAYS)`, overriding
   the global `non_null` setting. `reasons` and `errors` are always present (`[]` when none).
3. **`reasons` and `errors` stay in sync.** `errors` is the field-attributed form (`{field, message}`)
   of the same messages in `reasons`. The service derives `reasons` from the errors' messages.
4. **Money/percentages use `BigDecimal`**, never `double`. Results are normalized (trailing zeros
   stripped, no scientific notation — `2.50` → `2.5`).
5. **Thresholds are exclusive.** REVIEW_REQUIRED triggers on drift `> 5` and cash flow `> 20%` of
   begin — *strictly greater than*. Exactly 5% / 20% is VALID.
6. **Return formula** only applies when `beginMarketValue > 0`; otherwise `portfolioReturnPct = 0`.
   A null `benchmarkReturnPct` is treated as `0` for excess/drift.
7. **Idempotency** is keyed on `portfolioId + valuationDate`. A duplicate returns the *original*
   response verbatim (same `processedAt`), for VALID / REVIEW_REQUIRED / INVALID_INPUT alike. Dedup
   lives in the service/repository layer — keep it out of the controller.
8. **Logging hygiene.** Use the `event=` marker style with a `correlationId`. Log identifiers only
   (`portfolioId`, `valuationDate`, status, reasons). **Never log** market values, cash flow, or
   `requestedBy` (PII). INFO for normal flow, WARN for validation failures, DEBUG for internals.

## Validation rules (current scope)

`DailyReturnValidator` reports, each attributed to a field:
1. Negative `beginMarketValue` → `beginMarketValue`
2. Negative `endMarketValue` → `endMarketValue`
3. Blank/missing `currency` → `currency`
4. `beginMarketValue == 0` while `endMarketValue != 0` → `beginMarketValue`

Attribution covers these existing rules only; presence/type checks for every field were intentionally
left out. Malformed/unparseable bodies are handled in `GlobalExceptionHandler` (field named when
Jackson can identify it, else `null`).

## Conventions

- DTOs are Java `record`s; document fields with `@Schema` examples for Swagger.
- Inject `Clock` (don't call `Instant.now()` directly) so timestamps are testable.
- Every change ships with tests. Tests share a singleton dedup store, so either clear it
  (`deduplicationStore.clear()` in `@BeforeEach`) or use unique `portfolioId + valuationDate` per test.
- Keep the controller thin; put logic in the service/calculator/validator.

## Testing

`./mvnw clean test` must stay green. Suites: acceptance (MockMvc, the 5 assessment scenarios),
controller, calculator, validator, and dedup-store. See `PROMPT_LOG.md` for how the project was built.
