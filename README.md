# Chat Backend API

This repository contains the RESTful backend implementation for a chat application. Developed as a final project targeting a comprehensive real-world backend architecture, it provides complete functionality for managing users, conversations, and messages. The application follows layered architectural patterns, utilizing Spring Boot and Spring Data JPA.

## Technologies Used

*   **Java 17**
*   **Spring Boot 3.5.11**
*   **Spring Security** for authentication and authorization
*   **Spring Data JPA** with Hibernate ORM
*   **H2 In-Memory Database** (configured for development and testing)
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

## Setup and Execution

1.  **Clone the repository and enter the directory.**
2.  **Execute the Maven Wrapper:**
    To run the application locally on port 8080:
    ```bash
    ./mvnw spring-boot:run
    ```
    If using Windows:
    ```cmd
    .\mvnw.cmd spring-boot:run
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

## API Documentation

Once the application is running, the Swagger UI can be accessed to interact directly with the REST endpoints.
Navigate to: `http://localhost:8080/swagger-ui.html`

## Postman Collection

A `Chat_Backend.postman_collection.json` file is included in the root directory. It can be imported directly into Postman for comprehensive testing of all endpoints.
