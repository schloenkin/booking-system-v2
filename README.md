# Booking System v2

[![CI](https://github.com/schloenkin/booking-system-v2/actions/workflows/ci.yml/badge.svg)](https://github.com/schloenkin/booking-system-v2/actions/workflows/ci.yml)

A production-style backend portfolio project built with Java and Spring Boot.

Booking System v2 provides secure management of users, bookable services, and bookings. The project demonstrates modular architecture, domain-driven business rules, JWT authentication, role-based and object-level authorization, PostgreSQL persistence, database migrations, concurrency control, automated testing, OpenAPI documentation, containerization, and continuous integration.

## Key Features

- User registration and login
- Stateless JWT authentication
- Role-based authorization with `USER` and `ADMIN`
- Ownership-based access control for bookings
- Bookable service management
- Booking creation, search, filtering, sorting, and pagination
- Controlled booking status transitions
- PostgreSQL persistence through Spring Data JPA
- Flyway database migrations
- Optimistic locking for concurrent booking updates
- Unified API error contract with stable machine-readable error codes
- OpenAPI 3 documentation with Swagger UI and JWT authorization
- Unit, controller, security, integration, and concurrency tests
- PostgreSQL integration tests with Testcontainers
- Multi-stage Docker image
- Docker Compose runtime for the application and PostgreSQL
- GitHub Actions CI for Maven verification and Docker image builds

## Technology Stack

- Java 17
- Spring Boot 3.5.14
- Spring Web
- Spring Security
- JWT with JJWT 0.13.0
- Spring Data JPA
- PostgreSQL 17
- Flyway
- Springdoc OpenAPI 2.8.17
- Docker
- Docker Compose
- GitHub Actions
- Maven
- JUnit 5
- Mockito
- MockMvc
- Testcontainers

## Architecture

The project is organized as a multi-module Maven application:

```text
booking-system-v2
|-- .github
|   `-- workflows
|       `-- ci.yml
|-- booking-domain
|-- booking-application
|-- booking-infrastructure
|-- booking-api
|-- docs
|   `-- adr
|-- .env.example
|-- Dockerfile
|-- compose.yaml
|-- request.http
|-- README.md
`-- pom.xml
```

### `booking-domain`

Contains the core business model and domain rules.

Examples:

- `Booking`
- `BookableService`
- booking status transitions
- domain exceptions
- booking invariants

The domain module does not depend on Spring, JPA, HTTP, or PostgreSQL.

### `booking-application`

Contains application use cases and repository contracts.

Examples:

- `BookingService`
- `BookableServiceService`
- authentication service
- authorization services and policies
- search and pagination models
- repository interfaces

### `booking-infrastructure`

Contains technical implementations.

Examples:

- JPA entities
- Spring Data repositories
- repository adapters
- PostgreSQL persistence
- Flyway migrations
- JWT token implementation
- password hashing
- in-memory repository implementations

### `booking-api`

Contains the HTTP, documentation, and security entry points.

Examples:

- REST controllers
- request and response DTOs
- validation
- Spring Security configuration
- JWT authentication filter
- global exception handling
- OpenAPI configuration
- application configuration
- static demonstration page

## Dependency Direction

The main dependency direction is:

```text
booking-api
     |
     v
booking-application
     |
     v
booking-domain

booking-infrastructure
     |
     +--> booking-application
     `--> booking-domain
```

Business rules remain inside the domain model, while infrastructure components implement technical concerns such as persistence and JWT handling.

## Booking Lifecycle

A new booking is created with the status:

```text
PENDING
```

Allowed transitions:

```text
PENDING   -> CONFIRMED
PENDING   -> CANCELLED
CONFIRMED -> CANCELLED
```

Other transitions are rejected by the domain model.

Examples:

```java
booking.confirm();
booking.cancel();
```

A booking may be physically deleted only after it has reached the `CANCELLED` status.

The repository does not decide whether a transition is allowed. It persists the aggregate only after the domain operation has succeeded.

Additional architectural reasoning is documented in:

[ADR 0001: Booking domain rules and persistence contract](docs/adr/0001-booking-domain-and-persistence-contract.md)

## Security Model

The API uses stateless JWT authentication.

### Public endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a user |
| `POST` | `/api/auth/login` | Authenticate and receive a JWT |
| `GET` | `/api/health` | Application health check |
| `GET` | `/api/services` | List bookable services |
| `GET` | `/api/services/{id}` | Get a service by ID |

Swagger UI and the generated OpenAPI documents are also publicly accessible.

### `USER` permissions

A regular user can:

- create a booking for themselves;
- view only their own bookings;
- retrieve their own booking by ID;
- cancel their own booking.

The owner is determined from the authenticated JWT. A client cannot select another user by passing a `userId` in the request body.

### `ADMIN` permissions

An administrator can:

- create bookable services;
- activate and deactivate services;
- access user endpoints;
- view all bookings;
- confirm bookings;
- cancel any booking;
- delete eligible cancelled bookings.

Authorization does not replace domain validation. An administrator may have permission to attempt an operation, but the domain model can still reject an invalid state transition.

## API Overview

### Authentication

```http
POST /api/auth/register
POST /api/auth/login
```

Registration example:

```json
{
  "email": "user@example.com",
  "password": "UserPass123!"
}
```

Authentication response:

```json
{
  "accessToken": "<jwt-token>"
}
```

Authenticated requests use:

```http
Authorization: Bearer <jwt-token>
```

### Bookable Services

```http
GET  /api/services
GET  /api/services/{id}
POST /api/services
PUT  /api/services/{id}/activate
PUT  /api/services/{id}/deactivate
```

Service creation example:

```json
{
  "name": "Backend consultation",
  "description": "One-hour backend architecture consultation",
  "durationMinutes": 60
}
```

### Bookings

```http
GET    /api/bookings
GET    /api/bookings/{id}
POST   /api/bookings
PUT    /api/bookings/{id}/confirm
PUT    /api/bookings/{id}/cancel
DELETE /api/bookings/{id}
```

Booking creation example:

```json
{
  "serviceId": 1,
  "startTime": "2026-08-10T10:00:00",
  "endTime": "2026-08-10T11:00:00"
}
```

The authenticated user becomes the owner of the booking automatically.

The booking collection endpoint supports search, filtering, sorting, and pagination.

Example:

```http
GET /api/bookings?page=0&size=20&sortBy=startTime&direction=asc
```

A complete example request collection is available in:

[`request.http`](request.http)

## OpenAPI and Swagger UI

The API documentation is generated with Springdoc OpenAPI.

After the application starts, the documentation is available at:

```text
Swagger UI:        http://localhost:8080/swagger-ui.html
OpenAPI JSON:      http://localhost:8080/v3/api-docs
OpenAPI YAML:      http://localhost:8080/v3/api-docs.yaml
```

The OpenAPI definition contains the HTTP bearer security scheme:

```text
bearerAuth
```

To call a protected endpoint from Swagger UI:

1. Log in through `POST /api/auth/login`.
2. Copy the returned access token.
3. Click **Authorize** in Swagger UI.
4. Paste the token without the `Bearer` prefix.
5. Execute the protected request.

Controllers document their success and error responses, including the shared `ErrorResponse` schema.

## Unified Error Contract

Application, validation, authentication, and authorization failures use the same response structure.

Example:

```json
{
  "timestamp": "2026-07-31T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "path": "/api/auth/register",
  "violations": [
    {
      "field": "password",
      "message": "Password must contain between 8 and 72 characters"
    }
  ]
}
```

Contract fields:

| Field | Purpose |
|---|---|
| `timestamp` | Time when the response was created |
| `status` | HTTP status code |
| `error` | Standard HTTP status description |
| `code` | Stable machine-readable API error code |
| `message` | Human-readable error explanation |
| `path` | Request path that produced the error |
| `violations` | Field-level validation errors; empty for other errors |

Examples of stable API error codes include:

```text
AUTHENTICATION_REQUIRED
ACCESS_DENIED
INVALID_CREDENTIALS
VALIDATION_FAILED
USER_NOT_FOUND
SERVICE_NOT_FOUND
BOOKING_TIME_CONFLICT
BOOKING_CANNOT_BE_CONFIRMED
BOOKING_CANNOT_BE_CANCELLED
BOOKING_CANNOT_BE_DELETED
INTERNAL_ERROR
```

The contract is verified by exception-handler, API, JWT security, and OpenAPI integration tests.

## Concurrency Control

Bookings use optimistic locking through a JPA `@Version` field.

When two transactions attempt to update the same persisted booking version concurrently, one update may succeed while the conflicting update fails instead of silently overwriting the committed state.

This protects the system from lost updates during concurrent confirmation or cancellation attempts.

The behavior is covered by PostgreSQL and Testcontainers integration tests.

## Database Migrations

Flyway manages the database schema.

Current migrations:

```text
V1__create_initial_schema.sql
V2__add_booking_version.sql
```

Migrations are located in:

```text
booking-infrastructure/src/main/resources/db/migration
```

## Running the Project with Docker Compose

### Prerequisites

- Docker Desktop or another Docker-compatible runtime
- Git

Java and Maven are required only when building or running the application outside Docker.

### 1. Clone the repository

```bash
git clone https://github.com/schloenkin/booking-system-v2.git
cd booking-system-v2
```

### 2. Create the environment file

Copy:

```text
.env.example
```

to:

```text
.env
```

PowerShell:

```powershell
Copy-Item .env.example .env
```

Set secure local values:

```properties
POSTGRES_DB=booking_db
POSTGRES_USER=booking_user
POSTGRES_PASSWORD=change_me
JWT_SECRET=replace_with_a_long_random_secret_of_at_least_32_characters
JWT_EXPIRATION_MS=3600000
```

The real `.env` file is excluded from Git.

### 3. Build and start the complete application

```bash
docker compose up --build -d
```

Docker Compose starts:

- PostgreSQL;
- the Booking System API.

The API waits until PostgreSQL passes its health check.

### 4. Verify the containers

```bash
docker compose ps
```

Local addresses:

```text
API:               http://localhost:8080
Health check:      http://localhost:8080/api/health
Swagger UI:        http://localhost:8080/swagger-ui.html
PostgreSQL:        localhost:5433
```

### 5. View application logs

```bash
docker compose logs -f booking-api
```

Press `Ctrl+C` to leave the log view without stopping the containers.

### 6. Stop the application

```bash
docker compose down
```

To stop the application and delete the PostgreSQL volume:

```bash
docker compose down -v
```

The `-v` option permanently removes local database data stored in the Compose volume.

## Running from IntelliJ

For development outside the application container, start only PostgreSQL:

```bash
docker compose up -d postgres
```

Then run:

```text
BookingApiApplication
```

The application connects to PostgreSQL through the local port configured for development.

## Creating a Local Administrator

Registration creates users with the `USER` role.

First register a user:

```powershell
$body = @{
    email = "admin@example.com"
    password = "AdminPass123!"
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/auth/register" `
    -ContentType "application/json" `
    -Body $body
```

Open PostgreSQL through the Docker container:

```powershell
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Promote the registered account:

```sql
UPDATE public.users
SET role = 'ADMIN'
WHERE email = 'admin@example.com';
```

Exit PostgreSQL:

```text
\q
```

Log in again to receive a new JWT containing the administrator role.

This database role change is intended only for local demonstration and development.

## Testing

The project contains several test levels:

- domain unit tests;
- application service tests with Mockito;
- authorization policy tests;
- controller tests with MockMvc;
- Spring Security tests;
- repository integration tests;
- PostgreSQL integration tests;
- JWT security integration tests;
- error-contract API tests;
- OpenAPI integration tests;
- concurrency and optimistic-locking tests;
- Testcontainers-based end-to-end integration tests.

Run the full test suite with:

```bash
mvn clean install
```

Docker must be running for tests that start PostgreSQL containers.

The same full build can be run from the root Maven project in IntelliJ:

```text
booking-system-v2
`-- Lifecycle
    |-- clean
    `-- install
```

## Continuous Integration

The GitHub Actions workflow is located at:

```text
.github/workflows/ci.yml
```

It runs for:

- pushes to `main`;
- pushes to branches matching `feature/**`;
- pull requests targeting `main`.

The CI job:

1. checks out the repository;
2. configures Temurin Java 17;
3. restores the Maven dependency cache;
4. runs:

   ```bash
   mvn --batch-mode --no-transfer-progress clean verify
   ```

5. builds the Docker image:

   ```bash
   docker build --tag booking-system:ci .
   ```

A change is considered verified only when both the Maven build and Docker image build succeed.

## Docker Image

The project uses a multi-stage Dockerfile.

The build stage:

- uses Maven with Java 17;
- builds the multi-module project;
- packages the executable `booking-api.jar`.

The runtime stage:

- uses a smaller Java 17 JRE image;
- copies only the packaged application;
- runs the process as a non-root `booking` user;
- exposes port `8080`.

## Demonstrated End-to-End Flow

The application has been manually verified with the following sequence:

```text
Register user
    ->
Authenticate and receive JWT
    ->
Promote local administrator
    ->
Create bookable service
    ->
Register regular user
    ->
Create booking
    ->
Confirm booking as administrator
    ->
Cancel booking as owner
    ->
Delete cancelled booking as administrator
    ->
Verify that the deleted booking returns 404
```

## Design Decisions

Important architectural decisions are documented under:

```text
docs/adr
```

Current ADRs:

- [ADR 0001: Booking domain rules and persistence contract](docs/adr/0001-booking-domain-and-persistence-contract.md)

## Project Purpose

This project was built as a backend engineering portfolio project.

It demonstrates practical experience with:

- Java and Spring Boot backend development;
- modular and layered architecture;
- domain-oriented business logic;
- REST API design;
- authentication and authorization;
- relational persistence;
- database migrations;
- concurrency control;
- unified error-contract design;
- OpenAPI and Swagger documentation;
- automated testing with Testcontainers;
- multi-stage Docker builds and Docker Compose;
- continuous integration with GitHub Actions.

## Development workflow

1. Create a separate branch for each task.
2. Make changes and run relevant tests.
3. Commit and push the branch to GitHub.
4. Open a Pull Request for review.
