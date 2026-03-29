# Detection Rules

Detailed parsing guidance for each brand identity source. The SKILL.md gives
the overview; this file has the specifics for when you need to dig deeper.

## Table of Contents
1. [Tailwind Config Parsing](#tailwind-config-parsing)
2. [CSS Custom Properties](#css-custom-properties)
3. [Design Tokens](#design-tokens)
4. [Component Analysis](#component-analysis)
5. [Package Signal Map](#package-signal-map)
6. [Conflict Resolution](#conflict-resolution)

---

## Tailwind Config Parsing

### File locations to check (in order)
1. `tailwind.config.ts`
2. `tailwind.config.js`
3. `tailwind.config.mjs`
4. `tailwind.config.cjs`

### What to extract

**Colors** — Look at `theme.extend.colors`:
```js
// Example: shadcn/ui project
colors: {
  border: "hsl(var(--border))",
  background: "hsl(var(--background))",
  foreground: "hsl(var(--foreground))",
  primary: {
    DEFAULT: "hsl(var(--primary))",
    foreground: "hsl(var(--primary-foreground))",
  },
}
```
Signal: This project uses CSS variables via HSL. All generated colors should
reference these variables, not Tailwind's default palette.

```js
// Example: Custom palette project
colors: {
  brand: {
    50: '#f0f7ff',
    500: '#1a73e8',
    900: '#0d47a1',
  },
  surface: '#fafafa',
}
```
Signal: This project uses a custom `brand` color scale with hex values. Use
`bg-brand-500`, `text-brand-900`, etc.

**Font Family** — Look at `theme.extend.fontFamily`:
```js
fontFamily: {
  sans: ['var(--font-geist-sans)', ...fontFamily.sans],
  mono: ['var(--font-geist-mono)', ...fontFamily.mono],
}
```
Signal: Geist fonts loaded via CSS variables (likely Next.js `next/font`).

**Border Radius** — Look at `theme.extend.borderRadius`:
```js
borderRadius: {
  lg: 'var(--radius)',
  md: 'calc(var(--radius) - 2px)',
  sm: 'calc(var(--radius) - 4px)',
}
```
Signal: Radius is driven by a CSS variable. Check `globals.css` for the
`--radius` value.

**Plugins** — Note which plugins are active:
- `@tailwindcss/typography` → project uses `prose` classes
- `@tailwindcss/forms` → form elements are pre-styled
- `@tailwindcss/container-queries` → uses `@container` patterns
- `tailwindcss-animate` → animation utilities available

---

## CSS Custom Properties

### File locations to check
1. `app/globals.css` (Next.js App Router)
2. `src/app/globals.css` (Next.js with src/)
3. `src/index.css` (Vite/CRA)
4. `src/app.css` (SvelteKit)
5. `assets/css/main.css` (Nuxt)

### Parsing the `:root` block

Extract all custom properties and categorize them:

```css
:root {
  /* Colors — note the format (oklch, hsl, hex) */
  --background: 0 0% 100%;        /* HSL without the hsl() wrapper */
  --foreground: 240 10% 3.9%;
  --primary: 240 5.9% 10%;
  --primary-foreground: 0 0% 98%;

  /* Sizing */
  --radius: 0.5rem;

  /* Fonts (if defined here) */
  --font-sans: 'Geist', sans-serif;
}
```

Important: Note how colors are formatted. shadcn/ui v2+ uses oklch:
```css
:root {
  --primary: oklch(0.21 0.006 285.88);
  --primary-foreground: oklch(0.985 0 0);
}
```

The format matters — when generating code, the CSS variable references work
regardless, but if you need to define new semantic colors, use the same format.

### Parsing the `.dark` block

```css
.dark {
  --background: 240 10% 3.9%;
  --foreground: 0 0% 98%;
}
```
This tells you: dark mode is class-based (toggled by adding `dark` to `<html>`).

If there's a `@media (prefers-color-scheme: dark)` block instead, dark mode
is system-preference-based.

---

## Design Tokens

### W3C Design Tokens Format (`.tokens.json`)
```json
{
  "color": {
    "primary": {
      "$value": "#1a73e8",
      "$type": "color"
    },
    "surface": {
      "default": {
        "$value": "#ffffff",
        "$type": "color"
      },
      "elevated": {
        "$value": "#f8f9fa",
        "$type": "color"
      }
    }
  },
  "spacing": {
    "sm": { "$value": "8px", "$type": "dimension" },
    "md": { "$value": "16px", "$type": "dimension" },
    "lg": { "$value": "24px", "$type": "dimension" }
  }
}
```

### Tokens Studio Format (`tokens.json`)
```json
{
  "global": {
    "colors": {
      "primary": {
        "value": "#1a73e8",
        "type": "color"
      }
    }
  },
  "light": {
    "bg": {
      "value": "{global.colors.white}",
      "type": "color"
    }
  }
}
```
Note: Tokens Studio uses `value` (no `$` prefix) and supports references
with `{group.token}` syntax.

### Style Dictionary Format
Check `style-dictionary.config.json` for the build configuration. Tokens
are usually in `tokens/` directory with `.json` files organized by category.

See `references/token-formats.md` for full format specifications.

---

## Component Analysis

### What to scan
Read 3-5 components from the project's UI library. Prioritize:
1. `components/ui/card.tsx` (or equivalent) — reveals card styling
2. `components/ui/button.tsx` — reveals interactive element styling
3. The main layout file (`app/layout.tsx`, `+layout.svelte`, etc.)
4. Any dashboard or settings page — reveals content layout patterns
5. The homepage or primary landing page

### What to extract from each component

**Card pattern** — How cards look is one of the strongest brand signals:
```tsx
// This project uses border-based cards with subtle shadow
<div className="rounded-lg border bg-card text-card-foreground shadow-sm">

// vs. this project uses shadow-based cards without borders
<div className="rounded-2xl bg-white shadow-lg">

// vs. this project uses flat cards with background color
<div className="rounded-md bg-muted p-4">
```

**Button pattern** — How buttons are styled and composed:
```tsx
// shadcn pattern with variants
<Button variant="default" size="sm">

// Custom pattern with explicit classes
<button className="bg-brand-500 text-white px-4 py-2 rounded-md font-medium">
```

**Layout pattern** — How pages are structured:
```tsx
// Dashboard layout with sidebar
<div className="flex h-screen">
  <Sidebar />
  <main className="flex-1 overflow-auto p-6">

// Content layout with max-width
<div className="mx-auto max-w-2xl px-4 py-8">
```

---

## Package Signal Map

Quick reference for what packages signal about the project's design approach:

| Package | Signal |
|---------|--------|
| `tailwindcss` | Utility-first CSS |
| `@radix-ui/*` | Headless UI primitives (likely shadcn) |
| `class-variance-authority` | Component variant system (likely shadcn) |
| `clsx` + `tailwind-merge` | Using `cn()` utility |
| `vuetify` | Material Design 3 system |
| `@mui/material` | Material UI (React) |
| `@mantine/core` | Mantine component library |
| `@chakra-ui/react` | Chakra UI |
| `styled-components` | CSS-in-JS approach |
| `@emotion/react` | CSS-in-JS (often with MUI) |
| `framer-motion` | Declarative animations |
| `@formkit/auto-animate` | Automatic animations |
| `geist` | Vercel's Geist font family |
| `@fontsource/*` | Self-hosted Google Fonts |
| `lucide-react` | Lucide icon set |
| `@heroicons/react` | Heroicons icon set |
| `@phosphor-icons/react` | Phosphor icon set |

---

## Conflict Resolution

When different sources give conflicting signals, resolve using this priority:

1. **`.brand-identity/overrides.md`** — User-explicit rules always win
2. **Existing components** — What's actually shipped is ground truth
3. **CSS Custom Properties** — These are the runtime values
4. **Tailwind config** — The configured design system
5. **Design tokens** — The token specification
6. **Package.json** — Weakest signal (just indicates capabilities)

Example conflict: `tailwind.config.ts` defines `borderRadius.lg = '1rem'` but
all existing components use `rounded-md` in practice. Trust the components —
the config might be a default that was never updated.
