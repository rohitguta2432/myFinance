---
name: springboot-patterns
description: Spring Boot 3.4.1 + Java 21 patterns for myFinance backend. H2 dev / PostgreSQL prod, Lombok DTOs, Assessment CRUD, AWS Bedrock AI chat. No Spring Security.
origin: ECC (adapted for myFinance)
---

# Spring Boot Development Patterns — myFinance

Spring Boot patterns matching the actual myFinance backend architecture.

## When to Activate

- Building REST APIs under `/api/v1/assessment`
- Structuring controller → service → repository layers
- Adding new entity types (profiles, incomes, expenses, assets, liabilities, goals, insurance, tax)
- Working with the dual DB strategy (H2 dev / PostgreSQL prod)
- Integrating with AWS Bedrock for AI chat

## Actual Project Structure

```
backend/src/main/java/com/myfinance/
├── MyFinanceApplication.java
├── controller/
│   ├── AssessmentController.java    ← @RestController
│   └── ChatController.java          ← @RestController (Bedrock AI)
├── service/
│   ├── AssessmentService.java        ← @Service
│   └── BedrockChatService.java       ← @Service (AWS SDK)
├── repository/
│   ├── ProfileRepository.java        ← JpaRepository
│   ├── IncomeRepository.java
│   ├── ExpenseRepository.java
│   ├── AssetRepository.java
│   ├── LiabilityRepository.java
│   ├── GoalRepository.java
│   ├── InsuranceRepository.java
│   └── TaxRepository.java
└── model/  (entities with @Data, @Builder, @Entity)
```

## REST API Pattern (Actual)

```java
@RestController
@RequestMapping("/api/v1/assessment")
@Validated
class AssessmentController {
    private final AssessmentService assessmentService;

    AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @PostMapping("/profile")
    ResponseEntity<ProfileResponse> saveProfile(@Valid @RequestBody CreateProfileRequest request) {
        Profile profile = assessmentService.saveProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProfileResponse.from(profile));
    }

    @GetMapping("/profile/{id}")
    ResponseEntity<ProfileResponse> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(ProfileResponse.from(assessmentService.getProfile(id)));
    }
}
```

## Lombok DTOs (NOT Java Records)

This project uses **Lombok** — not Java records for DTOs/entities:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProfileRequest {
    @NotBlank @Size(max = 100) private String city;
    @NotNull private Integer age;
    @NotBlank private String maritalStatus;
    @NotNull private Integer dependents;
    @NotBlank private String employmentType;
    @NotBlank private String residencyStatus;
    @NotBlank private String riskTolerance;
}
```

## Repository Pattern (Spring Data JPA)

```java
public interface AssetRepository extends JpaRepository<AssetEntity, Long> {
    List<AssetEntity> findByProfileId(Long profileId);
}
```

## Service Layer with Transactions

```java
@Service
public class AssessmentService {
    private final ProfileRepository profileRepo;
    private final AssetRepository assetRepo;

    public AssessmentService(ProfileRepository profileRepo, AssetRepository assetRepo) {
        this.profileRepo = profileRepo;
        this.assetRepo = assetRepo;
    }

    @Transactional
    public Profile saveProfile(CreateProfileRequest request) {
        ProfileEntity entity = ProfileEntity.from(request);
        return Profile.from(profileRepo.save(entity));
    }

    @Transactional(readOnly = true)
    public Profile getProfile(Long id) {
        return profileRepo.findById(id)
                .map(Profile::from)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));
    }
}
```

## Exception Handling

```java
@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiError.validation(message));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("Internal server error"));
    }
}
```

## Dual Database Strategy

```yaml
# DEV — application.yml (default)
spring:
  datasource:
    url: jdbc:h2:mem:myfinance
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate.ddl-auto: update

# PROD — via Docker Compose environment variables
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/myfinance
SPRING_DATASOURCE_DRIVER_CLASS_NAME: org.postgresql.Driver
SPRING_JPA_DATABASE_PLATFORM: org.hibernate.dialect.PostgreSQLDialect
```

## AWS Bedrock AI Chat Pattern

```java
@Service
public class BedrockChatService {
    private final BedrockRuntimeClient bedrockClient;

    // Uses AWS SDK credential chain — NEVER hardcode keys
    // Config: aws.bedrock.region=us-east-1, aws.bedrock.model-id=amazon.nova-lite-v1:0
}
```

## Logging (SLF4J with Lombok)

```java
@Slf4j
@Service
public class AssessmentService {
    public Profile saveProfile(CreateProfileRequest request) {
        log.info("save_profile city={} age={}", request.getCity(), request.getAge());
        try {
            // logic
        } catch (Exception ex) {
            log.error("save_profile_failed", ex);
            throw ex;
        }
    }
}
```

## Production Defaults

- Prefer constructor injection, avoid field injection
- Use Lombok (`@Data`, `@Builder`, `@Slf4j`) — project standard
- Configure HikariCP pool sizes for PostgreSQL in prod
- Use `@Transactional(readOnly = true)` for queries
- Server runs on port 8081 (mapped to 8080 via Docker)

**Remember**: Keep controllers thin, services focused, repositories simple, and errors handled centrally.
