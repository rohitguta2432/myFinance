---
description: How to add a new entity (e.g., Loan, SIP, Mutual Fund) end-to-end across backend and frontend
---

# Add Entity Workflow

Follow these steps exactly when adding a new domain entity to myFinance.

> **Golden Rule**: ALL business logic / calculations go in backend services. Frontend hooks only call APIs and format display values.

## Backend (Spring Boot)

### 1. Create Entity — `backend/src/main/java/com/myfinance/model/`
```java
@Entity
@Table(name = "new_entities")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NewEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank private String name;
    @NotNull private BigDecimal amount;
    // add domain fields
}
```

### 2. Create DTO — `backend/src/main/java/com/myfinance/dto/`
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NewEntityDTO {
    private Long id;
    private String name;
    private Double amount;
}
```

### 3. Create Repository — `backend/src/main/java/com/myfinance/repository/`
```java
public interface NewEntityRepository extends JpaRepository<NewEntity, Long> {}
```

### 4. Add to Existing Service OR Create New Service
**If the entity belongs to an existing step** (e.g., adding SIP under Goals):
- Add methods to `GoalService.java`

**If it needs a new step/domain** (e.g., adding Loans):
- Create `LoanService.java` with structured logging:
```java
@Service @RequiredArgsConstructor @Slf4j
public class LoanService {
    private final LoanRepository loanRepo;

    @Transactional(readOnly = true)
    public List<LoanDTO> getLoans() {
        log.info("loans.get started");
        var loans = loanRepo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
        log.info("loans.get.success count={}", loans.size());
        return loans;
    }

    @Transactional
    public LoanDTO addLoan(LoanDTO dto) {
        log.info("loans.add name={} amount={}", dto.getName(), dto.getAmount());
        Loan loan = Loan.builder().name(dto.getName()).amount(dto.getAmount()).build();
        LoanDTO saved = toDTO(loanRepo.save(loan));
        log.info("loans.add.success id={}", saved.getId());
        return saved;
    }
}
```

### 5. Add Endpoints — Existing or New Controller
**If existing step**: Add to the relevant controller (e.g., `GoalController.java`)
**If new step**: Create a new controller:
```java
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;

    @GetMapping
    public ResponseEntity<List<LoanDTO>> getLoans() {
        return ResponseEntity.ok(loanService.getLoans());
    }

    @PostMapping
    public ResponseEntity<LoanDTO> addLoan(@RequestBody LoanDTO dto) {
        return ResponseEntity.ok(loanService.addLoan(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        loanService.deleteLoan(id);
        return ResponseEntity.ok().build();
    }
}
```

## Frontend (React + Zustand)

### 6. Add API Paths — `src/features/assessment/services/assessmentApi.js`
```javascript
export const fetchLoans = () => api.get('/loans');
export const addLoan = (data) => api.post('/loans', data);
export const deleteLoan = (id) => api.delete(`/loans/${id}`);
```

### 7. Add to Zustand Store — `src/features/assessment/store/useAssessmentStore.js`
```javascript
loans: [],
addLoan: (loan) => set((state) => ({ loans: [...state.loans, loan] })),
removeLoan: (id) => set((state) => ({ loans: state.loans.filter(e => e.id !== id) })),
updateLoan: (id, updates) => set((state) => ({
    loans: state.loans.map(e => e.id === id ? { ...e, ...updates } : e)
})),
```

### 8. Create/Update Wizard Step Page — `src/features/assessment/pages/`
- Follow existing pattern from `Step3AssetsLiabilities.jsx`
- Use Tailwind classes + `clsx` for styling
- Wire to Zustand store actions

### 9. Add Dashboard Analysis (Backend API, NOT frontend hook)
```java
// ✅ CORRECT — analysis in backend service
@GetMapping("/analysis")
public ResponseEntity<LoanAnalysisDTO> getLoanAnalysis() {
    return ResponseEntity.ok(loanService.analyzeLoanHealth());
}
```
```javascript
// ❌ WRONG — do NOT put calculations in frontend hooks
// export const useLoanAnalysis = () => { useMemo(() => { ... }) }
```

## Checklist
- [ ] Entity with `@Data @Builder`
- [ ] DTO with `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- [ ] Repository extends `JpaRepository`
- [ ] Service methods with `@Transactional` + structured logging (`domain.entity.action key=value`)
- [ ] Controller endpoints (thin — delegates to service)
- [ ] Frontend API paths in `assessmentApi.js`
- [ ] Zustand store: add/remove/update actions
- [ ] UI page with Tailwind styling
- [ ] Business logic / analysis in backend service (NOT frontend hooks)
