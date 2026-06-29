# Prompt Log

This file records the prompts used to build **daily-return-service**, in order. Each prompt drove
one increment of the implementation; together they trace the project from an empty scaffold to the
finished, field-attributed error contract.

---

## Prompt 1 — Bootstrap

I am building a Spring Boot backend API for a portfolio performance platform. The single purpose of
this service is to calculate a daily return summary for a portfolio and determine whether the result
is within acceptable tolerance.

Set up a Spring Boot project with the following:

- A single POST endpoint at /api/performance/daily-return
- The endpoint should accept a JSON request body and return a JSON response with HTTP 200
- For now, the response can be a placeholder — just confirm the endpoint is reachable and returns 200

The request will eventually carry fields like portfolio ID, valuation date, market values, cash flow,
benchmark return, currency, and who requested it. The response will eventually carry calculated
return percentages and a status decision.

Keep the project structure clean and layered. Choose your own package names, class names, and project
structure. Include any dependencies you consider standard for a production-ready Spring Boot REST API.

**Goal:** the app starts without errors and a POST to /api/performance/daily-return returns HTTP 200.

---

## Prompt 2 — Request / Response Contract + Input Validation

I have a running Spring Boot API with a POST endpoint at /api/performance/daily-return that returns a
placeholder 200.

Now I want to lock down the API contract and add input validation.

The request body must carry:
  - portfolioId       (String)
  - valuationDate     (ISO date string)
  - beginMarketValue  (number)
  - endMarketValue    (number)
  - netCashFlow       (number)
  - benchmarkReturnPct (number, percentage)
  - currency          (String, required)
  - requestedBy       (String)

The response body must carry:
  - portfolioId
  - valuationDate
  - portfolioReturnPct   (calculated, not yet — return 0 for now)
  - benchmarkReturnPct
  - excessReturnPct      (not yet — return 0 for now)
  - status               (VALID / REVIEW_REQUIRED / INVALID_INPUT)
  - reasons              (list of strings explaining why status was set)
  - processedAt          (server timestamp, ISO format with timezone)

Implement these input validation rules. Any failure must return HTTP 200 with status = "INVALID_INPUT"
and a clear reason in the reasons list:

  1. If beginMarketValue or endMarketValue is negative → INVALID_INPUT
  2. If currency is missing or blank → INVALID_INPUT
  3. If beginMarketValue is exactly 0 and endMarketValue is not 0 → INVALID_INPUT

For any request that passes all three checks, return HTTP 200 with status = "VALID" for now
(calculation comes next).

Keep the layered structure clean. Feel free to introduce any service classes or validators you think
make sense.

**Goal:** valid input returns 200 VALID, bad input returns 200 INVALID_INPUT with a reason.

---

## Prompt 3 — Calculation Engine + Business Rules

I have a Spring Boot API where:
- The request/response contract is defined
- Input validation correctly returns INVALID_INPUT for bad requests
- Valid requests currently return a hardcoded VALID with zeroes

Now implement the core business logic for valid requests.

**Step 1 — Calculate portfolio return:**
  portfolioReturnPct = ((endMarketValue - beginMarketValue - netCashFlow) / beginMarketValue) * 100

  This formula only applies when beginMarketValue > 0. If beginMarketValue is 0 and endMarketValue is
  also 0, treat portfolioReturnPct as 0.

**Step 2 — Calculate excess return:**
  excessReturnPct = portfolioReturnPct - benchmarkReturnPct

**Step 3 — Determine the status:**
  Check both these conditions independently (a request can trigger both):

  Condition A — Benchmark drift:
    If the absolute difference between portfolioReturnPct and benchmarkReturnPct is greater than 5%,
    the status is REVIEW_REQUIRED. Reason: explain that return deviates from benchmark by more than 5%.

  Condition B — Cash flow spike:
    If the absolute value of netCashFlow is greater than 20% of beginMarketValue, the status is
    REVIEW_REQUIRED. Reason: explain that net cash flow exceeds 20% of begin market value.

  If neither condition triggers, status = VALID and reasons = [].
  If one or both trigger, status = REVIEW_REQUIRED with all triggered reasons in the list.

All responses return HTTP 200. Keep the business rules isolated in their own class — separate from the
controller and HTTP concerns.

**Goal:** POST with beginMarketValue=1000000, endMarketValue=1035000, netCashFlow=10000,
benchmarkReturnPct=1.8 returns portfolioReturnPct=2.5, excessReturnPct=0.7, status=VALID.

---

## Prompt 4 — Idempotency + Observability

I have a working Spring Boot API that correctly calculates portfolio return and classifies results as
VALID, REVIEW_REQUIRED, or INVALID_INPUT.

Now address two non-functional requirements.

**Requirement 1 — Idempotency:**
  The same request submitted more than once must not be processed twice. A request is considered a
  duplicate if it has the same portfolioId and valuationDate as a previously processed request.

  On a duplicate: return the original response immediately without recalculating. You can use an
  in-memory store for this — no database is needed.

  The behaviour must be the same whether the first request was VALID, REVIEW_REQUIRED, or
  INVALID_INPUT.

**Requirement 2 — Observability:**
  Add structured logging throughout the processing lifecycle so it is possible to trace what happened
  to any request. At minimum, log:
  - When a request is received (include portfolioId)
  - When a duplicate is detected
  - When validation fails
  - When processing completes successfully

  Do not log sensitive values like raw market values or PII. Use appropriate log levels (INFO for
  normal flow, WARN for validation failures, DEBUG for internals if needed).

Keep all changes inside the service/repository layer. The controller should not know about
deduplication.

**Goal:** sending the same portfolioId + valuationDate twice returns the same response both times.
Logs clearly show the duplicate was detected on the second call.

---

## Prompt 5 — Tests + Swagger + Assessment Wrap-up

I have a complete Spring Boot API for portfolio daily return calculation with validation, business
logic, idempotency, and logging all working.

Now finalise the project for assessment submission.

**1. Automated tests:**
   Write a test class using Spring Boot's test support (MockMvc or WebTestClient, your choice).
   Include at least these 5 test cases:

   Test 1 — Happy path VALID:
     beginMarketValue=1000000, endMarketValue=1035000, netCashFlow=10000, benchmarkReturnPct=1.8,
     currency=USD. Expect: status=VALID, portfolioReturnPct=2.5, excessReturnPct=0.7, reasons is empty

   Test 2 — REVIEW_REQUIRED from benchmark drift:
     Use a benchmarkReturnPct far enough from the calculated return to exceed 5% absolute difference.
     Expect: status=REVIEW_REQUIRED, reasons list is not empty

   Test 3 — REVIEW_REQUIRED from cash flow spike:
     Use a netCashFlow greater than 20% of beginMarketValue. Expect: status=REVIEW_REQUIRED

   Test 4 — INVALID_INPUT from negative market value:
     Pass a negative beginMarketValue. Expect: status=INVALID_INPUT, reasons list contains a message

   Test 5 — Idempotency:
     Send the same request twice. Assert both calls return 200 with identical status and
     portfolioReturnPct values.

   All 5 tests must pass on a clean build.

**2. Swagger / OpenAPI:**
   Add Swagger UI so the API is self-documenting and explorable in the browser. Add a meaningful
   title, description, and version to the OpenAPI spec. Add a brief description to the POST endpoint
   so an assessor can understand what it does without reading the code.

**3. README:**
   Add a README.md with:
   - What the service does (2-3 sentences)
   - How to run it locally
   - How to access Swagger UI
   - Any assumptions made

**Goal:** mvn test passes with all 5 tests green, Swagger UI is accessible at the default path,
README is present in the root of the project.

---

## Prompt 6 — Updating the Error Response Message

I have a Spring Boot API for portfolio daily return calculation. It already returns HTTP 200 for
every request, where the outcome is carried in the body as a status (VALID / REVIEW_REQUIRED /
INVALID_INPUT) plus a reasons list of strings.

Right now, when a request is rejected as INVALID_INPUT, the reasons list explains what is wrong in
prose (e.g. "beginMarketValue must not be negative") but a client cannot machine-read which field
caused each error.

Enhance the error reporting so each validation error names the offending field.

**Requirement:**
For any 200 response with status = INVALID_INPUT, the response must also indicate, per error, which
request field caused it.

**Response shape (additive, non-breaking):**
- Keep the existing reasons (List<String>) exactly as-is.
- Add a new errors array, where each entry is an object:
  - field — the request field that caused the error (e.g. beginMarketValue)
  - message — the human-readable explanation (same text as the matching reason)
- The errors array must always be present: [] when there are no errors.

**Scope — attribute the existing validation rules only** (do not add new presence/type checks for
every field):
  1. Negative beginMarketValue → field = beginMarketValue
  2. Negative endMarketValue → field = endMarketValue
  3. Blank/missing currency → field = currency
  4. beginMarketValue is 0 while endMarketValue is non-zero → field = beginMarketValue

**Malformed / unparseable body:**
- When a single field fails to parse (e.g. an invalid valuationDate), attribute the error to that field.
- When the whole body is unparseable, set field to null.

**Constraints:**
- Keep the layered structure clean — validation/error logic stays out of the controller.
- VALID and REVIEW_REQUIRED responses must have errors = [].
- Add tests covering field attribution for each rule and the bad-date case.

**Goal:** an INVALID_INPUT response returns both reasons and a parallel errors array; each error
correctly names its field; all existing tests still pass.
