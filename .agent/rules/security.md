# Security Guidelines

## Mandatory Security Checks

Before ANY commit:
- [ ] No hardcoded secrets (API keys, passwords, tokens)
- [ ] All user inputs validated (Bean Validation on backend DTOs)
- [ ] SQL injection prevention (Spring Data JPA — auto-parameterized)
- [ ] Error messages don't leak sensitive data
- [ ] AWS credentials use `${ENV_VAR}` syntax in Docker Compose

## Secret Management

- NEVER hardcode secrets in source code
- ALWAYS use environment variables or Docker runtime injection
- Validate that required secrets are present at startup
- Rotate any secrets that may have been exposed

## Current Known Issues ⚠️

| Issue | Location | Status |
|-------|----------|--------|
| Hardcoded `POSTGRES_PASSWORD: postgres` | `docker-compose.yml` line 8 | ⚠️ Should use `${DB_PASSWORD}` |
| Hardcoded DB creds in backend env | `docker-compose.yml` lines 28-30 | ⚠️ Should use env vars |
| H2 console enabled in dev | `application.yml` line 13 | ⚠️ Ensure disabled in prod profile |
| No Spring Security dependency | `pom.xml` | 📝 Auth not yet implemented |
| No rate limiting | Backend | 📝 Not yet implemented |

## Security Response Protocol

If security issue found:
1. STOP immediately
2. Use **security-reviewer** agent
3. Fix CRITICAL issues before continuing
4. Rotate any exposed secrets
5. Review entire codebase for similar issues

## myFinance-Specific Security

- **Zustand Persist**: Financial data (incomes, assets, liabilities, insurance) auto-persists to `localStorage` under key `assessment-storage` — ensure no PII like PAN/Aadhaar is captured in store fields
- **AWS Bedrock**: API calls to `amazon.nova-lite-v1:0` — ensure `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` are ONLY in Docker runtime env, never in source code
- **Docker `.env` files**: Verify `.env` is in `.dockerignore` AND `.gitignore`
- **EC2 Deployment**: SSH keys and AWS credentials must never appear in git history or Docker layers
- **Backend API**: Base URL `/api/v1/assessment` — no auth middleware exists yet, plan for it before production
- **H2 Console**: Development uses H2 in-memory DB with console at `/h2-console` — must be disabled in production profile
