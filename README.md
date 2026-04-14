# Chat Backend API

This repository contains the RESTful backend implementation for a chat application. Developed as a final project targeting a comprehensive real-world backend architecture, it provides complete functionality for managing users, conversations, and messages. The application follows layered architectural patterns, utilizing Spring Boot and Spring Data JPA.

## Technologies Used

*   **Java 17**
*   **Spring Boot 3.5.11**
*   **Spring Security** for authentication and authorization
*   **Spring Data JPA** with Hibernate ORM
*   **H2 In-Memory Database** (default profile — development & testing, no setup required)
*   **PostgreSQL** (production profile — persistent, production-ready database)
*   **JJWT** for stateless JWT token validation
*   **Maven** for dependency management and build execution
*   **JUnit 5 and MockMvc** for unit and integration testing
*   **Swagger / OpenAPI 3** for interactive API documentation

## Implemented Features

1.  **Authentication & Authorization:**
    *   JWT-based stateless authentication flow.
    *   Role-Based Access Control (RBAC) ensuring administrative endpoints are protected.
    *   Passwords securely hashed using BCrypt.

2.  **User Management:**
    *   Registration and secure login capabilities.
    *   Endpoints to update account details and soft-delete/remove users.

3.  **Real-Time Chat Data Model:**
    *   Support for multi-participant conversations.
    *   Message sending and retrieval logic linked accurately to conversations.

4.  **Pagination & Sorting:**
    *   Integrated Spring Data `Pageable` across relevant endpoints to ensure performance at scale.
    *   Retrieval lists for users, conversations, and messages are paginated and sorted.

5.  **Audit Trail:**
    *   Utilizes Spring Data JPA Auditing to automatically track `createdAt` and `updatedAt` timestamps across entities.

6.  **Soft Deletion:**
    *   Message deletion is implemented as a soft delete (`deleted=true`) utilizing Hibernate's `@SQLDelete` and `@SQLRestriction`.

## Database Configuration

The application supports two database profiles, selected via the `spring.profiles.active` property.

| Profile | Database | Data Persistence | Setup Required |
|---|---|---|---|
| `h2` *(default)* | H2 In-Memory | Resets on restart | ❌ None |
| `postgres` | PostgreSQL | Persistent | ✅ See below |

### Running with H2 (default)

No configuration needed — just run:

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
.\mvnw.cmd spring-boot:run
```

H2 web console is available at `http://localhost:8080/h2-console`
- **JDBC URL:** `jdbc:h2:mem:chatdb`
- **Username:** `sa`  |  **Password:** *(leave empty)*

### Running with PostgreSQL

1. Make sure PostgreSQL is installed and running.
2. Create the database:
   ```sql
   CREATE DATABASE chatdb;
   ```
3. Set your credentials via environment variables (recommended) or edit `application-postgres.properties`:

   ```bash
   # Linux / macOS
   export DB_URL=jdbc:postgresql://localhost:5432/chatdb
   export DB_USERNAME=postgres
   export DB_PASSWORD=your_password
   ./mvnw spring-boot:run -Dspring.profiles.active=postgres
   ```

   ```powershell
   # Windows (PowerShell)
   $env:DB_URL="jdbc:postgresql://localhost:5432/chatdb"
   $env:DB_USERNAME="postgres"
   $env:DB_PASSWORD="your_password"
   .\mvnw.cmd spring-boot:run -Dspring.profiles.active=postgres
   ```

> **Note:** Never commit credentials directly. Use environment variables or a local `application-local.properties` file (already git-ignored).

## Frontend (Vanilla JS UI)

This project already includes a simple frontend built with Vanilla JavaScript.
The UI is served by Spring Boot from `src/main/resources/static`.

### How to open the frontend

1. Start the backend:

```powershell
.\mvnw.cmd spring-boot:run
```

2. Open your browser and go to:

`http://localhost:8080/`

### Important notes

- Do not open `index.html` directly from the file system. Open it through Spring Boot at `http://localhost:8080/`.
- If PostgreSQL is not available locally, run with the H2 profile:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2
```

## Sample Credentials for Testing

When the application starts (except with the `test` profile), default users are seeded automatically.

*   **Admin User**
    *   Username: `admin`
    *   Email: `admin@test.com`
    *   Password: `password123`
*   **Regular User**
    *   Username: `demo`
    *   Email: `user@test.com`
    *   Password: `password123`

Use the username and password pair in `/api/auth/login`.

## Testing

The project is thoroughly tested using JUnit 5, Mockito, and MockMvc for integration tests.

To execute the test suite:
```bash
./mvnw clean test
```

## Test Coverage Report (JaCoCo)

JaCoCo is configured in Maven to generate coverage reports and validate minimum thresholds.

*   **Overall coverage goal:** 60%+ (line coverage)
*   **Service layer coverage goal:** 70%+ (line coverage for `com/chatapp/chat_backend/service`)

Run coverage checks:

```bash
./mvnw verify
```

Coverage report output:

*   HTML report: `target/site/jacoco/index.html`
*   XML report: `target/site/jacoco/jacoco.xml`

## API Documentation

Once the application is running, the Swagger UI can be accessed to interact directly with the REST endpoints.
Navigate to: `http://localhost:8080/swagger-ui/index.html`

## Authentication

The API uses a **short-lived Access Token + long-lived Refresh Token** flow.

### POST /api/auth/register

```http
POST /api/auth/register
Content-Type: application/json

{ "username": "alice", "email": "alice@example.com", "password": "password123" }
```

### POST /api/auth/login

```http
POST /api/auth/login
Content-Type: application/json

{ "username": "alice", "password": "password123" }
```

**Response:**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

### POST /api/auth/refresh

Exchanges a refresh token for a **new access token and a rotated refresh token** (old refresh token is revoked).

```http
POST /api/auth/refresh
Content-Type: application/json

{ "refreshToken": "550e8400-e29b-41d4-a716-446655440000" }
```

**Response:**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "new-uuid-..."
}
```

### POST /api/auth/logout

Revokes the refresh token. The access token expires on its own (24 h TTL).

```http
POST /api/auth/logout
Content-Type: application/json

{ "refreshToken": "550e8400-e29b-41d4-a716-446655440000" }
```

**Response:** `204 No Content`

### Using the access token

Include the `accessToken` value as a Bearer token in all protected API calls:

```http
Authorization: Bearer <accessToken>
```

## Postman Collection

A `Chat_Backend_API.postman_collection.json` file is included in the root directory. It can be imported directly into Postman for comprehensive testing of all endpoints. The Login request stores `accessToken` and `refreshToken` automatically into collection variables used by subsequent requests.
