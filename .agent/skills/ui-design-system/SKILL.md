---
name: ui-design-system
description: Centralized UI/UX design system for myFinance — fonts, colors, spacing, component patterns, and Tailwind conventions. Use this skill whenever building, modifying, or auditing any frontend component.
---

# myFinance UI Design System

> **ALWAYS read this before writing any JSX.** This is the single source of truth for all visual decisions.

---

## 1. Technology Stack

| Layer        | Tool                    | Version |
|-------------|-------------------------|---------|
| CSS Framework | Tailwind CSS            | v4      |
| Token System  | `@theme {}` block in CSS | —       |
| Theming       | CSS variables (`:root` + `[data-theme="light"]`) | —       |
| Font          | Inter (Google Fonts)    | —       |
| Icons         | Lucide React            | —       |

---

## 2. Color Tokens — Use ONLY These

All colors are defined in `src/styles/index.css` via the `@theme {}` block and CSS variables. **NEVER hardcode hex values in JSX.**

### Semantic Colors (Tailwind classes)

| Token              | Tailwind Class            | Purpose                                |
|-------------------|---------------------------|----------------------------------------|
| Primary           | `text-primary`, `bg-primary` | CTAs, highlights, accents (green #0ab842) |
| Primary Dark      | `bg-primary-dark`         | Hover state for primary buttons        |
| Background Dark   | `bg-background-dark`      | Page/app background                    |
| Surface Dark      | `bg-surface-dark`         | Cards, panels, modals                  |
| Surface Active    | `bg-surface-active`       | Active/selected surfaces               |
| Background Light  | `bg-background-light`     | Light theme page background            |

### Text Colors

| Purpose                  | Class              | Notes                              |
|-------------------------|--------------------|------------------------------------|
| Headings / Primary text  | `text-white`       | Auto-adapts via CSS var            |
| Secondary text           | `text-slate-400`   | Labels, descriptions               |
| Muted / Tertiary text    | `text-slate-500`   | Captions, hints, footnotes         |
| Success / Positive       | `text-emerald-400` | Coverage met, gains                |
| Warning                  | `text-amber-400`   | Gaps, attention needed             |
| Danger / Negative        | `text-red-400`     | Deficits, missing data, errors     |
| Info / Neutral accent    | `text-blue-400`    | Conservative options, links        |

### Status Colors (Background + Border Pattern)

```
✅ Success:  bg-emerald-500/10  border-emerald-500/20  text-emerald-400
⚠️ Warning:  bg-amber-500/10    border-amber-500/20    text-amber-400
❌ Error:    bg-red-500/10       border-red-500/20      text-red-400
ℹ️ Info:     bg-blue-500/10      border-blue-500/20     text-blue-400
🟢 Primary:  bg-primary/10       border-primary/20      text-primary
```

### ❌ NEVER DO THIS IN JSX

```jsx
// BAD — hardcoded hex colors
stroke="#10B981"
style={{ color: '#0df259' }}
className="text-[#94a3b8]"

// GOOD — use Tailwind tokens
className="text-emerald-400"
className="stroke-emerald-500"
className="text-primary"
```

> **Exception**: SVG `stroke` and `fill` attributes that don't support Tailwind classes may use hex, but ONLY from the token table above. Leave a comment: `{/* token: emerald-500 */}`

---

## 3. Typography Scale

Uses Tailwind's built-in scale. **Do NOT invent arbitrary pixel sizes.**

### Allowed Font Sizes

| Class      | Size  | Use For                                    |
|-----------|-------|--------------------------------------------|
| `text-2xl` | 24px  | Page headers (h1)                          |
| `text-xl`  | 20px  | Section headers                            |
| `text-lg`  | 18px  | Card headers, large metrics                |
| `text-base`| 16px  | Key metric values, important body          |
| `text-sm`  | 14px  | **Default body text**, descriptions, labels |
| `text-xs`  | 12px  | Section labels, category headers, badges   |

### ❌ BANNED — Arbitrary Font Sizes

```jsx
// NEVER use these
text-[9px]   ← illegible, banned
text-[10px]  ← use text-xs (12px) instead
text-[11px]  ← use text-xs (12px) instead
text-[13px]  ← use text-sm (14px) instead
```

> **Known debt**: ~88 instances of `text-[10px]` and `text-[9px]` exist in the codebase. When touching a file, migrate these to `text-xs`.

### Font Weight Hierarchy

| Weight        | Class           | Use For                     |
|--------------|----------------|-----------------------------|
| Bold         | `font-bold`     | Headlines, metric values    |
| Semibold     | `font-semibold` | Emphasized labels           |
| Medium       | `font-medium`   | Button text, nav items      |
| Regular      | (default)       | Body text, descriptions     |

### Label Patterns

```jsx
// Section label (uppercase tracking)
<p className="text-xs uppercase tracking-wider font-bold text-slate-500">Section Title</p>

// Metric label
<p className="text-xs text-slate-400 mb-1">Monthly Expenses</p>

// Footnote
<p className="text-xs text-slate-500 mt-0.5">from your cash flow</p>
```

---

## 4. Spacing & Layout

### Standard Padding

| Context        | Class   | Pixels |
|---------------|---------|--------|
| Card padding   | `p-5`   | 20px   |
| Section gap    | `gap-4`  | 16px   |
| Inner element  | `p-3` or `p-4` | 12-16px |
| Between cards  | `space-y-4` or `mb-6` | 16-24px |

### ❌ BANNED — Arbitrary Spacing

```jsx
// NEVER use arbitrary spacing
p-[18px]  w-[132px]  gap-[11px]  m-[7px]

// Use Tailwind scale: p-3 p-4 p-5 gap-3 gap-4 mb-4 mb-6
```

---

## 5. Component Patterns

### 5.1 Card (Standard)

```jsx
<div className="bg-surface-dark rounded-2xl border border-white/5 shadow-lg overflow-hidden">
  {/* Gradient header */}
  <div className="bg-gradient-to-r from-{color}-500/10 to-{color2}-500/10 p-5">
    <h3 className="font-bold text-white flex items-center gap-2 text-base">
      <Icon className="w-5 h-5 text-{color}-400" />
      Card Title
    </h3>
    <p className="text-sm text-slate-400 mt-1">Subtitle text here</p>
  </div>
  {/* Body */}
  <div className="p-5">
    {/* Content */}
  </div>
</div>
```

### 5.2 Metric Grid (3-column)

```jsx
<div className="grid grid-cols-3 divide-x divide-white/5 border-y border-white/5">
  <div className="p-4 text-center">
    <p className="text-xs text-slate-400 mb-1">Label</p>
    <p className="text-white font-bold text-base">₹1,00,000</p>
    <p className="text-xs text-slate-500 mt-0.5">footnote</p>
  </div>
  {/* ... repeat */}
</div>
```

### 5.3 Status Badge

```jsx
<div className={`px-3 py-1.5 rounded-full text-xs font-bold border
  ${status === 'success' ? 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30' : ''}
  ${status === 'warning' ? 'bg-amber-500/15 text-amber-400 border-amber-500/30' : ''}
  ${status === 'error' ? 'bg-red-500/15 text-red-400 border-red-500/30' : ''}
`}>
  {statusLabel}
</div>
```

### 5.4 Action Row (Scenario cards)

```jsx
<div className="flex items-start gap-3 bg-{color}-500/5 rounded-xl p-4 border border-{color}-500/15">
  <div className="w-8 h-8 rounded-full bg-{color}-500/20 flex items-center justify-center shrink-0">
    <span className="text-sm">🚀</span>
  </div>
  <div>
    <p className="text-sm font-bold text-white">Title</p>
    <p className="text-sm text-slate-400 mt-0.5">Description with <span className="text-{color}-400 font-bold">highlight</span></p>
  </div>
</div>
```

### 5.5 Progress Bar

```jsx
<div className="p-5">
  <div className="flex items-center justify-between mb-2">
    <span className="text-sm text-slate-400">Label</span>
    <span className="text-sm font-semibold text-white">X / Y units</span>
  </div>
  <div className="h-3 w-full bg-background-dark rounded-full overflow-hidden">
    <div
      className="h-full rounded-full transition-all duration-1000 bg-gradient-to-r from-emerald-500 to-primary"
      style={{ width: `${percent}%` }}
    />
  </div>
</div>
```

### 5.6 Input Fields

```jsx
<input
  className="w-full bg-background-dark border border-white/10 rounded-xl p-3 text-white focus:outline-none focus:border-primary/50 transition-colors"
/>
```

### 5.7 Buttons

```jsx
{/* Primary CTA */}
<button className="px-6 py-3 bg-primary hover:bg-primary-dark text-background-dark font-bold text-sm rounded-xl transition-all shadow-[0_0_15px_rgba(13,242,89,0.25)]">
  Save
</button>

{/* Secondary */}
<button className="px-6 py-3 bg-surface-dark hover:bg-surface-active text-white font-bold text-sm rounded-xl transition-all">
  Cancel
</button>
```

---

## 6. Border Radius Scale

| Element        | Class          | Use For                  |
|---------------|----------------|--------------------------|
| Cards          | `rounded-2xl`  | Main containers          |
| Inner sections | `rounded-xl`   | Nested panels, inputs    |
| Badges/pills   | `rounded-full`  | Status tags, avatars     |
| Small items    | `rounded-lg`    | Small chips, toggles     |

---

## 7. Animation Standards

```jsx
// Standard transition for interactive elements
className="transition-all"

// Progress bars and animated fills
className="transition-all duration-1000"

// Fade-in on mount (from index.css)
className="animate-fade-in"
className="animate-fade-in-up"
```

---

## 8. Dark/Light Theme Compliance

- All colors MUST use Tailwind tokens or CSS variables — they auto-adapt.
- `text-white` maps to `#ffffff` in dark, `#111827` in light via CSS var.
- `text-slate-*` shades are remapped per theme in `index.css`.
- **Test both themes** after any UI change by toggling `data-theme`.

---

## 9. Responsive Design Rules

- Mobile-first (`min-w-320px`).
- Cards: `w-full` by default, grid with `grid-cols-3` for metric rows.
- Use `sm:` breakpoint for desktop-only elements: `<div className="hidden sm:flex ...">`.
- Footer buttons: `fixed bottom-0 left-0 right-0` with `backdrop-blur-md`.

---

## 10. UI Audit Checklist

When **reviewing or modifying** any component, check each item:

- [ ] **No hardcoded hex colors in JSX** — use Tailwind tokens
- [ ] **No `text-[Npx]` arbitrary sizes** — use `text-xs` / `text-sm` / `text-base` / `text-lg`
- [ ] **No arbitrary spacing** (`p-[18px]`) — use Tailwind scale
- [ ] **Card follows standard pattern** — `bg-surface-dark rounded-2xl border border-white/5`
- [ ] **Labels use standard pattern** — `text-xs text-slate-400`
- [ ] **Status colors follow the pattern** — `bg-{color}-500/10 border-{color}-500/20 text-{color}-400`
- [ ] **Progress bars have label + value above** — not ₹0 markers
- [ ] **Buttons follow standard** — primary/secondary patterns
- [ ] **Grammar check** — singular/plural (1 month vs months)
- [ ] **Icon sizing consistent** — `w-4 h-4` (inline), `w-5 h-5` (card header), `w-8 h-8` (feature icon)
- [ ] **Font weight hierarchy respected** — bold for values, medium for labels
- [ ] **Dark/light theme tested** — all colors auto-adapt

---

## 11. Known Technical Debt

These are existing violations to fix when touching the respective files:

| Issue | Files Affected | Fix |
|-------|---------------|-----|
| `text-[9px]` / `text-[10px]` usage | Step1, Step4, Step5, InsuranceTab, Layout, Dashboard, ProjectionChart | Replace with `text-xs` |
| `text-[11px]` usage | FinancialTimeMachine, PillarInterpretation, InsuranceTab, Dashboard | Replace with `text-xs` |
| Hardcoded hex in SVG | Step3, Step4, ProjectionChart, FinancialTimeMachine | Use token + comment |
| Hardcoded slider colors | `index.css` (range inputs) | Convert to CSS vars |
| Inconsistent `rounded-*` | Mixed `rounded-lg` / `rounded-xl` / `rounded-2xl` within same card | Standardize per §6 |

---

## 12. Currency Formatting

Always use the project's `formatToCrLakh()` utility for amounts > ₹999.
For smaller amounts, use `toLocaleString('en-IN')` with ₹ prefix:

```jsx
// Large amounts
{formatToCrLakh(efTarget)}  // → "₹6.08L" or "₹1.2Cr"

// Monthly/small amounts
₹{Math.round(amount).toLocaleString('en-IN')}  // → "₹1,00,000"
```

---

## 13. Plural/Singular Helper

When displaying dynamic counts, always handle grammar:

```jsx
const formatMonths = (m) => {
  const rounded = Math.ceil(m);
  return `${rounded} ${rounded === 1 ? 'month' : 'months'}`;
};
// Usage: "ready in {formatMonths(value)}"
// → "ready in 1 month" or "ready in 3 months"
```

> Apply this pattern to all countable units: months, years, goals, assets, etc.
