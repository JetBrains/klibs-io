# klibs.io Development Guidelines

This document provides guidelines and instructions for developing and testing the klibs.io project.

## Junie Task Protocol

- Ask clarifying questions whenever requirements are ambiguous or incomplete.
- Ask for plan approval only after presenting a concrete plan.
- If no edits are proposed (analysis-only), skip plan approval and provide the answer.
- Do not request confirmation when there is no plan shown.
- Summarize the agreed plan before making changes and wait for approval.

## Build/Configuration Instructions

### Prerequisites

- JDK 21 or higher
- Docker (for running tests with Testcontainers)
- PostgreSQL 14-16 (for local development)

### Building the Project

The project uses Gradle as its build system. To build the project:

```bash
./gradlew build
```

To build without running tests:

```bash
./gradlew build -x test
```

To build a runnable JAR:

```bash
./gradlew bootJar
```

### Configuration

The application uses Spring profiles to run in different environments:

- **prod**: For production use, with restricted debug utilities
- **local**: For local development and testing

Key configuration files:

- `app/src/main/resources/application.yml`: Base configuration
- `app/src/main/resources/application-local.yml`: Local development configuration
- `app/src/main/resources/application-prod.yml`: Production configuration

Important configuration properties:

1. **Database Configuration**:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/postgres
       username: postgres
       password: your-password
   ```

2. **API Tokens**:
   ```yaml
   klibs:
     integration:
       github:
         personal-access-token: your-github-token
     spring:
       ai:
         openai:
           api-key: your-openai-api-key
   ```

3. **Indexing Configuration**:
   ```yaml
   klibs:
     indexing: true  # Enable/disable indexing
     indexing-configuration:
       executor:
         thread-count: 2
       central-sonatype:
         enabled: true
       gmaven:
         enabled: true
   ```

4. **Cache Directories**:
   ```yaml
   klibs:
     readme:
       cache-dir: ${user.dir}/cache/readme
     integration:
       github:
         cache:
           request-cache-path: ${user.dir}/cache/request-cache
           request-cache-size-mb: 100
   ```

## Testing Information

### Test Configuration

The project uses JUnit 5 for testing, along with:
- Spring Boot Test for integration testing
- Testcontainers for database testing
- MockMvc for testing web endpoints

Test configuration is in `app/src/test/resources/application-test.yml`.

### Running Tests

To run all tests:

```bash
./gradlew test
```

To run tests in a specific module:

```bash
./gradlew :app:test
./gradlew :core:package:test
# etc.
```

To run a specific test class:

```bash
./gradlew :app:test --tests "io.klibs.app.example.SimpleExampleTest"
```

### Types of Tests

1. **Unit Tests**: Simple tests that don't require external dependencies.
   
   Example:
   ```kotlin
   class SimpleExampleTest {
       @Test
       fun `simple test`() {
           val result = 2 + 2
           assertEquals(4, result)
       }
   }
   ```

2. **Integration Tests with Database**: Tests that require a database, using Testcontainers.
   
   Example:
   ```kotlin
   class DatabaseTest : BaseUnitWithDbLayerTest() {
       @Autowired
       private lateinit var packageRepository: PackageRepository

       @Test
       fun `repository operations`() {
           // Test repository operations
       }
   }
   ```

3. **Web Tests**: Tests for REST API endpoints using MockMvc.
   
   Example:
   ```kotlin
   class ApiTest : SmokeTestBase() {
       @Test
       fun `endpoint returns 200`() {
           mockMvc.get("/api/endpoint")
               .andExpect {
                   status { isOk() }
               }
       }
   }
   ```

### Adding New Tests

1. Decide on the type of test you need (unit, integration, web)
2. Create a new test class in the appropriate module's test directory
3. Extend the appropriate base class if needed:
   - `BaseUnitWithDbLayerTest` for database tests
   - `SmokeTestBase` for web tests
4. Write your test methods using JUnit 5 annotations and assertions
5. Run your tests to verify they work

## Additional Development Information

### Code Style

The project follows Kotlin coding conventions. Key points:

- Use 4 spaces for indentation
- Maximum line length is 120 characters
- Use camelCase for variables and functions, PascalCase for classes
- Use meaningful names for classes, methods, and variables
- Add KDoc comments for public APIs

### Module Structure

The project follows a "module by feature" approach:

- **app**: Main server module, glue for all other modules
- **core/package**: Maven packages handling
- **core/project**: Project entity management
- **core/scm-owner**: SCM repository owners management
- **core/scm-repository**: Git repositories handling
- **core/search**: Search functionality
- **integrations/ai**: OpenAI integration
- **integrations/github**: GitHub integration
- **integrations/maven**: Maven Central integration

When adding new features, consider which module they belong to, or whether a new module is needed.

### Database Migrations

The project uses Liquibase for database migrations. Migration files are in `app/src/main/resources/db/migration/`.

To add a new migration:
1. Create a new changelog file in the appropriate directory
2. Add your changes (tables, columns, indexes, etc.)
3. Include your changelog in the master changelog file

### Debugging

For local development:
Enable SQL logging with `spring.jpa.show-sql: true`

### Common Issues

1. **Docker not available**: Ensure Docker is running for tests that use Testcontainers
2. **Database connection issues**: Verify PostgreSQL is running and credentials are correct
3. **API rate limiting**: GitHub and OpenAI have rate limits; use caching where possible