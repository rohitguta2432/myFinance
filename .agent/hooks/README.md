# Hooks — Event-Driven Automations

Reference documentation for patterns to enforce in the myFinance project.

## Secret Detection Hook

**Trigger:** Before any commit or file save

### Patterns to Flag

| Pattern | Example | Action |
|---------|---------|--------|
| AWS Access Key | `AKIA...` | 🛑 BLOCK |
| AWS Secret Key | 40-char alphanumeric | 🛑 BLOCK |
| SSH Private Key | `-----BEGIN RSA PRIVATE KEY-----` | 🛑 BLOCK |
| Hardcoded DB password in YML/compose | `password: actualPassword123` | 🛑 BLOCK |
| API Key in Java/JS source | `apiKey = "sk-..."` | 🛑 BLOCK |
| `.env` file not in `.gitignore` | Missing `.env` entry | ⚠️ WARNING |

### Files to Always Scan
- `application.yml`
- `docker-compose.yml`
- Any `*.java`, `*.js`, `*.jsx` file

### Known Current Issues
- `docker-compose.yml` has `POSTGRES_PASSWORD: postgres` — should be flagged ⚠️

---

## Console.log Warning Hook

**Trigger:** Before committing `.js`/`.jsx` files

| Pattern | Action |
|---------|--------|
| `console.log(` | ⚠️ WARNING: Remove or use toast |
| `console.error(` | ✅ OK in error handlers |
| `debugger;` | 🛑 BLOCK: Never commit |

---

## Financial Data Sanitization Hook

**Trigger:** Before logging or error reporting

### Fields to Redact in Logs
- Exact ₹ amounts → Log as range (e.g., `₹10L-50L`)
- PAN/Aadhaar → `***REDACTED***`
- Zustand `assessment-storage` key → Never dump full store to logs

---

## Pre-Commit Quality Checklist

1. ✅ No files > 800 lines
2. ✅ No functions > 50 lines
3. ✅ No nesting > 4 levels deep
4. ✅ No hardcoded secrets
5. ✅ No `console.log` in production code
6. ✅ `.env` is in `.gitignore`
7. ✅ Tailwind classes use `clsx`/`tailwind-merge` (not string concatenation)

---

## Usage

Integrate these checks into:
1. **Git pre-commit hooks** (Husky or custom scripts)
2. **CI/CD pipeline** (GitHub Actions)
3. **Code review checklists** (reference during PR reviews)
