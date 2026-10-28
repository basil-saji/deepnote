---
name: Obsidian Intelligence
colors:
  surface: '#121316'
  surface-dim: '#121316'
  surface-bright: '#38393c'
  surface-container-lowest: '#0d0e11'
  surface-container-low: '#1b1b1f'
  surface-container: '#1f1f23'
  surface-container-high: '#292a2d'
  surface-container-highest: '#343538'
  on-surface: '#e3e2e6'
  on-surface-variant: '#c7c4d7'
  inverse-surface: '#e3e2e6'
  inverse-on-surface: '#2f3034'
  outline: '#908fa0'
  outline-variant: '#464554'
  surface-tint: '#c0c1ff'
  primary: '#c0c1ff'
  on-primary: '#1000a9'
  primary-container: '#8083ff'
  on-primary-container: '#0d0096'
  inverse-primary: '#494bd6'
  secondary: '#ffb95f'
  on-secondary: '#472a00'
  secondary-container: '#ee9800'
  on-secondary-container: '#5b3800'
  tertiary: '#4edea3'
  on-tertiary: '#003824'
  tertiary-container: '#00885d'
  on-tertiary-container: '#000703'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e1e0ff'
  primary-fixed-dim: '#c0c1ff'
  on-primary-fixed: '#07006c'
  on-primary-fixed-variant: '#2f2ebe'
  secondary-fixed: '#ffddb8'
  secondary-fixed-dim: '#ffb95f'
  on-secondary-fixed: '#2a1700'
  on-secondary-fixed-variant: '#653e00'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#121316'
  on-background: '#e3e2e6'
  surface-variant: '#343538'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  display-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: -0.015em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 30px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 22px
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.25rem
  margin: 1.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.25rem
---

## Brand & Style

This design system targets researchers, knowledge workers, software architects, and thought leaders who demand a high-performance, focused thinking environment. It blends dark minimalism with futuristic ambient precision—reminiscent of deep terminal spaces elevated with luminous, calibrated accents.

The emotional baseline is deep immersion, intellectual velocity, and understated luxury. Visuals avoid aggressive high-contrast clashes, instead utilizing deep obsidian, graphite, and charcoal planes structured by razor-thin, luminous dividers. Key AI triggers radiate subtle indigo-violet luminescence, imbuing contextual synthesis with a sense of quiet computational intelligence.

## Colors

The system operates strictly within a rich dark environment calibrated across layered obsidian tones:

- **Background & Canvas (`#0E0F12`)**: The ultra-low reflective foundation for global framing and window backgrounds.
- **Surface Level 1 (`#121316`)**: Canvas base for main note viewing and active panes.
- **Surface Level 2 (`#18191E`)**: Elevated cards, toolbars, sidebar panels, and document tabs.
- **Surface Level 3 (`#202228`)**: Interactive hover states, dropdown menus, and popovers.
- **Border Trim (`#2E313A`)**: Subtle, 1px structural hairpins with 60% opacity for quiet boundary separation.
- **Primary Accent (`#6366F1`)**: Electric Indigo driving focal navigation, active tab markers, AI interactions, and key buttons. Includes ambient glow states (`rgba(99, 102, 241, 0.25)`).
- **Secondary Accent (`#F59E0B`)**: Radiant Amber for highlights, pinned metadata, and warning states.
- **Tertiary Accent (`#10B981`)**: Emerald for synced statuses, active collaborator indicators, and semantic success.
- **Text Primary (`#F3F4F6`)**: High legibility off-white for titles and crisp narrative body copy.
- **Text Secondary (`#9CA3AF`)**: Balanced muted neutral for metadata, timestamps, and icons.
- **Text Tertiary (`#6B7280`)**: Whispering grey for inactive tab indicators, shortcut glyphs, and helper labels.

## Typography

The typography bridges editorial clarity with structural ergonomics. 

Headlines utilize **Plus Jakarta Sans** with geometric, humanist apexes that ensure note titles and greeting headers remain inviting and authoritative. Reading and writing contexts utilize **Inter** with generous line heights (`28px` on `16px` body) to alleviate eye strain during long synthesis sessions. 

All UI microcopy, tags, pill badges, and AI interaction anchors leverage medium-to-semibold weights in **Plus Jakarta Sans** to maintain sharp optical definition against dark obsidian surfaces.

## Elevation & Depth

Visual hierarchy uses tonal surface layering and ambient illumination rather than conventional stark drop shadows:

- **Flat/Canvas Tier**: Background canvas sits at `#0E0F12` with no shadow.
- **Layer 1 (Panels & Tabs)**: Background at `#121316` framed with a 1px border (`#2E313A`, 60% opacity).
- **Layer 2 (Cards & Floating Toolbars)**: Background `#18191E`, complemented by a crisp 1px stroke (`#2E313A`) and a subtle ambient occlusion shadow: `0 8px 24px -4px rgba(0, 0, 0, 0.45)`.
- **AI Highlight / Focus State**: Special floating elements (e.g., the 'Ask AI' pill or copilot conversational triggers) leverage an electric violet halo: `0 0 16px -2px rgba(99, 102, 241, 0.35)`.
- **Overlays & Drawers**: Backdrop filter blur (`backdrop-filter: blur(12px)`) with `#121316` tinted at 85% opacity, creating translucent depth across overlapping views.

## Shapes

The design uses balanced curvature that balances functional desktop software density with modern polish:

- **Standard Containers & Cards**: `rounded-md` (`8px`) to `rounded-lg` (`12px`) for note cards, code blocks, and context windows.
- **Interactive Micro-Pills**: Full pill radius (`9999px`) for search input fields, metadata tag filters, status tags, and action badges ('Ask AI').
- **Toolbars & Buttons**: `8px` (`0.5rem`) for compact icon buttons and tab strips, ensuring tight spatial economy without feeling abrasive.

## Components

### Buttons & AI Action Badges
- **Primary Action ('Ask AI')**: Luminous pill button featuring an obsidian-to-indigo gradient background or solid `#18191E` with an electric indigo border (`#6366F1`), violet glow, and spark icon prefix.
- **Secondary Icon Buttons**: 36x36px square pads with `8px` radius, background `#18191E`, subtle `#2E313A` border, holding crisp monochrome iconography with hover transition to `#202228`.

### Search Bar
- Fully rounded pill container (`44px` height) surfaced in `#18191E` with a 1px `#2E313A` border.
- Features a subdued search icon, glowing focus state with `#6366F1` stroke ring, and keyboard shortcut glyph (`⌘K`) anchored at the trailing edge.

### Chips & Filter Pills
- Fully rounded (`9999px`) pills with `8px` horizontal padding and `4px` vertical padding.
- **Default State**: Surface `#18191E`, text `#9CA3AF`, border `#2E313A`.
- **Active State**: Surface `#202228`, text `#F3F4F6`, border `#6366F1`.
- **Status Indicator Dots**: 6px circular nodes styled in emerald `#10B981` (saved/synced) or amber `#F59E0B` (drafting/indexed).

### Note Cards
- Rounded rectangular surfaces (`12px` radius) set to `#18191E` with a 1px outline in `#2E313A`.
- Interior padding of `16px`. Displays title, 2-line excerpt in `#9CA3AF`, word/character count, read time, and associated tag pills along the footer.
- Hover produces a top border accent illumination (`#6366F1` at 50% opacity) and elevates the card via a subtle -2px vertical translation.

### Multi-Tab Document Strip
- Horizontal track resting above the document canvas.
- Each tab features a rounded top edge (`8px`), 1px structural separator, status indicator dot, document title, and a discrete hover-revealed close glyph ('✕').
- Active tab has an obsidian background (`#121316`) matching the note canvas, crowned by a 2px active indicator line in `#6366F1`.

### Copilot Side-Panel & Conversational Bubbles
- Fixed or collapsible docking pane (`#18191E`) bordered with 1px `#2E313A`.
- **User Prompt Bubbles**: Right-aligned, dark indigo-tinted surface (`rgba(99, 102, 241, 0.15)`) bordered with `#6366F1` (30% opacity).
- **AI Response Bubbles**: Left-aligned, `#121316` surface, `#2E313A` border, typography set to `body-md` in `#F3F4F6`, flanked by a distinct glowing AI avatar spark.
- **Bottom Prompt Input**: Integrated input bar with an obsidian field, microphone/attachment utilities, and an elevated circular submit button carrying the `#6366F1` accent.