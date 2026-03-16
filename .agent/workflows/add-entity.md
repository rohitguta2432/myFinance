---
description: How to add a new entity (e.g., Loan, SIP, Mutual Fund) end-to-end across backend and frontend
---

# Add Entity Workflow

Follow these steps exactly when adding a new domain entity to myFinance.

## Backend (Spring Boot)

### 1. Create Entity — `backend/src/main/java/com/myfinance/model/`
```java
@Entity
@Table(name = "new_entities")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NewEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @NotBlank private String name;
    @NotNull private BigDecimal amount;
    // add domain fields
}
```

### 2. Create Repository — `backend/src/main/java/com/myfinance/repository/`
```java
public interface NewEntityRepository extends JpaRepository<NewEntity, Long> {
    List<NewEntity> findByProfileId(Long profileId);
}
```

### 3. Add to AssessmentService — `backend/src/main/java/com/myfinance/service/AssessmentService.java`
```java
@Transactional
public NewEntity saveNewEntity(Long profileId, CreateNewEntityRequest request) { ... }

@Transactional(readOnly = true)
public List<NewEntity> getNewEntities(Long profileId) { ... }
```

### 4. Add Endpoints — `backend/src/main/java/com/myfinance/controller/AssessmentController.java`
```java
@PostMapping("/profile/{profileId}/new-entities")
ResponseEntity<NewEntity> addNewEntity(@PathVariable Long profileId, @Valid @RequestBody CreateNewEntityRequest req) { ... }

@GetMapping("/profile/{profileId}/new-entities")
ResponseEntity<List<NewEntity>> getNewEntities(@PathVariable Long profileId) { ... }
```

## Frontend (React + Zustand)

### 5. Add to Zustand Store — `src/features/assessment/store/useAssessmentStore.js`
```javascript
newEntities: [],
addNewEntity: (entity) => set((state) => ({ newEntities: [...state.newEntities, entity] })),
removeNewEntity: (id) => set((state) => ({ newEntities: state.newEntities.filter(e => e.id !== id) })),
updateNewEntity: (id, updates) => set((state) => ({
    newEntities: state.newEntities.map(e => e.id === id ? { ...e, ...updates } : e)
})),
```

### 6. Create Wizard Step Page (if needed) — `src/features/assessment/pages/`
- Follow existing pattern from `Step3AssetsLiabilities.jsx`
- Use Tailwind classes + `clsx` for styling
- Wire to Zustand store actions

### 7. Add Dashboard Analysis Hook (if needed) — `src/hooks/`
```javascript
export const useNewEntityAnalysis = () => {
    const { newEntities } = useAssessmentStore();
    return useMemo(() => {
        // derived calculations here
    }, [newEntities]);
};
```

## Checklist
- [ ] Entity with `@Data @Builder` + relationships
- [ ] Repository extends `JpaRepository`
- [ ] Service methods with `@Transactional`
- [ ] Controller endpoints with `@Valid`
- [ ] Zustand store: add/remove/update actions
- [ ] UI page with Tailwind styling
- [ ] `useMemo` hook for derived calculations
