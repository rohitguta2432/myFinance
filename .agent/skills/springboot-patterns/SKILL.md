---
name: springboot-patterns
description: Spring Boot 3.4.1 + Java 21 patterns for myFinance backend. Step-based domain controllers, structured logging, backend-first business logic. H2 dev / PostgreSQL prod, Lombok DTOs, AWS Bedrock AI chat. No Spring Security.
origin: ECC (adapted for myFinance)
---

# Spring Boot Development Patterns — myFinance

Spring Boot patterns matching the actual myFinance backend architecture.

## When to Activate

- Building REST APIs under `/api/v1/{domain}`
- Adding new entities or step-based domain controllers
- Implementing business logic (calculations, analysis, scoring)
- Working with the dual DB strategy (H2 dev / PostgreSQL prod)
- Integrating with AWS Bedrock for AI chat

## ⚠️ Golden Rule: Backend Owns Business Logic

**ALL business logic, calculations, and analysis MUST live in backend services — NOT in frontend hooks.**

| Logic Type | Backend (✅ Correct) | Frontend (❌ Wrong) |
|---|---|---|
| Financial health score | `HealthScoreService.java` | ~~`useFinancialHealthScore.js`~~ |
| Tax analysis (Old vs New regime) | `TaxAnalysisService.java` | ~~`useTaxAnalysis.js`~~ |
| Insurance gap analysis | `InsuranceAnalysisService.java` | ~~`useInsuranceAnalysis.js`~~ |
| Red flags & priority actions | `AnalyticsService.java` | ~~`useRedFlags.js`, `usePriorityActions.js`~~ |
| Personalised benchmarks | `BenchmarkService.java` | ~~`usePersonalisedBenchmarks.js`~~ |
| Time machine projections | `ProjectionService.java` | ~~`useTimeMachine.js`~~ |
| Currency formatting / display | OK in frontend | — |

> Frontend hooks should only: (1) call backend APIs, (2) manage UI state, (3) format display values.

## Actual Project Structure

```
backend/src/main/java/com/myfinance/
├── MyFinanceApplication.java
├── controller/                         ← 1 controller per wizard step
│   ├── ProfileController.java          ← Step 1: /api/v1/profile
│   ├── CashFlowController.java         ← Step 2: /api/v1/cashflow
│   ├── NetWorthController.java         ← Step 3: /api/v1/networth
│   ├── GoalController.java             ← Step 4: /api/v1/goals
│   ├── InsuranceController.java        ← Step 5: /api/v1/insurance
│   ├── TaxController.java              ← Step 6: /api/v1/tax
│   └── ChatController.java             ← AI chat (Bedrock)
├── service/                            ← 1 service per domain
│   ├── ProfileService.java
│   ├── CashFlowService.java
│   ├── NetWorthService.java
│   ├── GoalService.java
│   ├── InsuranceService.java
│   ├── TaxService.java
│   └── BedrockChatService.java
├── repository/                         ← 1 repo per entity
│   ├── ProfileRepository.java
│   ├── IncomeRepository.java
│   ├── ExpenseRepository.java
│   ├── AssetRepository.java
│   ├── LiabilityRepository.java
│   ├── GoalRepository.java
│   ├── InsuranceRepository.java
│   └── TaxRepository.java
├── model/                              ← JPA entities (@Data, @Builder)
├── dto/                                ← Lombok DTOs
└── util/
    ├── EnumUtils.java                  ← Safe enum parsing
    └── JsonUtils.java                  ← JSON ↔ Map utilities
```

## Step-Based API Pattern

Each wizard step has its own controller with a dedicated base path:

```
/api/v1/profile      → ProfileController      (GET, POST)
/api/v1/cashflow     → CashFlowController      (GET, POST /income, POST /expense, DELETE /income/{id}, DELETE /expense/{id})
/api/v1/networth     → NetWorthController       (GET, POST /asset, POST /liability, DELETE /asset/{id}, DELETE /liability/{id})
/api/v1/goals        → GoalController           (GET, POST, DELETE /{id})
/api/v1/insurance    → InsuranceController       (GET, POST, DELETE /{id})
/api/v1/tax          → TaxController             (GET, POST)
```

### Controller Template (Thin — delegates to service)

```java
@RestController
@RequestMapping("/api/v1/cashflow")
@RequiredArgsConstructor
public class CashFlowController {
    private final CashFlowService cashFlowService;

    @GetMapping
    public ResponseEntity<FinancialsResponse> getCashFlow() {
        return ResponseEntity.ok(cashFlowService.getCashFlow());
    }

    @PostMapping("/income")
    public ResponseEntity<IncomeDTO> addIncome(@RequestBody IncomeDTO dto) {
        return ResponseEntity.ok(cashFlowService.addIncome(dto));
    }

    @DeleteMapping("/income/{id}")
    public ResponseEntity<Void> deleteIncome(@PathVariable Long id) {
        cashFlowService.deleteIncome(id);
        return ResponseEntity.ok().build();
    }
}
```

### Service Template (Business logic + structured logging)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CashFlowService {
    private final IncomeRepository incomeRepo;
    private final ExpenseRepository expenseRepo;

    @Transactional(readOnly = true)
    public FinancialsResponse getCashFlow() {
        log.info("cashflow.get started");
        var incomes = incomeRepo.findAll().stream().map(this::toIncomeDTO).collect(Collectors.toList());
        var expenses = expenseRepo.findAll().stream().map(this::toExpenseDTO).collect(Collectors.toList());
        log.info("cashflow.get.success incomes={} expenses={}", incomes.size(), expenses.size());
        return FinancialsResponse.builder().incomes(incomes).expenses(expenses).build();
    }

    @Transactional
    public IncomeDTO addIncome(IncomeDTO dto) {
        log.info("cashflow.income.add source={} amount={} frequency={}",
                dto.getSourceName(), dto.getAmount(), dto.getFrequency());
        Income income = Income.builder()
                .sourceName(dto.getSourceName())
                .amount(dto.getAmount())
                .frequency(EnumUtils.safeEnum(Frequency.class, dto.getFrequency()))
                .build();
        IncomeDTO saved = toIncomeDTO(incomeRepo.save(income));
        log.info("cashflow.income.add.success id={}", saved.getId());
        return saved;
    }
}
```

## Structured Logging Pattern

All services use `domain.entity.action key=value` format:

```
profile.save age=30 city=Mumbai              ← action started
profile.save.success id=1                    ← action completed
cashflow.income.add source=Salary amount=100000.0 frequency=MONTHLY
cashflow.income.add.success id=1
networth.asset.add type=Equity name=Stocks value=500000.0
goals.add type=Retirement name=Retire at 60
```

Rules:
- **Start log**: `domain.entity.action` + relevant params
- **Success log**: `domain.entity.action.success` + id or count
- **Error log**: `domain.entity.action.failed` + exception message
- Use `@Slf4j` Lombok annotation — never manual LoggerFactory

## Lombok DTOs (NOT Java Records)

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IncomeDTO {
    private Long id;
    private String sourceName;
    private Double amount;
    private String frequency;
    private Boolean taxDeducted;
    private Double tdsPercentage;
}
```

## Enum Handling (EnumUtils)

```java
// Safe parsing — returns null instead of throwing on unknown values
Frequency freq = EnumUtils.safeEnum(Frequency.class, dto.getFrequency());

// Safe toString — returns null for null enums
String name = EnumUtils.enumName(entity.getFrequency());
```

## Dual Database Strategy

```yaml
# DEV — application.yml (default)
spring:
  datasource:
    url: jdbc:h2:mem:myfinance
  jpa:
    hibernate.ddl-auto: update

# PROD — via Docker Compose environment variables
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/myfinance
SPRING_DATASOURCE_DRIVER_CLASS_NAME: org.postgresql.Driver
```

## Frontend API Layer

```javascript
// src/services/api.js — native fetch wrapper
const BASE_URL = '/api/v1';
export const api = {
    get: (endpoint) => request(endpoint, { method: 'GET' }),
    post: (endpoint, data) => request(endpoint, { method: 'POST', body: JSON.stringify(data) }),
    put: (endpoint, data) => request(endpoint, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (endpoint) => request(endpoint, { method: 'DELETE' }),
};

// src/features/assessment/services/assessmentApi.js — domain-specific paths
fetchProfile → api.get('/profile')
fetchFinancials → api.get('/cashflow')
addIncome → api.post('/cashflow/income', data)
```

## Production Defaults

- Use `@RequiredArgsConstructor` (Lombok) for DI — avoid `@Autowired`
- Use `@Transactional(readOnly = true)` for queries
- Use `EnumUtils.safeEnum()` for all enum parsing — never raw `Enum.valueOf()`
- Keep controllers thin (max ~5 lines per method)
- Server runs on port 8081 (mapped to 8080 via Docker)
- No Spring Security — endpoints are open

**Remember**: Controllers delegate, services own logic, repositories stay simple, logging is structured.
