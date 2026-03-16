# Performance Optimization

## Context Window Management

Avoid last 20% of context window for:
- Large-scale refactoring
- Feature implementation spanning multiple files
- Debugging complex interactions

Lower context sensitivity tasks:
- Single-file edits
- Independent utility creation
- Documentation updates
- Simple bug fixes

## Build Troubleshooting

If build fails:
1. Analyze error messages
2. Fix incrementally
3. Verify after each fix

## myFinance Performance Rules

### Frontend (Vite + React 19 + Tailwind v4)
- **Zustand Store**: Store is auto-persisted — avoid unnecessary `set()` calls that trigger re-renders
- **Heavy Hooks**: `useTaxAnalysis` (275 lines), `useFinancialHealthScore`, `useInsuranceAnalysis` — all use `useMemo` for expensive calculations. **Always wrap derived financial data in `useMemo`**
- **Bundle Size**: Monitor Vite chunk sizes — lazy-load dashboard tabs and `AiChatWidget` component
- **Tailwind**: Use `tailwind-merge` + `clsx` for conditional classes — avoids CSS specificity conflicts
- **Images**: Use `lucide-react` icons instead of custom SVGs where possible

### Backend (Spring Boot 3.4.1 + Java 21)
- **Database**: Dev uses H2 in-memory (fast restarts), Prod uses PostgreSQL via Docker
- **Lombok**: Use `@Data`, `@Builder` to reduce boilerplate — no need for manual getters/setters
- **JPA**: Use `@Transactional(readOnly = true)` for query-only service methods
- **AWS Bedrock**: AI chat calls are network-bound — consider `@Async` if response times are slow
- **No caching exists yet**: When needed, add `spring-boot-starter-cache` + `@Cacheable`

### Docker & Deployment
- **Multi-stage builds**: Frontend Dockerfile should separate build and serve stages (nginx)
- **Health checks**: PostgreSQL container already has health check — backend should add `/actuator/health`
- **Image size**: Use Alpine-based images where possible
