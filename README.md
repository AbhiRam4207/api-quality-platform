# AI API Quality & Reliability Platform

**A developer-focused platform for testing REST APIs, validating response contracts, tracking execution history, and detecting regressions.**

The AI API Quality & Reliability Platform is a Java-based backend project designed to help developers verify API behavior, identify unexpected response changes, and investigate reliability issues through automated testing and execution history.

Instead of manually checking every API response, developers can register an API, define expected behavior, execute reusable tests, validate responses, compare contracts, and identify potential regressions.

> **Project status:** Actively under development. The core API testing and contract-comparison workflows are implemented. Regression analysis and additional reliability improvements are evolving. AI-assisted capabilities are part of the planned roadmap, not a claim of currently implemented functionality.

---

## Table of Contents

- [Why This Project?](#why-this-project)
- [Key Features](#key-features)
- [How It Works](#how-it-works)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Example Workflow](#example-workflow)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Current Limitations](#current-limitations)
- [Roadmap](#roadmap)
- [Security](#security)
- [Contributing](#contributing)
- [License](#license)

---

## Why This Project?

REST APIs are a critical part of modern applications, but an endpoint returning HTTP 200 does not necessarily mean the API is behaving correctly.

A response might be missing required fields, return unexpected data types, become significantly slower, or change in a way that breaks a consuming application.

This project explores how to detect these problems systematically through reusable tests, response validation, contract comparison, execution history, and regression analysis.

### Project Goals

- Automate repetitive REST API verification.
- Validate HTTP status codes and response structures.
- Detect unexpected changes in API response contracts.
- Record test execution results for later analysis.
- Identify potential reliability and performance regressions.
- Build a foundation for future AI-assisted test generation and failure analysis.

## Key Features

### 1. API Registration and Test Management

- Register APIs for testing.
- Configure reusable tests for registered APIs.
- Store expected HTTP status codes and validation requirements.
- Configure request methods, headers, request bodies, query parameters, and path parameters.

### 2. HTTP Request Execution

- Support common HTTP methods: GET, POST, PUT, PATCH, and DELETE.
- Configure request headers and JSON request bodies.
- Support query and path parameters.
- Support NONE, BASIC, BEARER, and API-key authentication modes.
- Configure request timeouts and measure response time.

### 3. Response Validation

- Validate actual HTTP status against the expected status.
- Check for required response fields.
- Validate JSON data types.
- Validate response-time expectations.
- Record test results and execution details.

### 4. API Contract Comparison

Compare two API execution responses to identify changes in their JSON structures.

The comparison workflow supports identifying:

- Added fields.
- Removed fields.
- Fields whose JSON data types have changed.

Contract comparison history can be persisted for later inspection.

### 5. Execution History

- Persist test execution records.
- Retrieve execution history for a test.
- Store execution status, response data, and measured response time.

### 6. Regression Analysis

The project includes regression-analysis logic intended to identify changes in API behavior and performance across executions.

Current logic covers status changes, performance regressions, configurable performance thresholds, and execution errors. This functionality is still being tested and improved.

---

## How It Works

The platform follows a simple workflow:

1. **Register an API** — provide its name, URL, HTTP method, and authentication configuration.
2. **Create a test** — define the expected status code, required fields, data types, and request configuration.
3. **Execute the test** — the platform sends the configured HTTP request.
4. **Validate the response** — compare the actual response with the expected behavior.
5. **Persist the result** — store execution details for future inspection.
6. **Compare contracts** — compare responses from different executions.
7. **Analyze regressions** — identify potential changes in status, response time, or execution reliability.

The goal is to make API behavior easier to verify and unexpected changes easier to investigate.

## Architecture

The backend uses a layered architecture to separate HTTP handling, business logic, and data access.

```text
Client / API Consumer
        |
        v
REST Controllers
        |
        v
Service Layer
        |
        +----------------------+
        |                      |
        v                      v
API Execution           Response Validation
        |                      |
        +-----------+----------+
                    |
                    v
             Repositories
                    |
                    v
              PostgreSQL
```

### Layer Responsibilities

| Layer | Responsibility |
|---|---|
| Controller | Exposes REST endpoints and handles incoming requests. |
| Service | Implements API management, execution, validation, contract comparison, and regression logic. |
| Repository | Provides persistence and database access through Spring Data JPA. |
| Entity | Represents persistent domain objects. |
| DTO | Defines request and response data exchanged through the API. |
| Configuration | Configures infrastructure components such as the HTTP client. |

The application is designed to keep business logic out of controllers and database access out of the service layer.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Primary programming language |
| Spring Boot 4.1.0 | Backend application framework |
| Spring Web / RestClient | REST endpoints and outbound HTTP requests |
| Spring Data JPA | Persistence abstraction |
| Hibernate | ORM and database interaction |
| PostgreSQL | Relational database |
| Maven | Dependency management and build automation |
| JUnit / Spring Boot Test | Automated testing |
| Git and GitHub | Version control and source-code hosting |

The exact dependency configuration is defined in [`pom.xml`](pom.xml).

---

## Getting Started

### Prerequisites

Install the following:

- Java Development Kit (JDK) 21
- PostgreSQL
- Git
- An API client such as Postman or curl

The Maven Wrapper is included, so a separate Maven installation should not be necessary.

### 1. Clone the Repository

```bash
git clone https://github.com/AbhiRam4207/api-quality-platform.git
cd api-quality-platform
```

### 2. Create the PostgreSQL Database

Create a database named `api_quality` using PostgreSQL.

For example, connect to PostgreSQL as an appropriately privileged database user and execute:

```sql
CREATE DATABASE api_quality;
```

Use a dedicated local database role with the required permissions for the application.

### 3. Configure Environment Variables

The application reads its database connection settings from environment variables.

Set the following variables in your terminal or development environment:

| Variable | Purpose | Example |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/api_quality` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | Set locally; do not commit it |
| `API_EXECUTION_USERNAME` | Optional username for the execution endpoint | Configure only if needed |
| `API_EXECUTION_PASSWORD` | Optional password for the execution endpoint | Configure only if needed |

Example PowerShell configuration for the current terminal session:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/api_quality"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="YOUR_LOCAL_DATABASE_PASSWORD"
```

Replace the placeholder with your local database password. Do not commit real credentials to source control.

The application configuration contains safe defaults for the connection URL and username, but the password must be supplied in your local environment when required.

### 4. Run the Application

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On Linux or macOS:

```bash
./mvnw spring-boot:run
```

The application uses the Spring Boot configured server port, which is 8080 unless overridden.

### 5. Verify the Application

Use an API client to call one of the endpoints documented below.

The exact runtime behavior depends on your local database configuration and the current project implementation.

---

## API Endpoints

The following table summarizes the implemented endpoint routes.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/apis` | Register an API |
| GET | `/api/apis` | Retrieve registered APIs |
| POST | `/api/apis/{apiId}/tests` | Create a test for a registered API |
| GET | `/api/apis/{apiId}/tests` | Retrieve tests for an API |
| POST | `/api/apis/tests/{testId}/execute` | Execute a configured API test |
| GET | `/api/apis/execute?url={url}` | Execute a direct HTTP request |
| GET | `/api/test-executions/test/{testId}` | Retrieve execution history for a test |
| GET | `/api/contracts/compare/{previousExecutionId}/{currentExecutionId}` | Compare two execution contracts |
| GET | `/api/contracts/history` | Retrieve contract-comparison history |
| GET | `/api/regressions/test/{testId}/execution/{executionId}` | Analyze a test execution for regression |

**Important:** Confirm the current controller mappings and request DTOs before relying on this table as a complete API reference. Authentication requirements, request schemas, and response formats should be documented alongside the endpoint implementations as the project matures.

## Example Workflow

A typical testing workflow looks like this:

### Step 1: Register an API

Submit an API registration request to `POST /api/apis`, using the request fields defined in `ApiCreateRequest`.

### Step 2: Define a Test

Create a test through `POST /api/apis/{apiId}/tests`.

Configure the expected status, required response fields, data types, request configuration, and optional response-time limit.

### Step 3: Execute the Test

Call `POST /api/apis/tests/{testId}/execute`.

The platform executes the configured request and evaluates the response against the test requirements.

### Step 4: Inspect the Result

Retrieve execution history through `GET /api/test-executions/test/{testId}`.

### Step 5: Compare API Contracts

Compare two execution IDs using the contract-comparison endpoint to inspect structural changes.

### Step 6: Investigate Potential Regressions

Use the regression-analysis endpoint to evaluate the selected execution against the available historical execution data.

For exact JSON request bodies and response examples, refer to the request DTOs and controller implementations in `src/main/java/api_quality_platform/`.

---

## Testing

The project includes automated tests for core execution and validation behavior, including:

- HTTP methods.
- Authentication configuration.
- Custom headers.
- Request bodies.
- Query and path parameters.
- Response validation.
- Negative HTTP status expectations.
- Contract comparison and persistence.
- Execution error handling.

Run the test suite on Windows:

```powershell
.\mvnw.cmd test
```

Or on Linux and macOS:

```bash
./mvnw test
```

### Database-Dependent Tests

Some integration tests require a working PostgreSQL connection and valid test database credentials. Configure the test environment before running the complete suite.

The current test setup is being improved so that database-dependent tests can be executed more reliably in local and clean-clone environments.

Do not interpret a successful compilation as proof that every test passes.

---

## Project Structure

```text
api-quality-platform/
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/api_quality_platform/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/api_quality_platform/
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml
├── PROJECT_STRUCTURE.md
└── README.md
```

- `controller/` — REST endpoint definitions.
- `dto/` — request and response objects.
- `entity/` — JPA entities.
- `repository/` — persistence interfaces.
- `service/` — business and domain logic.
- `config/` — application infrastructure configuration.
- `src/test/` — automated test suite.

---

## Current Limitations

This project is under active development. Current areas for improvement include:

- More reliable test database configuration for clean environments.
- Broader regression-analysis test coverage.
- Consistent structured error responses.
- More comprehensive request and response validation.
- Better documentation of API request schemas and examples.
- Additional security hardening and configuration validation.
- Continuous integration and automated quality checks.

These are active engineering concerns, not claims that the platform is production-ready.

## Roadmap

Planned enhancements include:

- [ ] Improve database-independent and integration-test workflows.
- [ ] Expand regression-analysis test coverage.
- [ ] Add richer contract-change reporting.
- [ ] Provide clearer execution failure diagnostics.
- [ ] Add an interactive frontend for API and test management.
- [ ] Introduce AI-assisted test generation.
- [ ] Add AI-assisted failure analysis and debugging suggestions.
- [ ] Integrate Git-based workflows for regression checks.
- [ ] Add scheduled executions and historical reporting.
- [ ] Add Docker-based development and deployment.
- [ ] Introduce CI/CD integration.

Roadmap items may change as the project evolves.

## Security

Security is an ongoing part of the project.

- Keep database passwords and API credentials out of committed files.
- Use environment variables or appropriate local secret-management mechanisms.
- Never publish real API tokens, passwords, or private keys.
- Review authentication settings before exposing the application to a network.
- Treat outbound API URLs and credentials as untrusted input and sensitive configuration.
- Rotate any credential that may have been exposed in a repository or shared environment.

The application is a development project and should not be considered production-hardened without additional security review.

## Contributing

Contributions, suggestions, and bug reports are welcome.

A useful contribution workflow is:

1. Fork the repository.
2. Create a feature branch.
3. Implement a focused change.
4. Add or update tests.
5. Run the relevant test suite.
6. Open a pull request describing the change and its validation.

Keep changes focused, avoid committing secrets, and document any new configuration requirements.

## License

No license has been specified yet. Until a license is added to the repository, do not assume that the code is available for unrestricted reuse, modification, or redistribution.

---

**Built as a hands-on exploration of Java, Spring Boot, REST API testing, contract validation, and software reliability engineering.**