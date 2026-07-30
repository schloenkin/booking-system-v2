# Booking System v2

A production-style backend portfolio project built with Java and Spring Boot.

Booking System v2 provides secure management of users, bookable services, and bookings. The project demonstrates modular architecture, domain-driven business rules, JWT authentication, role-based and object-level authorization, PostgreSQL persistence, database migrations, concurrency control, and automated testing.

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
- Consistent API error responses
- Unit, controller, security, integration, and concurrency tests
- PostgreSQL integration tests with Testcontainers
- Docker Compose setup for local PostgreSQL

## Technology Stack

- Java 17
- Spring Boot 3.5.14
- Spring Web
- Spring Security
- JWT with JJWT 0.13.0
- Spring Data JPA
- PostgreSQL 17
- Flyway
- Docker Compose
- Maven
- JUnit 5
- Mockito
- MockMvc
- Testcontainers

## Architecture

The project is organized as a multi-module Maven application:

```text
booking-system-v2
|-- booking-domain
|-- booking-application
|-- booking-infrastructure
|-- booking-api
|-- docs
|   `-- adr
|-- compose.yaml
|-- request.http
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

Contains the HTTP and security entry points.

Examples:

- REST controllers
- request and response DTOs
- validation
- Spring Security configuration
- JWT authentication filter
- global exception handling
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

## Running the Project Locally

### Prerequisites

- Java 17
- Docker Desktop or another Docker-compatible runtime
- Maven, or an IDE with Maven support
- Git

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

Example PowerShell command:

```powershell
Copy-Item .env.example .env
```

Set secure local values in `.env`.

Required variables:

```properties
POSTGRES_DB=booking_db
POSTGRES_USER=booking_user
POSTGRES_PASSWORD=change_me
JWT_SECRET=replace_with_a_long_random_secret_of_at_least_32_characters
JWT_EXPIRATION_MS=3600000
```

The real `.env` file is excluded from Git.

### 3. Start PostgreSQL

```bash
docker compose up -d
```

PostgreSQL is exposed locally on:

```text
localhost:5433
```

### 4. Build the project

With Maven installed:

```bash
mvn clean install
```

Alternatively, use the root Maven project in the IDE:

```text
booking-system-v2 -> Lifecycle -> clean
booking-system-v2 -> Lifecycle -> install
```

### 5. Run the application

Run:

```text
BookingApiApplication
```

The API starts at:

```text
http://localhost:8080
```

Health check:

```text
http://localhost:8080/api/health
```

## Creating a Local Administrator

Registration creates users with the `USER` role.

For a local demonstration, first register a user:

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
- concurrency and optimistic-locking tests;
- Testcontainers-based end-to-end integration tests.

Run the full test suite with:

```bash
mvn clean install
```

Docker must be running for tests that start PostgreSQL containers.

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

## Error Handling

The API provides centralized exception handling for cases such as:

- validation failures;
- missing resources;
- duplicate users;
- invalid credentials;
- forbidden access;
- conflicting bookings;
- invalid booking status transitions;
- forbidden booking deletion;
- concurrent update conflicts.

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
- automated testing;
- Docker-based local infrastructure.