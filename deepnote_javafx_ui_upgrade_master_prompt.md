# DeepNote JavaFX UI Polish & Bug-Fix Prompt (Iteration 2)

**Role:** Principal JavaFX Engineer  
**Scope:** Fix the remaining windowing, layout, AI panel rendering, scrollbar, and dialog issues in `src/main/java/com/deepnote/fx/*` and `src/main/resources/com/deepnote/fx/*`.  
**Constraint:** Keep everything 100% dynamic and functional with existing services. Do not break existing tests.

---

## 1. Top Bar Window Controls, Native Dragging & Screen Fitting (`foundation.fxml`, `FoundationController.java`, `DeepNoteFxApplication.java`)

**Current Bugs:**
- The top-right corner has a `"● Saved"` / `"● Local Workspace"` pill instead of the 3 Windows window-control buttons (`—`, `□`, `✕`).
- The window cannot be dragged from the top bar, and screen/full-screen fitting is broken.

**Exact Fixes:**
1. **Remove Top-Right Status Pill:** Delete `topStatusPill`, `topStatusDot`, and `topStatusLabel` (`"Saved"` / `"Local Workspace"`) from `foundation.fxml` and `FoundationController.java`.
2. **Add Windows Top-Right Control Buttons (`—` Minimize, `□` Maximize/Restore, `✕` Close):**
   - Place an `HBox` (`alignment="CENTER_RIGHT"`, `spacing="0"`) at the far right of `.top-shell-bar`:
     - **Minimize (`—`):** `stage.setIconified(true)`
     - **Maximize / Restore (`□` / `❐`):** Toggles custom maximized state (fit to `Screen.getPrimary().getVisualBounds()`, storing/restoring previous `x, y, width, height`) so it never covers the Windows taskbar and fits the screen 100% cleanly. Also support double-clicking the empty area of `.top-shell-bar` to toggle maximize/restore.
     - **Close (`✕`):** Triggers the existing `handleWindowCloseRequest` (checking dirty tabs before closing). Style hover background as `#E81123` (Windows red) with `#FFFFFF` icon.
   - Style each window button as `44px × 42px`, borderless `#0D0F12` bg, `#969DA9` crisp vector `SVGPath` icon, `#22252B` hover bg (except Close which hovers `#E81123`).
3. **Make Window Draggable & Screen-Fitted:**
   - Attach `onMousePressed` and `onMouseDragged` handlers to `.top-shell-bar` (ignoring clicks on interactive buttons/tabs) that move the `Stage` (`stage.setX(event.getScreenX() - dragOffsetX)`, `stage.setY(event.getScreenY() - dragOffsetY)`). If dragged while maximized, un-maximize proportionally first.
   - In `DeepNoteFxApplication.java`, clamp the initial window size to `Screen.getPrimary().getVisualBounds()` so it never overflows smaller laptop screens, and add `StageStyle.UNDECORATED` (or ensure the custom top bar acts as the complete native-feeling Windows title bar with resize-border support on window edges).

---

## 2. Top-Left Back Button in Note Editor (`foundation.fxml`, `FoundationController.java`, `EditorViewController.java`)

**Current Bug:** When viewing a note in the Editor, there is no Back button on the top-left corner to return to the previous/Home screen.

**Exact Fix:**
- Add a compact **Back icon button (`←`)** (`28x28px` rounded square, `.top-back-btn`, crisp left-arrow `SVGPath`, Tooltip `"Back to Home"`) at the **far left of `.top-shell-bar`** (before `tabStrip`) OR at the top-left of the Editor view.
- **Behavior:** When in a Note Editor tab, clicking the Back button navigates back to the previous screen / switches the tab back to the **Home View** (prompting Save/Discard/Cancel if the current note has unsaved changes, or switching to the Home tab). Hide or disable the Back button when already on the Home screen.

---

## 3. AI Side Panel Fixes (`assistant-panel.fxml`, `AssistantPanelController.java`, `foundation.css`)

Look at the attached screenshot of the AI Side Panel (`DeepNote AI`):

1. **Fix Stretched Blue Sparkle Badge Icon:**
   - **Why it happens:** In `assistant-panel.fxml`, `<StackPane styleClass="assistant-sparkle-badge">` is inside an `HBox` with `alignment="CENTER_LEFT"`, so it stretches vertically to the full height of the two-line VBox (`DeepNote AI` + `● Connected`), becoming a tall pill instead of a `28x28px` square!
   - **Fix:** Set `minWidth="28"`, `prefWidth="28"`, `maxWidth="28"`, `minHeight="28"`, `prefHeight="28"`, `maxHeight="28"` on `.assistant-sparkle-badge` in both FXML and CSS (`-fx-min-width: 28px; -fx-max-width: 28px; -fx-min-height: 28px; -fx-max-height: 28px; -fx-background-radius: 8px;`).
2. **Fix User Speech Bubble Width (Fit to Text!):**
   - **Why it happens:** In `AssistantPanelController.appendUserBubble()`, `bubble.setMaxWidth(265);` combined with `VBox` fill width stretches `"Summarize this note"` into a wide 265px blue bar.
   - **Fix:** Wrap the user `Label` (or `HBox` with `Region` spacer on the left) so the blue `.chat-user-bubble` **hugs the exact width of the user's message** (`Region.USE_PREF_SIZE` up to `maxWidth="260"` for multi-line wrapping). Short messages like `"Summarize this note"` must only be as wide as their text + padding (`10px 14px`).
3. **Render Markdown in AI Responses (`**bold**`, `` `code` ``, headings, bullet lists):**
   - **Current Bug:** Raw `**Switch branch**:` and `` `git switch -c` `` asterisks/backticks are dumped into a plain `Label`.
   - **Fix:** Replace the plain `Label body` in `appendAssistantCard()` with a custom **Markdown-to-JavaFX renderer** (`VBox` of blocks using `TextFlow` for inline formatting):
     - Parse `###` / `##` / `#` headings as bold `#F1F3F6` labels (`14px–15px`).
     - Parse `**bold**` spans into `Text` nodes with `-fx-font-weight: 700; -fx-fill: #F1F3F6;`.
     - Parse `` `inline code` `` spans into styled inline code `Label` or `Text` (`JetBrains Mono` `12px`, `#8AB4FF` text, `#252932` background pill).
     - Parse fenced code blocks (` ``` ... ``` `) into a dark `#0D0F12` rounded box (`#252932` border, `8px` padding, `JetBrains Mono` `12px` `#A6C8FF` text).
     - Parse numbered (`1. `) and bulleted (`- ` / `* `) lists with clean indentation.
     - Keep `copyToClipboard(response)` and `insertTextCallback.accept(response)` using the clean text.

---

## 4. Invisible Scrollbar on Home Page (`home-view.fxml` & `foundation.css`)

**Current Bug:** The scrollbar on the Home page is visible and distracting.  
**Exact Fix:**
- Keep mouse-wheel/trackpad vertical scrolling 100% functional on `homeScrollPane`, but make the scrollbar **completely invisible** on the Home screen:
  - Add styleClass `home-scroll-invisible` to `homeScrollPane`.
  - In `foundation.css`, target `.home-scroll-invisible > .scroll-bar:vertical` and `.home-scroll-invisible > .scroll-bar:horizontal`:
    ```css
    .home-scroll-invisible > .scroll-bar:vertical,
    .home-scroll-invisible > .scroll-bar:horizontal {
        -fx-opacity: 0;
        -fx-pref-width: 0;
        -fx-max-width: 0;
        -fx-pref-height: 0;
        -fx-max-height: 0;
        -fx-padding: 0;
    }
    ```

---

## 5. Custom Modern Dark Dialogs (`FoundationController.java`, `EditorViewController.java`, `foundation.css`)

**Current Bug:** Default JavaFX `Alert` and `TextInputDialog` boxes (Create Note, Rename Note, Delete Confirmation, Unsaved Changes) look clunky, use ugly default OS headers/icons, and feel disconnected from the UI.

**Exact Fix:**
- Create a reusable custom modal dialog helper (or style `StageStyle.TRANSPARENT` / `StageStyle.UNDECORATED` custom JavaFX dialog panes) for **New Note**, **Rename Note**, **Delete Note**, and **Unsaved Changes**:
  - **Surface:** Rounded `#1B1D22` card (`1px solid #30343D`, `14px` radius, `24px` padding, subtle drop shadow) centered over the main `Stage` with a dimmed backdrop overlay (`rgba(0, 0, 0, 0.55)`).
  - **Typography & Inputs:** Crisp `18px` bold `#F1F3F6` title, `13px` `#969DA9` subtitle/description, sleek `#0D0F12` `TextField` (`40px` height, `#30343D` border, `#4C8DFF` focus border, `8px` radius) for New/Rename dialogs.
  - **Buttons:** Right-aligned `HBox` (`10px` spacing) with secondary `#22252B` `Cancel` button and primary `#4C8DFF` action button (`Create`, `Rename`, `Save`) or `#F87171` destructive button (`Delete`, `Discard`). Support `Enter` to confirm and `Esc` to cancel.