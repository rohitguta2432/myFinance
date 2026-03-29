# Enforcement Checklist

Full verification checklist to run after generating UI code. Each check has
a description, what to look for, and an example of correct vs incorrect output.

---

## 1. Token Compliance

**What to check:** Every visual value (color, font, spacing, radius, shadow)
should reference a token or variable from the brand profile.

**Red flags:**
- Hardcoded hex: `text-[#333333]` or `color: #1a73e8`
- Hardcoded rgb: `bg-[rgb(26,115,232)]`
- Default Tailwind colors when project has custom palette: `bg-blue-600`
  instead of `bg-primary`
- Arbitrary spacing: `p-[13px]` instead of a scale value like `p-3`

**Correct:**
```tsx
// Using project's CSS variables
<div className="bg-background text-foreground">
  <h1 className="text-primary">Title</h1>
  <p className="text-muted-foreground">Description</p>
</div>
```

**Incorrect:**
```tsx
// Hardcoded values that bypass the design system
<div className="bg-white text-gray-900">
  <h1 className="text-blue-600">Title</h1>
  <p className="text-gray-500">Description</p>
</div>
```

---

## 2. Component Reuse

**What to check:** Are we using existing components from the project's library
instead of creating new ones that duplicate functionality?

**Before generating a new component, check if these exist:**
- Card / CardHeader / CardContent / CardFooter
- Button (with variants)
- Input / Textarea / Select
- Badge / Tag / Chip
- Dialog / Modal / Sheet
- Table / DataTable
- Tabs / TabsList / TabsTrigger / TabsContent
- Form / FormField / FormItem / FormLabel
- Avatar
- Separator / Divider
- Skeleton (loading state)
- Alert / Toast

**Correct:**
```tsx
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"

<Card>
  <CardHeader>
    <CardTitle>Settings</CardTitle>
  </CardHeader>
  <CardContent>
    <Button variant="outline">Cancel</Button>
    <Button>Save</Button>
  </CardContent>
</Card>
```

**Incorrect:**
```tsx
// Reinventing Card from scratch when it already exists
<div className="rounded-xl border bg-white p-6 shadow-md">
  <h2 className="text-xl font-bold">Settings</h2>
  <div className="mt-4">
    <button className="border px-4 py-2 rounded-md mr-2">Cancel</button>
    <button className="bg-blue-600 text-white px-4 py-2 rounded-md">Save</button>
  </div>
</div>
```

---

## 3. Pattern Consistency

**What to check:** Does the new code follow the same structural patterns as
existing pages and components?

**Layout patterns to match:**
- Page wrapper (max-width, padding)
- Section spacing (gap between major sections)
- Heading hierarchy (h1 for page title, h2 for sections, h3 for subsections)
- Content alignment (left-aligned? centered?)
- Grid vs flex patterns for layout

**Example — if existing pages look like this:**
```tsx
<Shell>
  <PageHeader title="Users" description="Manage your team members" />
  <div className="space-y-4">
    <Card>...</Card>
    <Card>...</Card>
  </div>
</Shell>
```

**Then new pages should follow the same pattern:**
```tsx
// Correct — matches existing pattern
<Shell>
  <PageHeader title="Settings" description="Configure your workspace" />
  <div className="space-y-4">
    <Card>...</Card>
  </div>
</Shell>

// Incorrect — different structure
<div className="max-w-7xl mx-auto px-4 py-8">
  <h1 className="text-3xl font-bold mb-8">Settings</h1>
  <div className="grid grid-cols-2 gap-8">...</div>
</div>
```

---

## 4. No Alien Styles

**What to check:** Does anything look like it came from a different app?

**Common alien patterns:**
- Gradients when the project doesn't use any
- Heavy shadows (`shadow-xl`, `shadow-2xl`) in a flat-design project
- `rounded-full` on cards/containers (usually only for avatars and badges)
- Decorative borders or rings not used elsewhere
- Pastel/neon colors not in the project's palette
- Glassmorphism (`backdrop-blur`, opacity backgrounds) when not established
- Excessive hover effects or transforms when the project is minimal

**The "screenshot test":** If you took a screenshot of the new component and
placed it next to existing pages, would it look like it belongs? If something
would visually jar, it's an alien style.

---

## 5. Dark Mode Coverage

**What to check:** If the project supports dark mode, does the new code handle
it correctly?

**Class-based dark mode (most common):**
```tsx
// Correct — explicit dark mode variants
<div className="bg-card dark:bg-card border dark:border-border">

// Also correct — if using CSS variables that auto-switch
<div className="bg-background text-foreground">
// (These variables already have .dark definitions in globals.css)
```

**What to watch for:**
- Hardcoded `bg-white` without a `dark:bg-zinc-900` (or equivalent)
- Hardcoded `text-gray-900` without `dark:text-gray-100`
- Images or icons that don't adapt to dark mode
- Borders that become invisible in dark mode (`border-gray-200` with no dark variant)

**If the project uses CSS variables for colors** (shadcn pattern), dark mode
is usually automatic — the variables switch values. But verify new semantic
colors are also covered.

---

## 6. Responsive Design

**What to check:** Does the code work across the project's supported breakpoints?

**Common oversights:**
- Fixed widths that overflow on mobile
- Grid columns that don't collapse (e.g., `grid-cols-3` without `md:` prefix)
- Text sizes that are too large on small screens
- Padding/margin that wastes space on mobile
- Horizontal scrolling caused by rigid layouts

**Match the project's responsive pattern:**
```tsx
// If existing code uses this pattern:
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">

// Then new grids should follow the same breakpoints
// Don't use sm: if the project doesn't, don't use xl: if they stop at lg:
```

---

## 7. Typography Hierarchy

**What to check:** Do heading sizes, weights, and spacing match the existing
hierarchy?

**Extract the pattern from existing pages:**
```
Page title:    text-2xl font-semibold tracking-tight
Section head:  text-lg font-medium
Card title:    text-sm font-medium
Body text:     text-sm text-muted-foreground
Label:         text-sm font-medium
Helper text:   text-xs text-muted-foreground
```

**Then apply exactly the same scale.** Don't use `text-3xl` for a page title
if the project uses `text-2xl`. Don't use `font-bold` if the project uses
`font-semibold`. These small differences add up visually.

---

## 8. Icon Consistency

**What to check:** Are icons from the same library used consistently?

**Common icon libraries and their import patterns:**
```tsx
import { Settings, User, Bell } from "lucide-react"       // Lucide
import { Cog6ToothIcon } from "@heroicons/react/24/outline" // Heroicons
import { IconSettings } from "@tabler/icons-react"          // Tabler
```

**Rules:**
- Use the same icon library as the rest of the project
- Match the icon size convention (e.g., `className="h-4 w-4"` or `size={16}`)
- Match the icon style (outline vs solid vs filled)
- Don't mix emoji with icon components

---

## Quick Pass (for small changes)

For minor UI changes (editing a label, adding a field to a form), you don't
need the full checklist. Just verify:

1. Colors reference tokens (not hardcoded)
2. Reusing existing components (not creating new ones)
3. Matches adjacent code style
