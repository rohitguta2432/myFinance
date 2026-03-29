# Design Token Formats

Reference guide for parsing the three major design token formats you may
encounter in a project. Use this when the project has token files.

---

## W3C Design Tokens Community Group Format

The standard format as of the DTCG specification (v1, October 2025).
Files are typically named `.tokens.json` or stored in a `tokens/` directory.

### Structure
```json
{
  "color": {
    "$description": "Color tokens",
    "primary": {
      "$value": "#1a73e8",
      "$type": "color",
      "$description": "Primary brand color"
    },
    "primary-light": {
      "$value": "#4a90d9",
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
  "dimension": {
    "spacing": {
      "xs": { "$value": "4px", "$type": "dimension" },
      "sm": { "$value": "8px", "$type": "dimension" },
      "md": { "$value": "16px", "$type": "dimension" },
      "lg": { "$value": "24px", "$type": "dimension" },
      "xl": { "$value": "32px", "$type": "dimension" }
    },
    "radius": {
      "sm": { "$value": "4px", "$type": "dimension" },
      "md": { "$value": "8px", "$type": "dimension" },
      "lg": { "$value": "12px", "$type": "dimension" }
    }
  },
  "fontFamily": {
    "sans": {
      "$value": ["Geist", "system-ui", "sans-serif"],
      "$type": "fontFamily"
    },
    "mono": {
      "$value": ["Geist Mono", "monospace"],
      "$type": "fontFamily"
    }
  },
  "fontWeight": {
    "regular": { "$value": 400, "$type": "fontWeight" },
    "medium": { "$value": 500, "$type": "fontWeight" },
    "semibold": { "$value": 600, "$type": "fontWeight" }
  },
  "shadow": {
    "sm": {
      "$value": [
        {
          "color": "#0000000d",
          "offsetX": "0px",
          "offsetY": "1px",
          "blur": "2px",
          "spread": "0px"
        }
      ],
      "$type": "shadow"
    }
  }
}
```

### Key properties
- `$value` — The token value (always prefixed with `$`)
- `$type` — The token type: `color`, `dimension`, `fontFamily`, `fontWeight`,
  `duration`, `cubicBezier`, `shadow`, `strokeStyle`, `border`, `transition`,
  `gradient`, `typography`, `number`
- `$description` — Human-readable description
- Groups can be nested to any depth
- References use `{group.token}` syntax: `"$value": "{color.primary}"`

---

## Tokens Studio Format

Used by the Tokens Studio Figma plugin. Similar to W3C but with some differences.
Usually stored as `tokens.json` at the project root.

### Structure
```json
{
  "global": {
    "colors": {
      "primary": {
        "value": "#1a73e8",
        "type": "color"
      },
      "primary-hover": {
        "value": "#1557b0",
        "type": "color"
      },
      "white": {
        "value": "#ffffff",
        "type": "color"
      }
    },
    "spacing": {
      "xs": { "value": "4", "type": "spacing" },
      "sm": { "value": "8", "type": "spacing" },
      "md": { "value": "16", "type": "spacing" }
    },
    "borderRadius": {
      "sm": { "value": "4", "type": "borderRadius" },
      "md": { "value": "8", "type": "borderRadius" }
    }
  },
  "light": {
    "bg": {
      "default": {
        "value": "{global.colors.white}",
        "type": "color"
      }
    },
    "text": {
      "default": {
        "value": "{global.colors.gray-900}",
        "type": "color"
      }
    }
  },
  "dark": {
    "bg": {
      "default": {
        "value": "{global.colors.gray-950}",
        "type": "color"
      }
    },
    "text": {
      "default": {
        "value": "{global.colors.white}",
        "type": "color"
      }
    }
  },
  "$themes": [
    {
      "id": "light",
      "name": "Light",
      "selectedTokenSets": { "global": "enabled", "light": "enabled" }
    },
    {
      "id": "dark",
      "name": "Dark",
      "selectedTokenSets": { "global": "enabled", "dark": "enabled" }
    }
  ]
}
```

### Key differences from W3C
- `value` instead of `$value` (no `$` prefix)
- `type` instead of `$type`
- Spacing values are numbers without units (pixels implied)
- Has `$themes` array for theme configuration
- Token sets (global, light, dark) are top-level groups
- References use same `{group.token}` syntax

---

## Style Dictionary Format

Amazon's open-source token transformation tool. Tokens are usually in a
`tokens/` directory with a `style-dictionary.config.json` at the root.

### Config file
```json
{
  "source": ["tokens/**/*.json"],
  "platforms": {
    "css": {
      "transformGroup": "css",
      "buildPath": "build/css/",
      "files": [
        {
          "destination": "variables.css",
          "format": "css/variables"
        }
      ]
    },
    "js": {
      "transformGroup": "js",
      "buildPath": "build/js/",
      "files": [
        {
          "destination": "tokens.js",
          "format": "javascript/es6"
        }
      ]
    }
  }
}
```

### Token files
```json
{
  "color": {
    "base": {
      "primary": { "value": "#1a73e8" },
      "secondary": { "value": "#5f6368" }
    },
    "font": {
      "primary": { "value": "{color.base.primary.value}" },
      "secondary": { "value": "{color.base.secondary.value}" }
    }
  },
  "size": {
    "font": {
      "sm": { "value": "0.875rem" },
      "md": { "value": "1rem" },
      "lg": { "value": "1.25rem" }
    }
  }
}
```

### Key characteristics
- Uses `value` (no `$` prefix)
- No `type` field (type is inferred from the token name/path)
- References use `{path.to.token.value}` (includes `.value` suffix)
- Build output can be CSS variables, JS modules, iOS, Android, etc.

---

## How to Use Detected Tokens

Once you've parsed the token files, map them to the brand profile:

| Token Category | Brand Profile Field | Usage in Code |
|---------------|--------------------|----|
| `color.*` | Colors section | CSS variables or Tailwind theme colors |
| `spacing.*` / `dimension.*` | Spacing section | Tailwind spacing or CSS variables |
| `borderRadius.*` | Borders & Radius | Tailwind radius or CSS variables |
| `fontFamily.*` | Typography | Tailwind font or CSS variables |
| `fontWeight.*` | Typography | Tailwind font-weight or CSS |
| `shadow.*` | Shadows | Tailwind shadow or CSS variables |
| `duration.*` / `cubicBezier.*` | Animation | Tailwind transition or CSS |

If the project has tokens but doesn't reference them in code (e.g., tokens
exist in Figma but the codebase uses hardcoded values), note this in the
brand profile as an opportunity for alignment.
