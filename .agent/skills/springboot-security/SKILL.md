---
name: springboot-security
description: "DISABLED — Spring Security is NOT in this project's dependencies. This skill is kept as a reference for when auth is added."
origin: ECC
status: DISABLED
---

# Spring Boot Security — DISABLED

> **⚠️ This skill is DISABLED.** The myFinance project does not currently have `spring-boot-starter-security` in its `pom.xml`. There is NO authentication, authorization, JWT, CORS security filter, or rate limiting implemented.

## When to Enable

Add `spring-boot-starter-security` to `pom.xml` and activate this skill when:
- User authentication is required (login/signup)
- API endpoints need authorization (role-based access)
- Production deployment requires security hardening

## Reference — What to Add When Ready

1. `spring-boot-starter-security` dependency
2. `SecurityFilterChain` configuration
3. JWT or session-based authentication
4. `@PreAuthorize` on sensitive endpoints
5. CORS configuration (restrict to `https://myfinance.rohitraj.tech`)
6. Rate limiting (Bucket4j or Spring Cloud Gateway)
7. Password encoding (BCrypt)
8. CSRF configuration

## See Also

- **`security-reviewer.md`** agent — for current (non-auth) security checks
- **`rules/security.md`** — for Docker/AWS/data security rules that apply NOW
