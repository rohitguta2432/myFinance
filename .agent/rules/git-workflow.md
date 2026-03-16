# Git Workflow

## Commit Message Format

Use conventional commits:

```
<type>(<scope>): <subject>

[optional body]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code restructuring
- `docs`: Documentation only
- `style`: Formatting/whitespace
- `test`: Adding or fixing tests
- `chore`: Build, tooling, dependency changes

Examples:
```
feat(dashboard): add tax regime comparison chart
fix(store): correct Zustand persist key conflict
refactor(hooks): extract shared currency formatting from useTaxAnalysis
docs(skill): update springboot-patterns with Bedrock integration
chore(docker): parameterize POSTGRES_PASSWORD in compose
```

## Branch Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Production-ready code — deployed to EC2 |
| `dev` | Integration branch — merge features here first |
| `feature/*` | New features (e.g., `feature/ai-chat-widget`) |
| `fix/*` | Bug fixes (e.g., `fix/tax-slab-boundary`) |

## Merge Rules

- Feature → `dev` via PR
- `dev` → `main` when stable
- Never force-push to `main`
- Delete branches after merge
