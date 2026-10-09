# API Quality Platform — Project Structure

## Technology Stack
- **Framework:** Spring Boot 4.1.0
- **Language:** Java 21
- **Build Tool:** Maven
- **Database:** PostgreSQL
- **ORM:** Spring Data JPA (Hibernate)
- **HTTP Client:** RestClient (Spring 6+)
- **JSON Processing:** Jackson (`jackson-databind`)
- **Validation:** `spring-boot-starter-validation`

---

## Directory Layout

### Main Sources

```
src/main/java/api_quality_platform/
├── ApiQualityPlatformApplication.java      # Spring Boot entry point
│
├── config/
│   └── RestClientConfig.java               # RestClient & ObjectMapper beans
│
├── controller/
│   ├── ApiController.java                  # CRUD for APIs
│   ├── ApiTestController.java              # Test CRUD, execution & raw execute endpoints
│   ├── ContractComparisonController.java   # Contract diff & history endpoints
│   ├── RegressionController.java           # Regression detection endpoint
│   └── TestExecutionController.java        # Test execution history endpoint
│
├── dto/
│   ├── ApiCreateRequest.java               # API creation payload (incl. auth config)
│   ├── ApiResponse.java                    # API read response (credentials excluded)
│   ├── ApiTestCreateRequest.java           # Test creation payload (incl. validation rules + request body + headers + query params + path params + max response time + timeout)
│   ├── ApiTestResponse.java                # Test read response
│   ├── ContractComparisonHistoryResponse.java  # Contract diff history response
│   ├── ContractComparisonResult.java       # Contract diff result (added/removed/type-changed)
│   ├── RegressionResponse.java             # Regression check result
│   └── ValidationResponse.java             # Validation outcome payload (success/failure)
│
├── entity/
│   ├── Api.java                            # API definition (apis table)
│   ├── ApiTest.java                        # Test definition (api_tests table, incl. request body + headers + query params + path params + max response time + timeout)
│   ├── ContractComparisonHistory.java      # Contract diff log (contract_comparison_history table)
│   └── TestExecution.java                  # Execution log (test_executions table, incl. actual response time, nullable actual status for failures)
│
├── exception/
│   └── NotFoundException.java              # 404 Not Found exception
│
├── repository/
│   ├── ApiRepository.java                  # API data access
│   ├── ApiTestRepository.java              # Test data access (findByApiId)
│   ├── ContractComparisonHistoryRepository.java  # Contract diff history access
│   └── TestExecutionRepository.java        # Execution history access
│
└── service/
    ├── ApiService.java                     # API operations
    ├── ApiTestService.java                 # Test operations & execution orchestration (incl. request body + headers + query params + response time + timeout wiring + error handling)
    ├── ApiExecutionService.java            # HTTP method-aware execution via RestClient (incl. body, headers, query params, timing, timeout, error classification)
    ├── ApiExecutionResult.java             # Execution result wrapper (incl. actual response time + error info)
    ├── ApiResponseValidationService.java   # Status/field/type/response-time validation of responses
    ├── ApiTestResult.java                  # Test result wrapper (incl. validation outcome + actual response time + execution error info)
    ├── ContractComparisonService.java      # JSON field-level diffing + history persistence
    ├── RegressionService.java              # Regression detection between executions
    ├── TestExecutionService.java           # Persistence & lookup for test executions
    └── ValidationResult.java               # Validation result wrapper
```

### Test Sources

```
src/test/java/api_quality_platform/
├── ApiQualityPlatformApplicationTests.java
└── service/
    ├── ApiExecutionServiceAuthTest.java
    ├── ApiExecutionServiceErrorHandlingTest.java
    ├── ApiExecutionServiceHeadersTest.java
    ├── ApiExecutionServiceMethodTest.java
    ├── ApiExecutionServicePathParamsTest.java
    ├── ApiExecutionServiceQueryParamsTest.java
    ├── ApiExecutionServiceRequestBodyTest.java
    ├── ApiResponseValidationServiceNegativeStatusTest.java
    ├── ApiResponseValidationServiceTest.java
    ├── ApiResponseValidationServiceTypeValidationTest.java
    ├── ContractComparisonHistoryPersistenceTest.java
    └── ContractComparisonServiceTest.java
```

---

## Layer Responsibilities

| Layer | Responsibility |
|-------|----------------|
| **controller** | HTTP request handling, request validation, response mapping |
| **service** | Business logic, orchestration, transaction boundaries |
| **repository** | Data access abstraction using Spring Data JPA |
| **entity** | JPA entity mappings to database tables |
| **dto** | Request/response payloads to decouple API contracts from entities |
| **config** | Spring bean configuration |
| **exception** | Centralized exception handling |

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/apis` | Create a new API |
| GET | `/api/apis` | Get all APIs |
| POST | `/api/apis/{apiId}/tests` | Create a test for an API |
| GET | `/api/apis/{apiId}/tests` | Get tests for an API |
| POST | `/api/apis/tests/{testId}/execute` | Execute a specific test |
| GET | `/api/apis/execute?url=...` | Execute a raw GET request |
| GET | `/api/test-executions/test/{testId}` | Get execution history for a test |
| GET | `/api/contracts/compare/{previousExecutionId}/{currentExecutionId}` | Compare contract fields between two executions |
| GET | `/api/contracts/history` | List all contract comparison history |
| GET | `/api/regressions/test/{testId}/execution/{executionId}` | Check for status regression vs previous execution |

---

## Database Schema

### apis
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| name | VARCHAR | API name |
| url | VARCHAR | Target URL |
| method | VARCHAR | HTTP method |
| auth_type | VARCHAR | NONE / BASIC / BEARER / API_KEY |
| username | VARCHAR | Basic auth username |
| password | VARCHAR | Basic auth password |
| token | VARCHAR | Bearer token |
| api_key | VARCHAR | API key value |
| api_key_header | VARCHAR | Header name for API key |

### api_tests
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| test_name | VARCHAR | Test name |
| expected_status | INTEGER | Expected HTTP status code |
| required_fields | TEXT | Comma-separated required JSON field paths (dotted) |
| expected_field_types | TEXT | JSON map of field path → expected JSON type |
| request_body | TEXT | Optional JSON request body for POST/PUT/PATCH |
| headers | TEXT | Optional JSON map of request header name → value |
| query_params | TEXT | Optional JSON map of query parameter name → value |
| path_params | TEXT | Optional JSON map of path parameter name → value |
| max_response_time_ms | BIGINT | Optional max allowed response time in milliseconds |
| timeout_ms | BIGINT | Optional per-execution HTTP connect/read timeout in milliseconds |
| api_id | BIGINT | FK → apis.id |

### test_executions
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| test_id | BIGINT | FK → api_tests.id |
| expected_status | INTEGER | |
| actual_status | INTEGER | Nullable when execution fails (timeout / unreachable) |
| passed | BOOLEAN | |
| executed_at | TIMESTAMP | |
| response_body | TEXT | Raw response body for contract comparison; null when execution fails |
| actual_response_time_ms | BIGINT | Actual measured response time in milliseconds |

### contract_comparison_history
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| previous_execution_id | BIGINT | FK → test_executions.id |
| current_execution_id | BIGINT | FK → test_executions.id |
| contract_changed | BOOLEAN | Whether the contract differed |
| added_fields | TEXT | Comma-separated added field paths |
| removed_fields | TEXT | Comma-separated removed field paths |
| type_changed_fields | TEXT | Comma-separated type-changed field paths |
| compared_at | TIMESTAMP | When the comparison ran |

---

## Data Flow

```
ApiTestController.executeTest
        │
        ▼
ApiTestService.runTestById
    ├── ApiExecutionService.execute ──► HTTP (RestClient, method from Api.method, auth from Api.authType, optional request body, optional custom headers, optional query params, optional path params)
    │       ├── measures elapsed time around the HTTP call
    │       ├── applies per-test timeoutMs when configured (creates a RestClient with SimpleClientHttpRequestFactory)
    │       └── catches ResourceAccessException and classifies as TIMEOUT / CONNECTION_ERROR / UNKNOWN_ERROR
    ├── TestExecutionService.save ──► test_executions
    │       ├── success: status + response body + actual response time persisted
    │       └── failure: actual_status/response_body nullable, passed=false
    └── ApiResponseValidationService.validate
            ├── expected status vs actual status
            ├── required fields present (dotted JSON paths)
            ├── field types match expected types (JSON type map)
            └── actual response time vs max response time (if configured)
        │
        ▼
ApiTestResult (passed + validation valid/errors + actual response time + execution error info)

ContractComparisonController.compare
    ├── loads previous & current TestExecution
    └── ContractComparisonService.compareExecutions
            ├── collect field names from both JSON bodies
            ├── added / removed / type-changed field sets
            └── ContractComparisonHistoryRepository.save ──► contract_comparison_history
```

---

## Key Design Decisions

- **Immutable DTOs:** `ApiTestResponse`, `ApiTestResult`, `ApiExecutionResult`, `ContractComparisonResult`, `RegressionResponse`, and `ValidationResult` are immutable value objects with `final` fields and no setters.
- **Validation:** `ApiResponseValidationService` validates expected status code, presence of required fields (dotted paths), and JSON field types (via a JSON type map with `ARRAY`/`OBJECT`/`STRING`/`NUMBER`/`BOOLEAN`/`NULL`). `ValidationResponse` offers static `success()` / `failure()` factories.
- **Exception handling:** Domain-specific `NotFoundException` is used consistently across services to return HTTP 404 with descriptive messages.
- **Null-safety:** `ApiExecutionService.execute` guards against null/blank credentials and null/blank methods before calling `setBasicAuth`; defaults to GET.
- **Method-aware execution:** `ApiExecutionService.execute` reads the HTTP method from `Api.method` (GET/POST/PUT/PATCH/DELETE) and routes through the corresponding `RestClient` verb. `executeGet` remains as a backward-compatible GET-only shortcut.
- **Multi-type authentication:** `ApiExecutionService.execute` reads `Api.authType` and applies NONE, BASIC (username/password), BEARER (token), or API_KEY (header + key). Custom headers are applied before auth headers so non-auth headers are never overwritten. Credentials are never exposed via `ApiResponse`.
- **Request body:** `ApiTest.requestBody` stores an optional JSON body (TEXT). For POST/PUT/PATCH, `ApiExecutionService.execute` writes it via `RequestHeadersSpec.httpRequest` before `exchange()`. GET and DELETE ignore it.
- **Configurable headers:** `ApiTest.headers` stores an optional JSON map of header name → value (TEXT). Applied via `RequestHeadersSpec.headers()` before Basic Auth, so Basic Auth's `Authorization` header is never overwritten.
- **Configurable query parameters:** `ApiTest.queryParams` stores an optional JSON map of query parameter name → value (TEXT). Applied via `UriComponentsBuilder` before the request is sent, properly encoding values. Tests without query params behave identically to before.
- **Configurable path parameters:** `ApiTest.pathParams` stores an optional JSON map of path parameter name → value (TEXT). Placeholders like `{id}` in the API URL are replaced with configured values. Values are URI-encoded via `UriComponentsBuilder`. Unresolved placeholders throw `IllegalArgumentException`. Tests without path params behave identically to before.
- **Contract comparison:** `ContractComparisonService` parses JSON response bodies and computes field-level diffs — added fields, removed fields, and fields whose JSON type changed — between two executions.
- **History persistence:** Every `compareExecutions` call saves a `ContractComparisonHistory` row, enabling audit via `GET /api/contracts/history`.
- **Regression detection:** `RegressionService` compares the current execution's actual status against the previous execution's actual status for the same test.
- **Response-time validation:** `ApiTest.maxResponseTimeMs` (optional) sets a per-test SLA. `ApiExecutionService.execute` measures elapsed wall-clock time around the RestClient HTTP call and exposes it via `ApiExecutionResult.actualResponseTimeMs`. `ApiResponseValidationService.validateResponseTime` fails the test when actual time exceeds the configured limit; the check is skipped when `maxResponseTimeMs` is null. The timing value is also persisted in `test_executions.actual_response_time_ms` and returned in `ApiTestResult.actualResponseTimeMs`.
- **Timeout and error handling:** `ApiTest.timeoutMs` (optional) configures a per-execution HTTP connect/read timeout via `SimpleClientHttpRequestFactory`. `ApiExecutionService.execute` catches `ResourceAccessException` and classifies the error as `TIMEOUT` (`SocketTimeoutException`), `CONNECTION_ERROR` (`UnknownHostException`, `ConnectException`), or `UNKNOWN_ERROR`. Execution errors are returned as `ApiExecutionResult` with `error=true`, an `errorMessage`, and an `errorType`. `ApiTestService.runTest` persists failures in `TestExecution` with `actual_status` and `response_body` nullable and `passed=false`. The error details are surfaced in `ApiTestResult` via `executionError`, `executionErrorMessage`, and `executionErrorType` so clients can distinguish API communication failures from validation failures.

---

## Configuration

**application.properties:**
- Database: PostgreSQL on `localhost:5432/api_quality`
- Hibernate DDL auto-update enabled
- SQL logging enabled
