# DeepNote UI Design Specification

**Document:** `docs/design/DESIGN.md`  
**Version:** 1.0  
**Status:** Implementation specification for the existing JavaFX application  
**Primary references:** `DEEPNOTE UI.jpg`, `deepnote-home-demo.png`, `deepnote-editor-demo.png`, `deepnote-editor-ai-demo.png`

---

## 1. Purpose and authority

This document defines the visual design, screen composition, interaction model, layout rules, component behavior, and acceptance criteria for DeepNote's JavaFX desktop interface. It is intentionally more specific than a general request to “make the UI premium.”

Use it together with the existing Product Requirements Document, Software Design Document, Implementation Roadmap, Coding Standards, and Developer Handbook. Existing product and architecture decisions remain in force unless this document explicitly defines a visual or interaction detail.

### 1.1 Reference image inventory

Keep all four images in `docs/design/` and inspect them before implementation:

| Reference | Role | What to take from it |
|---|---|---|
| `DEEPNOTE UI.jpg` | Original wireframe | Homepage hierarchy, tabs as navigation, note editor, optional right AI panel, and the fundamental no-sidebar layout |
| `deepnote-home-demo.png` | Primary homepage design reference | Refined header, search, filters, grid/list control, Recent section, note-card composition, status bar, overall density |
| `deepnote-editor-demo.png` | Primary editor design reference | Compact top tab strip, note title and metadata, toolbar, readable writing surface, generous but controlled whitespace |
| `deepnote-editor-ai-demo.png` | Primary editor + assistant reference | Editor/assistant split, conversation layout, message styling, contextual composer and panel hierarchy |

The three demo screenshots are the visual references for the finished application. The original wireframe clarifies intended interaction and layout behavior. Treat the supplied blue/charcoal palette in this document as authoritative even where the demo screenshots show purple.

These screenshots are desktop product references, not instructions to reproduce every pixel blindly. Adapt mock data, fake syncing indicators, fake categories and macOS window controls to the actual Windows desktop app and current product capabilities.

### 1.2 Order of precedence

When requirements appear to conflict, follow this order:

1. Existing product requirements and explicit product constraints.
2. This design specification for visual composition and interactions.
3. The supplied reference screenshots.
4. The current implementation, which is a starting point—not a design authority.

Do not preserve poor spacing, repeated placeholder cards, purple styling, or awkward proportions merely because they exist in the current JavaFX implementation.

---

## 2. Product and visual intent

DeepNote is an offline-first desktop note-taking application. Its interface should feel like a thoughtful writing and knowledge tool: calm, precise, fast, private, and professionally finished. It should not look like a generic AI dashboard, a browser admin template, a rough wireframe, or a Java control gallery.

### 2.1 Design principles

- **Writing comes first.** Note content receives the most usable space and must remain comfortable to read for long sessions.
- **Quiet hierarchy.** Use spacing, typography, surface contrast and alignment to create hierarchy before adding color.
- **Blue is an interaction accent, not decoration.** Apply it to primary actions, selected states, focus and a few important links. Do not make every icon, border and card blue.
- **Charcoal surfaces create depth.** Separate the app background, cards, input surfaces and assistant panel through small luminance differences and subtle borders.
- **Compact, considered chrome.** Toolbars and tabs should be legible but should not steal space from the note.
- **Real content, not a screenshot mockup.** The visual quality must come from the actual JavaFX views populated with real TXT-backed notes and real application state.
- **Consistent across screens.** Home, editor and AI panel must look like parts of one application, not three separately themed demos.
- **Avoid visual noise.** No purple glow, excessive gradients, ornamental AI sparkle icons, thick outlines, huge empty hero areas, or arbitrary colored tags.

### 2.2 Non-negotiable constraints

- No left sidebar, icon rail, hidden drawer, collapsed navigation column, or reserved left-sidebar space.
- Use the existing Java 25 + JavaFX 27 + FXML + AtlantaFX 3.0.0 project. Do not start a React/Tauri migration as part of this task.
- Preserve UTF-8 TXT files as the source of truth. Do not add PostgreSQL, pgvector, embedding generation or semantic search in this phase.
- Preserve current local keyword search and local Ollama chat functionality.
- AI is optional. The app must remain useful if Ollama is not running.
- Never claim cloud sync, vault sync or synchronization unless that feature actually exists. This project is currently a local workspace.
- Do not render fake categories, tags, note counts, reading times, last-edited times, model connection statuses or version numbers.
- Never use fabricated demo notes in the real application to make the UI look populated. Sample content is acceptable only in an explicitly isolated development preview or test fixture.

---

## 3. Design tokens

Implement these values in one central JavaFX stylesheet, for example `src/main/resources/com/deepnote/fx/theme/deepnote.css`. Do not scatter arbitrary hex colors throughout controllers or individual FXML files. It is acceptable to retain a small number of semantic colors for success/warning/error states, but those must also be centralized.

### 3.1 Required palette

| Token | Hex | Intended use |
|---|---|---|
| `primary` | `#4C8DFF` | Primary action, active tab indicator, selected toggle, keyboard focus, essential interactive emphasis |
| `secondary` | `#8AB4FF` | Subtle link emphasis, secondary icon highlight, hover emphasis where appropriate |
| `tertiary` | `#A6C8FF` | Occasional tertiary emphasis; use sparingly |
| `background` | `#0D0F12` | Main application canvas and deepest surfaces |
| `surface` | `#1B1D22` | Note cards, inputs, assistant panel, elevated controls |
| `border` | `#30343D` | Structural outlines, separators and subtle control borders |
| `text-primary` | `#F1F3F6` | Main text, headings, note titles and important control labels |
| `text-muted` | `#969DA9` | Metadata, timestamps, secondary labels, disabled descriptions and helper text |
| `font-headline` | `Inter` | Headings and prominent text |
| `font-body` | `Inter` | Note content and standard interface text |
| `font-label` | `JetBrains Mono` | Compact status and technical metadata, keyboard hints and small counters |

Suggested additional semantic tokens, only where a state needs a distinct meaning:

| Semantic token | Suggested value | Use |
|---|---|---|
| `success` | `#34D399` | Saved/available status dot; pair with text, not color alone |
| `warning` | `#FBBF24` | Unsaved changes or a non-fatal warning |
| `danger` | `#F87171` | Destructive actions and errors |
| `surface-hover` | `#22252B` | Subtle hover fill derived from `surface` |
| `surface-raised` | `#202329` | Slightly raised input/control surface when necessary |
| `border-subtle` | `#252932` | Low-emphasis dividers, if `border` is too strong |

The required eight core color values and three font values must be used exactly. Additional tokens must support the palette, not introduce a competing color identity.

### 3.2 Purple removal requirement

The active JavaFX UI must not use purple/violet/indigo as an accent. Audit all visible UI states, including:

- primary and secondary buttons;
- selected tabs and toggles;
- keyboard focus rings and caret/selection highlights;
- hyperlinks and selected text;
- assistant icon, messages and suggested prompt controls;
- loading indicators, hover effects and disabled states;
- AtlantaFX default accent styles.

The app currently installs AtlantaFX `PrimerDark`; ensure the custom stylesheet actually overrides any default accent/focus colors that remain purple. Do not assume that defining `-primary` automatically changes every default control. Inspect the rendered app and test focus/selected/hover states.

The legacy Swing theme may contain old colors. Do not perform an unrelated wholesale Swing refactor in this task, but ensure the active JavaFX launcher displays the required palette.

### 3.3 Color usage rules

- Primary blue backgrounds are reserved for the most important action in a context, such as **New note**, the active **Grid/List** selection, or **Send** in the assistant.
- Secondary and tertiary blues normally appear as text/icon accents, not large filled blocks.
- Most surfaces should remain charcoal or near-black. Cards should not have bright outlines in the resting state.
- Hover and focus states must be perceptible but restrained.
- A selected tab can use a thin blue underline and a subtle surface fill; do not give the whole tab a large glowing blue background.
- A status dot may use green for success. Do not use color alone to convey saved, unsaved or offline states; include a concise label or accessible tooltip.
- Maintain readable contrast for text and control labels. Muted text must remain readable at normal desktop viewing distance.

---

## 4. Typography and spacing

### 4.1 Fonts

Use Inter for headlines and body copy, and JetBrains Mono for compact metadata. If either font is not installed, use platform fallbacks such as Segoe UI and Consolas/monospace. Missing optional fonts must never prevent the UI from launching. Do not require the user to install fonts just to run DeepNote.

### 4.2 Type scale

Use the following as the starting scale; adjust individual values slightly only when needed to match the references and preserve fit.

| Role | Suggested size | Weight | Notes |
|---|---:|---|---|
| Homepage greeting | 32–36 px | Bold / Extra bold if available | Prominent but not oversized |
| Editor note title | 30–34 px | Bold | Main editor heading |
| Card title | 15–17 px | Semibold / Bold | Must be clearly distinct from preview text |
| Assistant panel title | 15–17 px | Semibold | Smaller than the note title |
| Note body | 16–18 px | Regular | Comfortable, readable line length and wrapping |
| Standard UI body | 13–14 px | Regular | Buttons, prompts, general interface |
| Secondary description | 12–13 px | Regular | Card previews and descriptions |
| Section label | 10–11 px | Semibold, slight tracking | “RECENTS”, “NOTES” and compact uppercase headings |
| Metadata/status | 10–11 px | Regular | JetBrains Mono; never use for the main note body |

Do not make all text the same size. Do not use a large headline followed by tiny body copy. Use line spacing and weight to create readable hierarchy.

### 4.3 Spacing scale

Use a consistent 4 px base rhythm. Common spacing values: 4, 8, 12, 16, 20, 24, 32, 40 and 48 px. Do not assign unrelated margins throughout the controller.

- Control internals: 8–12 px.
- Between related controls: 8–10 px.
- Between a section heading and its content: 12–16 px.
- Between major homepage sections: 32–44 px.
- Homepage horizontal padding: approximately 32–40 px at a 1280 px-wide window.
- Editor content horizontal padding: approximately 32–48 px depending on available width.
- Keep card padding near 16–20 px, consistently across the grid.

---

## 5. Window chrome and global shell

### 5.1 Overall frame

The application should feel like a coherent desktop workspace. Use the native Windows title bar/window controls unless there is a tested, deliberate reason to implement a cross-platform custom title bar. The reference screenshots contain macOS-style traffic-light controls; do not draw fake red/yellow/green macOS controls inside a normal Windows title bar.

Do not place a large decorative frame around the entire UI, add unnecessary outer margins that waste space, or use a thick scrollbar. The application content should fill the available window naturally.

### 5.2 Global top area

Use a compact two-level application chrome:

1. **Brand/status row:** the DeepNote mark/name on the left; a truthful local/model status on the right. Keep its height around 36–44 px. The status may say `Local workspace` and show the configured Ollama model if known.
2. **Tab strip:** Home/editor tabs and the `+` action in one continuous horizontal row. Tabs should be compact, with clear active state and close controls. Do not put the `+` button on a separate row above the tabs.

The app may show a settings action at the right of the chrome, but it must open a real settings surface or dialog. If no settings implementation exists yet, do not display an inert settings button merely for decoration.

The header must not consume excessive vertical space. The main content area should remain the focus.

### 5.3 Status bar

Use a slim bottom status bar, about 24–30 px high, with a subtle top border and background consistent with the shell.

Show only truthful, useful information. Examples:

- Home: `Local workspace` and a real note count if the count is already loaded.
- Editor: `UTF-8`, `TXT`, actual save state (`Saved`, `Unsaved changes`, `Saving…`), and optionally accurate cursor line/column while editing.
- AI panel: the configured model and reachable/unavailable status only if availability was actually checked.

Do not say `Synced`, `Synced to Vault`, or show storage capacity figures unless those facts are implemented and accurately measured. Do not put decorative version numbers in the header unless read from real application-version metadata.

---

## 6. Navigation and tab behavior

Tabs are the primary way to move between Home and open notes. There must be no left navigation sidebar.

### 6.1 Tab appearance

- Home is a regular tab, not a separate permanent page outside the tab system.
- Each open note has one tab displaying its title.
- Active tab: subtle `surface` fill and a thin 2 px `primary` underline or equivalent restrained indicator.
- Inactive tab: transparent or background-colored, with muted text.
- Tab close button is visible on the active tab and on hover/focus for inactive tabs. It must have a usable hit target, not just a tiny glyph.
- Truncate long titles gracefully; expose the full title via tooltip.
- Show unsaved status with a restrained dot or indicator and a tooltip/accessible text. Do not use a green dot to indicate unsaved changes.
- A plus button immediately after the tabs opens a new Home tab. Its tooltip must explain its action.

### 6.2 Required navigation rules

1. Pressing `+` opens a new Home tab and selects it.
2. Selecting a note inside a Home tab replaces that Home tab with the note editor.
3. If that note is already open in another tab, switch to the existing editor tab instead of opening a duplicate.
4. Switching between tabs must not lose unsaved editor buffers.
5. Closing a dirty editor tab must offer Save, Discard and Cancel. Save must complete successfully before the tab closes; if saving fails, keep the editor open and display the error.
6. The app must never end up with a completely blank workspace. If the final tab is closed, create or retain one Home tab.
7. Renaming a note updates the visible editor title and tab title without duplicating the tab.
8. Deleting a note requires confirmation and closes any tab for that note after successful deletion.
9. If a Home tab is converted to a note editor, the editor belongs to that tab; do not spawn unrelated windows.

### 6.3 Keyboard shortcuts

Implement only shortcuts that work reliably, and show platform-correct labels/tooltips:

- `Ctrl+N` (platform accelerator): create a note or open the app's new-note dialog.
- `Ctrl+K`: focus and select the homepage search field; if the product chooses to support search in the editor, it may return to Home first.
- `Ctrl+S`: save the active note.
- `Ctrl+W`: close the active tab with unsaved-change protection.
- `Ctrl+J`: toggle the AI panel for the active note, only if this shortcut is supported consistently.
- `Esc`: close transient dialogs/popovers or dismiss assistant focus only when that does not discard content.

Do not display a keyboard hint for an action that is not actually wired.

---

## 7. Home screen

The Home screen is the only note-browsing surface. It should closely follow `deepnote-home-demo.png`, while using live note data.

### 7.1 Home layout, top to bottom

1. Greeting/action header.
2. Search field.
3. Optional category/tag filter row, only if real category/tag data exists.
4. Grid/List segmented control, aligned to the right side of the controls row.
5. Subtle separator between search/filter controls and the note collection.
6. Recent notes section, when applicable.
7. All Notes section (or a clear search-results section while filtering).
8. Slim global status bar at the bottom of the window.

Keep the content aligned to a consistent left edge. At a 1280 px-wide window, use roughly 32–40 px left/right content padding and let cards occupy the available width. Do not put the content in a narrow fixed-width column with a large unused right half.

### 7.2 Greeting and actions

- Show `Good morning 👋` as the default visual reference. If dynamic greeting by local time is implemented, it must be correct; otherwise use a stable greeting rather than pretending to determine the time.
- Keep the greeting in the 32–36 px range and use the headline font.
- Align a prominent `+ New note` primary action on the right of the greeting row.
- Settings, when implemented, is a compact secondary icon button with a tooltip and accessible label.
- Do not place a random colored dot beside the greeting. The greeting can use the emoji as the reference does, but do not add arbitrary decoration.

### 7.3 Search

- Full-width search field aligned with the content area; about 44–50 px high.
- Use `surface` fill, `border` outline, 8–10 px corner radius and muted prompt text.
- Prompt: `Search notes by title or content` or similarly accurate wording. Do not promise search across summaries/AI insights because those indexes do not exist.
- Optional magnifier icon at left and platform-aware keyboard shortcut at right.
- Search filters the already-loaded TXT-backed notes using existing `LocalSearchService` in a case-insensitive manner. Empty query shows all eligible notes.
- Update results as the query changes, without blocking the UI thread for expensive work.
- Provide a clear-query affordance if practical.
- An empty query result displays a helpful empty state with a suggestion to try another title or keyword. Do not retain stale cards underneath the empty state.

### 7.4 Categories and tags

The reference image includes category chips. The present data model does not support categories/tags in the TXT-backed implementation. Therefore:

- Do not fabricate `Ideas`, `Design System`, `Sprint Notes`, `AI Research`, `Product Spec`, or generic tag chips for real notes.
- Do not reserve a large empty row where those chips would have appeared.
- If a real category/tag feature is added later, show a compact filter-chip row between Search and Grid/List. The selected chip uses a restrained primary-blue fill; inactive chips use neutral surfaces/borders.
- Until that feature exists, place Grid/List directly under the search control with sensible spacing.

### 7.5 Grid/List control

- Place the two-state Grid/List segmented control on the right side of the control row.
- Use simple, consistent grid/list icons plus a text label where space permits. Use the selected state to indicate the current mode; the active segment may use `primary` fill, while the inactive segment remains a neutral surface.
- Changing view mode must rearrange the same real note collection, not reload or lose data.
- Grid mode is the default.
- List mode displays compact full-width rows with title, preview, metadata and modified time where available.

### 7.6 Recent notes section

- Section label: `RECENTS`, styled as a compact uppercase label in JetBrains Mono or an equivalent label style.
- A real count may appear beside the section label.
- Show up to the three most recently modified notes, ordered by actual file modification time.
- Cards are clickable, have a subtle hover state and open notes in the selected Home tab.
- Do not show a `View timeline` link unless a timeline view is implemented.
- Avoid an awkward double listing when the vault contains only a few notes: if there are three or fewer notes and every note would appear in both Recent and All Notes, prefer omitting the Recent section until there are more than three notes, or otherwise use a clearly deliberate recent/collection distinction. Do not render two identical blocks simply because a template created both sections.

### 7.7 All Notes section

- Section label: `NOTES` plus a real count, e.g. `NOTES · 18`.
- Optional sort control must be right-aligned and functional. The default sort can be actual last-modified time descending. Do not show `Last Modified` if the menu cannot change the sorting.
- Grid cards should fill the available horizontal space rather than staying at a fixed 280 px width. At a 1280 px application width, target three approximately equal columns. At intermediate widths, reduce to two columns; at narrow widths, one column. Calculate card widths from actual available content width, including gaps.
- At a typical 1280 px layout, cards should generally be around 340–370 px wide if three columns fit; don't hard-code this width in a way that creates unused blank space.
- In list mode, cards become full-width rows with a more compact height.
- Search mode should relabel the collection as `SEARCH RESULTS` and show the real result count when useful.

### 7.8 Note card anatomy

Each grid card should have a coherent top-to-bottom hierarchy:

1. Optional category chip on the left, only when real category metadata exists; actual modified time on the right.
2. Note title, with one or two lines maximum and truncation if necessary.
3. One- to three-line preview based on the real note content. Collapse whitespace for preview only; never alter the saved note content.
4. A subtle divider or spacing break.
5. Footer with real word count and, optionally, estimated reading time calculated consistently from the actual word count.

Style:

- Background: `surface`.
- Resting border: subtle `border` or `border-subtle`.
- Radius: approximately 10–12 px.
- Padding: around 16–20 px.
- Hover: slightly lighter surface plus subtle border adjustment; no bright glow.
- Card title must have stronger contrast/weight than preview and metadata.
- Cards within a row align consistently and have enough height to avoid visual jitter.
- Whole card opens the note, but any future nested action buttons must not trigger the card click unintentionally.

Metadata rules:

- Word count must come from the actual note content.
- Character count must use the actual content length and must state whether whitespace/newlines are included if this is shown in help text.
- Reading time is a transparent estimate, not a factual duration; calculate it from a defined words-per-minute constant and round sensibly, or omit it.
- Modified dates/times must come from filesystem metadata. Handle missing/unavailable timestamps gracefully.
- Never show fake identifiers like `RFC-04` unless the application really assigns those identifiers.

### 7.9 Home empty states

- No notes: a quiet, useful message and a clear `New note` action. Do not display a giant empty panel for no reason.
- No search results: say that no notes match the query and suggest adjusting the keywords, with an easy way to clear search.
- Failed note-directory read: show a proper error and retry or diagnostic action. Do not silently present an empty workspace as if there are no notes.
- Loading: use a small inline loading state only when a real loading operation is in progress.

---

## 8. Note editor

Follow `deepnote-editor-demo.png` for the main editor and `DEEPNOTE UI.jpg` for the behavior of the editor tab and toolbar.

### 8.1 Overall layout

- The selected note occupies the main workspace beneath the tab strip.
- Keep the note title and metadata in a compact header region.
- Place editor actions in a right-aligned, horizontally aligned toolbar.
- Add a subtle divider below the header before note content starts.
- The writing surface must use the available height and scroll vertically when needed.
- Center the readable text column within the available editor region, using a sensible maximum width of roughly 900–960 px on wide windows. Keep it left-aligned within that column; avoid centering each line or running text across the full ultra-wide window.
- Editor left/right padding should shrink gracefully as window width decreases.

### 8.2 Editor header

- Note title in approximately 30–34 px Inter bold.
- Metadata below or beside title: real word count, real character count and optional estimated reading time. Use small neutral text and separators.
- The demo includes a note ID and last-edited time; do not add an ID unless one exists. Use real file modification information only when it is genuinely available.
- Dirty state should be clear with a small indicator or `Unsaved changes` state, not a loud warning banner.
- Keep the title and metadata left-aligned. Keep action controls on the right at typical desktop widths; when width is constrained, allow the toolbar to wrap or move below the title cleanly rather than overlap.

### 8.3 Editor toolbar

Use compact icon buttons, with text labels where essential. Every icon-only button must have an accessible label and tooltip. Use consistent icon sources/sizing; don't mix arbitrary Unicode symbols, emoji and unrelated icon styles.

Expected actions:

- **Edit/Rename title:** the rename action must open a usable prompt or inline editor and update the tab title after success.
- **Delete:** destructive style is restrained; always confirm before removing a TXT file.
- **Save:** primary or secondary treatment depending on whether unsaved changes exist; Ctrl+S should work.
- **Ask DeepNote:** primary blue action that opens/toggles the right assistant panel. This should be the visually strongest editor action, but it must not use purple.

Only show actions that actually work. If an action is not implemented, omit it instead of drawing a decorative button.

### 8.4 Editing surface and content correctness

- Current note contents are plain UTF-8 text in `.txt` files. Do not silently convert them to another format.
- Preserve line breaks, punctuation, blank lines, Unicode characters and the user's exact content when loading/saving.
- Use a readable Inter body font around 16–18 px and comfortable line spacing. Wrap long lines and allow vertical scrolling.
- Use a plain-text editor unless rich-text/Markdown parsing is explicitly implemented in a later feature. Do not visually format `**bold**` or other markup while saving literal changes unless the design has a consistent, lossless behavior for it.
- The editor must remain responsive for large notes. Avoid performing file I/O or AI calls on the JavaFX Application Thread.
- Do not show fake autosave status if saves only happen on explicit Save. If autosave is implemented, debounce it, display truthful `Saving…`/`Saved`/error states, and ensure the close behavior cannot silently discard content.

### 8.5 Save, rename and delete states

- Saving: temporary `Saving…` feedback, disabled duplicate save if necessary, then `Saved` after a successful write.
- Save failure: present a clear error and keep the edited buffer open and dirty.
- Rename: validate according to `NoteService` title rules; show a useful message if invalid or colliding with an existing filename.
- Delete: confirmation dialog must identify the actual note title. The note disappears from Home only after the deletion succeeds.
- Close dirty editor: Save / Discard / Cancel. A failed save prevents closing.

---

## 9. DeepNote AI side panel

The optional right-hand panel appears only for an active editor tab. It is a companion to note writing, not a separate AI-themed dashboard.

### 9.1 Placement and sizing

- Place the panel on the right side of the editor, separated by a subtle vertical divider.
- Target width: approximately 340–380 px at a 1280 px-wide window, with a sensible minimum around 300 px. Avoid letting the panel consume more than about 38% of the total application content width during normal desktop use.
- The editor retains the remaining space and its own scrolling/content width.
- Prefer a `SplitPane` or equivalent arrangement that maintains a stable divider and allows reasonable resizing if feasible. The panel must not push the editor into an unusably narrow column.
- Closing/collapsing the panel restores the editor's width without reloading or resetting the note.
- Do not show the panel on the Home screen.

### 9.2 Panel structure

Top to bottom:

1. Compact header with `DeepNote AI`, truthful Ollama availability/model status, and close/collapse plus optional clear-conversation controls.
2. Conversation viewport that can scroll independently.
3. Initial welcome/empty state when no messages exist, with two to four suggested prompts that genuinely work.
4. User/assistant messages, visually distinguishable without purple.
5. Composer anchored to the bottom of the panel.
6. Small privacy/availability note only if it is accurate and does not dominate the panel.

The header should be around 48–56 px high. Use `surface` as the panel base, restrained borders, and neutral separators. The panel must look integrated with the editor.

### 9.3 Empty state and suggested prompts

When there is no conversation, show a compact introduction such as `Chat with this note` and a single clear sentence explaining that questions use the active note. Suggested prompts can include:

- `Summarize this note`
- `Explain it simply`
- `List key takeaways`
- `What details are missing?`

Each prompt button must populate/submit the actual request using the same AI flow as the composer. Suggested prompts should not remain visible in a way that crowds an established conversation.

### 9.4 Messages and response presentation

- User prompts align to the right or use a distinct but understated surface.
- Assistant responses align to the left and use readable Inter text, line spacing, bullets and paragraphs where returned by the model.
- Use neutral surfaces. Blue can be a small accent for active/selected controls; do not turn every user bubble or assistant card bright blue.
- Long messages wrap and scroll correctly; the composer remains anchored at the bottom.
- Do not fabricate response timing, token count or `Connected` state. Only show timing if measured and useful; only show connected state after an actual health check/request establishes it.
- Provide copy/insert actions only if they are functional. Copy must copy the actual response. Insert must insert the chosen response into the note in a predictable way, preserve existing text, mark the note dirty and allow undo where possible.

### 9.5 Composer

- Rounded neutral input at the bottom, with placeholder `Ask about this note…` or similarly accurate wording.
- Send icon/button uses `primary` blue but should not be oversized.
- Enter sends; Shift+Enter inserts a newline. If the chosen JavaFX control does not support this naturally, implement and test key handling explicitly.
- Disable sending when the prompt is blank or a request is in progress. Avoid duplicate requests.
- Display a small, honest `Thinking…`/loading state while waiting.
- On failure, keep the conversation and the user's ability to retry; show a readable local-model/network error.
- Use the active note's current editor buffer as context, not stale `Note.content` from disk.

### 9.6 Local-only and conversation rules

- Reuse `AIAssistantService`, `ContextManager` and `OllamaChatModelClient`.
- Execute chat requests in JavaFX `Task`/`Service` or another correct background mechanism. Never block the JavaFX Application Thread with the Ollama HTTP request.
- Keep conversation histories separated by note and bounded as the current context manager intends.
- Do not call a cloud AI API or send note contents to any external service.
- If Ollama is stopped, the editor and all note actions remain usable. Display AI as unavailable and explain the recovery path without crashing the application.
- Panel close/reopen must not unexpectedly reset the in-session conversation unless the user explicitly clears it.

---

## 10. Control styling and interaction feedback

All controls must look like a single design system rather than default JavaFX controls with a few colors changed.

### Buttons

- Primary: `primary` fill, high-contrast label, rounded radius around 7–9 px; use sparingly.
- Secondary: `surface` fill with subtle border and primary text.
- Ghost/icon: transparent or low-contrast surface until hover/focus; always accessible via tooltip/label.
- Destructive: neutral by default, danger color used for text or confirmation emphasis; avoid a large solid-red button unless required.
- Hover: small change in surface or border, no glow effect.
- Pressed: visible but brief state; keep text readable.
- Disabled: visibly disabled without making labels unreadable.

### Inputs

- Background: `surface` or a slightly raised neutral surface.
- Border: `border`; active focus: `primary` border or subtle focus ring.
- Prompt/placeholder: `text-muted`.
- Selected text/caret: use the blue theme and retain contrast.
- Avoid a bright border at all times; focus should be state-dependent.

### Cards and surfaces

- Use restrained 10–12 px rounded corners.
- Prefer thin subtle borders over shadows; a very subtle shadow is acceptable only when it improves hierarchy and renders correctly in JavaFX.
- No gradients as a substitute for layout quality.
- Avoid nested bordered boxes for every element. Use spacing and typography wherever those provide sufficient grouping.

### Icons

- Use one consistent icon family and size scale. Prefer vector icons or a consistent icon library already available in the project; if a new dependency is proposed, justify it and keep it lightweight.
- Do not mix emoji, miscellaneous Unicode characters and unrelated icon styles to represent normal toolbar actions. The greeting emoji is the one intentional exception from the reference.
- Icon-only controls need accessible text through tooltip and accessible role/text where practical.

### Scrolling

- Use clean, narrow scrollbars with a subtle track/thumb that remain discoverable.
- Homepage content scrolls vertically as a whole; horizontal scroll should not be needed for normal layouts.
- In the editor-with-AI state, the editor and conversation areas may scroll independently while the AI composer remains at the panel bottom.

---

## 11. Responsive and window-size behavior

Although the target is desktop, the app window can be resized. Use the space intelligently and prevent clipping.

- Suggested default window size: approximately 1280 × 820 px or comparable desktop proportions.
- Suggested minimum: around 960 × 620 px, subject to the actual desktop controls and screen availability.
- At 1280 px content width, show three note-card columns in Grid mode where feasible.
- At intermediate widths, reduce the number of columns rather than shrink text/cards into unreadable slivers.
- At narrow widths, use one column and allow toolbar actions to wrap or move to a second row without overlap.
- The Home page must not leave a huge empty right region because cards have a fixed 280 px width. Cards must respond to the container's actual width.
- The editor content column has a maximum readable width, but the layout itself should fill the application area naturally.
- When the AI panel is opened, adapt the editor column and panel width to the available space; keep the editor usable and scrollable.
- At reduced window height, the content area scrolls; the tab bar and status bar should remain in their intended positions.
- Do not depend on hard-coded screen coordinates to place controls.

---

## 12. Empty, loading, error and data states

Design and test these states; they are part of the actual product, not optional polish.

| State | Expected presentation |
|---|---|
| Empty notes directory | A welcoming, compact empty state and a working New Note action |
| No search results | Clear message with an easy route to clear/edit the query |
| Reading notes fails | Error notification/state; do not silently treat failure as an empty directory |
| Unsaved editor | Clearly marked; save/discard/cancel protections work |
| Saving | Honest temporary progress state; controls prevent duplicate conflicting saves if required |
| Save failure | Clear message, unsaved content retained, retry possible |
| Ollama available | Show availability only when known; allow sending real note-grounded requests |
| Ollama unavailable | No crash; show local-model availability error, keep all non-AI features working |
| AI request pending | Show `Thinking…` or a spinner; prevent duplicate submission |
| AI request fails | Show a retryable error without deleting conversation or the user's question |
| Note delete confirmed | Remove the note and any open tab only after successful deletion |
| Note rename collision/invalid name | Show validation feedback and keep the existing title/note intact |

Use JavaFX `Alert`/dialogs only where appropriate; for routine status feedback, prefer a small inline status or non-blocking notification. Avoid interrupting the user with alerts for every successful save.

---

## 13. JavaFX implementation guidance

This is a visual and behavior specification for the existing JavaFX project, not a request to build another technology stack.

### 13.1 Keep the UI maintainable

- Use FXML for substantial layouts and Java controllers for behavior.
- Use one central CSS stylesheet for design tokens and reusable styling. Split into multiple stylesheets only if it materially improves maintenance.
- Avoid a single very large controller programmatically building every section of the application. Separate responsibilities into focused UI components/controllers where that makes behavior easier to test.
- A reasonable decomposition is Home view/controller, note-editor view/controller, assistant-panel view/controller, reusable note-card component, and a small workspace/tab coordinator. Adapt names to existing project conventions rather than refactoring solely for naming.
- Keep business logic in the existing services. Controllers should coordinate UI events and service calls rather than reimplementing note storage or search.
- Do not expose absolute filesystem paths in user-facing labels.

### 13.2 AtlantaFX interaction

AtlantaFX can provide the base dark look, but the active JavaFX styles must override the defaults where they conflict with this spec. Verify actual computed/rendered states, especially selected toggle, active tab, focus, selection, text-area caret/highlight and button hover states. Do not merely define custom tokens while leaving important controls on the default violet accent.

### 13.3 Threading

- Note filesystem reads/writes and AI requests must not freeze the JavaFX Application Thread.
- Use JavaFX `Task`/`Service` for operations that can block. Update UI elements only on the JavaFX Application Thread.
- Handle task success/failure/cancellation and ensure disabled/loading controls are restored in every completion path.
- Do not read mutable JavaFX controls directly from a worker thread. Capture the prompt and active note text before starting the task.

### 13.4 Dependencies and persistence

- Reuse current dependencies and services where possible.
- Do not add an icon, animation or styling dependency unless it materially improves the result and is compatible with the existing stack.
- Do not introduce PostgreSQL, pgvector, embeddings, indexing or semantic search while implementing this visual pass.
- TXT content remains canonical. Maintain compatibility with existing notes in `~/DeepNote/notes` and the configured notes directory.

### 13.5 Preserve migration safety

- Keep useful legacy Swing files unchanged unless they are directly required to make the JavaFX launcher the active path.
- Ensure the JavaFX Maven launch command targets `com.deepnote.fx.DeepNoteFxApplication`.
- Do not claim the JavaFX migration is complete merely because `javac` succeeds or a process stays alive.
- Keep documentation accurate about the actual launcher, current behavior, tests and any remaining limitations.

---

## 14. Visual acceptance checklist

Before declaring the UI pass complete, launch the real JavaFX application and inspect screenshots of the actual running UI. Verify all of the following.

### Home screen

- [ ] The layout follows the hierarchy of `deepnote-home-demo.png` while using real notes.
- [ ] The content area uses the available width; there is no giant unused right half caused by fixed card widths.
- [ ] At a typical 1280 px window, three cards fill the row in Grid mode if the available width permits.
- [ ] Heading, search, controls, section labels and cards share consistent alignment.
- [ ] Recent/Notes sections do not create visually pointless duplicate blocks when the total notes count is small.
- [ ] Search filters actual notes and reports an accurate empty state.
- [ ] Category chips and sort/timeline affordances are shown only when their behavior/data exists.
- [ ] Grid/List toggle works and is styled consistently.
- [ ] No placeholder sample notes or fabricated metadata appear in the real app.

### Editor

- [ ] The layout follows `deepnote-editor-demo.png` with a compact toolbar and a comfortable text-reading column.
- [ ] Note title, metadata and toolbar do not overlap when the window is resized.
- [ ] Text wraps and scrolls correctly; editing preserves plain-text contents.
- [ ] Save/rename/delete/tab close work against the existing TXT service.
- [ ] Unsaved content cannot be silently lost.
- [ ] No sidebar or unnecessary left-navigation space appears.

### Editor + AI

- [ ] The panel opens on the right only for an active editor and closes without resetting editor state.
- [ ] The divider/panel leaves adequate space for readable note content.
- [ ] The conversation and composer have the hierarchy of `deepnote-editor-ai-demo.png`, translated to blue/charcoal.
- [ ] AI uses the current editor buffer, note-specific session history and local Ollama.
- [ ] Waiting/error/unavailable states are clear, recoverable and non-blocking.
- [ ] All suggested prompts and visible response actions actually work.

### Theme and quality

- [ ] The palette uses `#4C8DFF`, `#8AB4FF`, `#A6C8FF`, `#0D0F12`, `#1B1D22`, `#30343D`, `#F1F3F6` and `#969DA9` consistently.
- [ ] No purple/violet accent remains in the active JavaFX UI, including focused and selected default controls.
- [ ] Inter and JetBrains Mono are used where available, with robust fallbacks.
- [ ] Hover, pressed, selected, disabled and focus states look intentional and remain readable.
- [ ] The app behaves acceptably at the default window size and after resizing.
- [ ] All major actions have meaningful labels/tooltips and keyboard focus is visible.
- [ ] `mvn test` and `mvn package` results are reported honestly. Visual claims are backed by a screenshot or actual window inspection.

### Definition of done

The UI pass is done only when the real JavaFX application launches, has the complete Home/editor/AI states, behaves correctly with real local data, visibly follows this specification, and has been visually inspected. A successful compile by itself is not completion.

---

## 15. Instructions for the implementation agent

Read this document and the four images in `docs/design/` before editing code. Inspect the current repository and launcher configuration first. Then implement the UI in a few verifiable steps:

1. Establish design tokens and the shared shell/tab layout.
2. Complete the Home layout and responsive real-data cards.
3. Complete the editor layout and correct its toolbar, metadata and reading column.
4. Complete the right AI panel, including loading and failure states.
5. Verify all navigation and data behavior, then refine the rendered appearance against the screenshots.

Do not stop after creating a plan or a rough mockup. Do not replace real data with sample content. Do not add future database/semantic-search work. If the environment prevents Maven dependency resolution or prevents visual inspection, report the exact blocker and the remaining verification steps rather than claiming success.
