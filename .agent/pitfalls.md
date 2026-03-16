# Pitfalls — Things That Will Go Wrong

Known gotchas in the myFinance project. Read this BEFORE writing code.

## ❌ DO NOT

| Pitfall | Why | Correct Approach |
|---------|-----|-----------------|
| Use Java records for DTOs | Project uses Lombok everywhere | Use `@Data @Builder @NoArgsConstructor @AllArgsConstructor` |
| Use `@Autowired` field injection | Untestable, hidden deps | Use constructor injection (Lombok `@RequiredArgsConstructor`) |
| Add axios, got, or any HTTP lib | Native fetch wrapper exists | Use `src/services/api.js` |
| Use `useState` for shared state | Zustand is the state manager | Use `useAssessmentStore` or create new Zustand store |
| Write vanilla CSS | Tailwind v4 is project standard | Use Tailwind classes with `clsx` + `tailwind-merge` |
| Use string concatenation for classes | Causes specificity issues | Use `clsx('bg-white', isActive && 'bg-blue-500')` |
| Enable H2 console in prod | Security risk — exposes DB | Only in dev profile (`application.yml`) |
| Log exact ₹ amounts in backend | Financial PII exposure | Log as ranges: "amount_range=10L-50L" |
| Add Spring Security dependency | Not needed yet, will break everything | Auth is planned but not implemented |
| Hardcode more secrets in Docker Compose | Already has hardcoded DB password | Use `${ENV_VAR}` syntax |
| Create files > 800 lines | Hard to maintain, wastes agent context | Extract into smaller modules |
| Use `dangerouslySetInnerHTML` | XSS risk with user financial data | Use `textContent` or sanitize |
| Put new hooks in feature folders | Shared hooks go in `src/hooks/` | Only feature-specific store goes in feature folder |
| Forget `useMemo` on financial calcs | Expensive re-computation every render | Always wrap derived financial data in `useMemo` |
| Use `toFixed()` for display | Inconsistent with project formatting | Use `fmt()` pattern: Cr/L/₹ with `toLocaleString('en-IN')` |
