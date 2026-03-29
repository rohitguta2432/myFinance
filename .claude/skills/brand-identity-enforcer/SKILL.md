---
name: brand-identity-enforcer
description: >
  Enforces your project's unique visual identity across all AI-generated UI code.
  Auto-detects brand signals from tailwind.config, CSS variables, design tokens,
  existing components, and font packages — then constrains all UI generation to
  match YOUR design system instead of producing generic "AI-looking" output.
  Use this skill whenever creating, modifying, or reviewing UI components, pages,
  layouts, forms, dashboards, or any frontend code. Also triggers when the user
  mentions design consistency, brand identity, design system, design tokens,
  theming, or complains that AI output "doesn't match" or "looks generic."
  Works across React, Vue, Svelte, Flutter, SwiftUI, and vanilla HTML/CSS.
---

# Brand Identity Enforcer

## Why This Skill Exists

AI coding agents produce UI that all looks the same — purple-to-blue gradients,
rounded-2xl shadow-xl cards, Inter font, gray-900 text. This happens because
agents have no awareness of the project's existing design identity. They generate
from their training distribution instead of from your codebase.

This skill fixes that. It reads your project's design signals, builds a brand
profile, and constrains all UI generation to match what already exists. The
result: new code that looks like it belongs in your app, not in a generic template.

## How It Works

Every time you're about to write UI code, follow this three-phase process:
**Detect → Constrain → Verify**. If a brand profile already exists at
`.brand-identity/profile.md`, skip detection and load it directly.

---

## Phase 1: Detect Brand Identity

Before generating any UI, scan the project to understand its visual identity.
Check these sources in order — each one adds signal. You don't need all of them;
work with what exists.

### Source 1: Tailwind Configuration

Read `tailwind.config.ts` (or `.js`, `.mjs`). Extract:
- **Custom colors** — the `extend.colors` object reveals the project's palette.
  Note which colors are primary, secondary, accent, and what neutral scale is used
  (zinc? slate? gray? neutral? stone?)
- **Font families** — `extend.fontFamily` shows the typography stack
- **Border radius** — `extend.borderRadius` reveals the corner convention
- **Spacing overrides** — any custom spacing scale
- **Screens** — breakpoint conventions

### Source 2: CSS Custom Properties

Read `globals.css`, `app.css`, or the root stylesheet. Look for:
- `:root` and `.dark` blocks with CSS variables
- `--primary`, `--secondary`, `--accent`, `--background`, `--foreground`
- `--radius`, `--font-sans`, `--font-mono`
- oklch/hsl/hex color format (note which one — stay consistent)
- Any `@layer base` or `@layer components` definitions

### Source 3: Design Tokens (if present)

Check for W3C Design Tokens files:
- `.tokens.json`, `tokens/*.json`, `tokens.config.json`
- Style Dictionary config (`style-dictionary.config.json`)
- Tokens Studio format (`tokens.json` with `$value` / `$type`)

Read `references/token-formats.md` for parsing guidance on these formats.

### Source 4: Existing Components

Scan 3-5 existing UI component files (prioritize files in `components/ui/` or
the equivalent). Note:
- What Tailwind classes are actually used on cards, buttons, headings, inputs
- Whether the project uses a component library (shadcn, Radix, Vuetify, etc.)
- Composition patterns — do they use compound components? render props? slots?
- Import patterns — where do shared components come from?

### Source 5: Package Dependencies

Read `package.json` for signals:
- Font packages: `geist`, `@fontsource/*`, `next/font` usage in layout
- UI frameworks: `@radix-ui/*`, `vuetify`, `@mui/*`, `@mantine/*`
- Styling: `tailwindcss`, `styled-components`, `emotion`, `vanilla-extract`
- Animation: `framer-motion`, `@formkit/auto-animate`, `gsap`

### Source 6: Brand Assets (optional)

If a `.brand-identity/overrides.md` file exists, the user has written explicit
brand rules. These take highest priority over anything auto-detected.

### Build the Brand Profile

After scanning, synthesize your findings into a structured brand profile. Use
the template at `templates/brand-profile-template.md` as the structure. Save
the generated profile to `.brand-identity/profile.md` in the project root.

Tell the user: "I've detected your project's design identity and saved a brand
profile. Here's a summary of what I found: [brief summary]. You can edit
`.brand-identity/profile.md` to adjust anything."

---

## Phase 2: Constrain Generation

With the brand profile loaded, apply these constraints when writing UI code.
The goal is not rigidity — it's consistency. Every choice should feel like it
was made by the same designer who built the rest of the app.

### Color Usage
- Use CSS variables or Tailwind theme colors from the profile — never hardcoded
  hex/rgb/hsl values
- Follow the project's color variable naming convention exactly
  (e.g., `text-primary` not `text-blue-600`, `bg-card` not `bg-white`)
- If the project uses oklch variables, generate oklch. If hex, generate hex.
  Match the format.
- For semantic colors (success, warning, error), check if the project has
  defined these. If not, derive them from the existing palette rather than
  picking arbitrary green/yellow/red

### Typography
- Use the project's font stack — never introduce new fonts
- Follow the existing heading hierarchy (check: does the project use `text-2xl
  font-semibold` for page titles? Or `text-lg font-medium`? Match it.)
- Match the body text size (many projects use `text-sm` as default body, not
  `text-base`)
- Use the same font weight conventions for emphasis

### Spacing & Layout
- Follow the project's spacing grid (if cards use `p-4`, don't use `p-6` or `p-8`)
- Match the gap/space-y convention between elements
- Use the same max-width for content areas
- Follow the existing responsive breakpoint patterns

### Borders, Radius & Shadows
- Use the project's border-radius value — this is one of the strongest brand
  signals. `rounded-lg` vs `rounded-xl` vs `rounded-none` completely changes
  the feel.
- Match shadow intensity — if the project uses `shadow-sm` and `border`, don't
  generate `shadow-xl`
- Follow the border convention (`border border-border` vs no borders, etc.)

### Component Reuse
- Before creating new UI, check if an existing component already handles it.
  Reuse `<Card>`, `<Button>`, `<Input>`, `<Badge>`, etc. from the project's
  component library
- Follow the project's import path conventions
- Match the component API pattern (props vs children vs slots)
- If the project uses a library like shadcn/ui, use its components and
  composition patterns rather than building from scratch

### Animation & Interaction
- Match the project's animation style (or lack thereof)
- If the project uses `transition-all duration-150`, don't generate
  `transition-all duration-500`
- If the project doesn't use animations, don't add them

### Dark Mode
- Follow the project's dark mode strategy (class-based, media query, or none)
- If class-based, use the established dark: prefix patterns
- Match the dark mode color mapping (does dark mode use zinc-950 or slate-900?)

### Anti-Patterns to Avoid
These are the telltale signs of generic AI output. Never generate:
- `bg-gradient-to-br from-purple-600 to-blue-500` (or any gradient not in the
  brand profile)
- `shadow-xl` or `shadow-2xl` (unless the project actually uses heavy shadows)
- `rounded-full` on cards or containers (unless it's an established pattern)
- Arbitrary Tailwind values like `w-[347px]` or `mt-[13px]`
- Colors not in the project's palette
- Fonts not in the project's font stack
- Generic hero sections with stock patterns when the project has its own layout

---

## Phase 3: Verify Output

After generating UI code, run this quick check before presenting it to the user.
This catches drift before it ships.

### Checklist

1. **Token compliance** — Does every color, font, spacing, and radius value
   reference a token/variable from the brand profile? Flag any hardcoded values.

2. **Component reuse** — Did we use existing components where they fit, or did
   we reinvent something that already exists in the project?

3. **Pattern consistency** — Does the new code follow the same structural
   patterns as existing pages/components? (Same layout wrapper, same heading
   style, same card pattern.)

4. **No alien styles** — Does anything in the generated code look like it came
   from a different app? Check for styles that clash with the established
   aesthetic.

5. **Dark mode coverage** — If the project supports dark mode, does the new
   code handle it correctly using the project's convention?

Read `references/enforcement-checklist.md` for the full detailed checklist
with examples.

If any check fails, fix the code before presenting it. Don't flag it as an
issue for the user — just generate it correctly the first time.

---

## Phase 4: Design Memory

### Saving New Decisions

When you make a design decision during generation that establishes a new pattern
(e.g., "for data tables, we'll use alternating row backgrounds with `bg-muted/50`"),
offer to save it:

"I established a new pattern for [X]. Want me to save this to your brand profile
so future generations stay consistent?"

If yes, append to `.brand-identity/decisions.md` with the date and context.

### Updating the Profile

If the user explicitly changes a design direction ("actually, let's use sharper
corners from now on" or "switch to the blue accent instead of green"), update
`.brand-identity/profile.md` immediately and confirm the change.

### Loading Overrides

If `.brand-identity/overrides.md` exists, these are user-specified rules that
override auto-detection. Common overrides:
- "Never use gradients"
- "Always use border-based elevation, never shadows"
- "Our brand color is exactly #1a73e8, not Tailwind blue-600"
- "Use Geist Mono for all data/numbers, not just code blocks"

Overrides always win over auto-detected values.

---

## Framework-Specific Guidance

### React / Next.js (with Tailwind + shadcn/ui)
- Check `components.json` for shadcn configuration (cssVariables, style, rsc)
- Use `cn()` utility for conditional classes
- Follow the project's Server/Client Component boundary patterns
- Match existing use of `<Suspense>`, loading states, error boundaries

### Vue (with Vuetify / PrimeVue / custom)
- Check `vuetify.config.ts` for theme definition
- Use the project's composable patterns for shared logic
- Match `<script setup>` vs Options API convention
- Follow the project's `<style scoped>` vs Tailwind approach

### Svelte / SvelteKit
- Check if using Skeleton UI, shadcn-svelte, or custom components
- Match the project's store patterns
- Follow the `+page.svelte` / `+layout.svelte` conventions

### Flutter
- Read `lib/theme/` for `ThemeData` configuration
- Use the project's `ColorScheme` and `TextTheme`
- Follow the widget composition patterns (prefer composition over inheritance)
- Match the project's state management approach (Riverpod, BLoC, Provider)

### SwiftUI
- Check for custom `Color` extensions and `ViewModifier` definitions
- Match the project's `View` composition style
- Follow the existing `@Environment` / `@State` patterns

### Vanilla HTML/CSS
- Check the CSS architecture (BEM? utility classes? CSS modules?)
- Match naming conventions
- Follow the existing `<section>` / `<article>` semantic patterns

---

## Quick Reference: Detection → Profile → Constraint Flow

```
Project Files                    Brand Profile              Constrained Output
─────────────                    ─────────────              ──────────────────
tailwind.config.ts ──┐
globals.css ─────────┤
.tokens.json ────────┼──→ .brand-identity/profile.md ──→ UI code that
components/ui/* ─────┤                                     matches YOUR app
package.json ────────┤
overrides.md ────────┘
```

## Reference Files

- `references/detection-rules.md` — Detailed parsing rules for each source
- `references/enforcement-checklist.md` — Full verification checklist with examples
- `references/token-formats.md` — W3C Design Tokens, Style Dictionary, Tokens Studio formats
- `templates/brand-profile-template.md` — Template for generating brand profiles
- `examples/` — Example brand profiles for different project types
