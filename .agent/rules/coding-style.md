# Coding Style

## Immutability (CRITICAL)

ALWAYS create new objects, NEVER mutate existing ones:

```javascript
// Zustand store — CORRECT immutable pattern
set((state) => ({ items: [...state.items, newItem] }))
set((state) => ({ items: state.items.filter(i => i.id !== id) }))
set((state) => ({ items: state.items.map(i => i.id === id ? { ...i, ...updates } : i) }))

// WRONG — direct mutation
state.items.push(newItem)  // ❌ Never do this
```

Rationale: Immutable data prevents hidden side effects, makes debugging easier, and Zustand's reactivity depends on new references.

## File Organization

MANY SMALL FILES > FEW LARGE FILES:
- High cohesion, low coupling
- 200-400 lines typical, 800 max
- Extract utilities from large modules
- Organize by feature/domain, not by type

## Error Handling

ALWAYS handle errors comprehensively:
- Handle errors explicitly at every level
- Provide user-friendly error messages via `react-hot-toast`
- Log detailed error context on the server side (SLF4J)
- Never silently swallow errors

## Input Validation

ALWAYS validate at system boundaries:
- Validate all user input before processing
- Use Bean Validation (`@Valid`, `@NotBlank`, `@Size`) on Spring Boot DTOs
- Fail fast with clear error messages
- Never trust external data (API responses, user input, file content)

## Code Quality Checklist

Before marking work complete:
- [ ] Code is readable and well-named
- [ ] Functions are small (<50 lines)
- [ ] Files are focused (<800 lines)
- [ ] No deep nesting (>4 levels)
- [ ] Proper error handling
- [ ] No hardcoded values (use constants or config)
- [ ] No mutation (immutable patterns used)

## myFinance-Specific Rules

- **State Management**: Use **Zustand** with `persist` middleware — state auto-syncs to `localStorage` under key `assessment-storage`
- **Styling**: Use **Tailwind CSS v4** + `tailwind-merge` + `clsx` for class composition — NOT vanilla CSS
- **HTTP Client**: Use the native `fetch` wrapper in `src/services/api.js` — zero external HTTP libraries
- **Currency Formatting**: Use India-specific `fmt()` pattern with ₹, L (Lakh), Cr (Crore) notation
- **Component Structure**: Feature-based folders under `src/features/` (assessment, dashboard) + shared `src/hooks/`, `src/components/`, `src/utils/`
- **Domain Hooks**: Complex calculations live in custom hooks (e.g., `useTaxAnalysis`, `useFinancialHealthScore`, `useInsuranceAnalysis`)
- **Backend**: Spring Boot 3.4.1 + Java 21 + **Lombok** — use `@Data`, `@Builder`, `@AllArgsConstructor` not Java records
- **AI Integration**: AWS Bedrock (`amazon.nova-lite-v1:0`) via `BedrockChatService`
