# Settings accordion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the flat settings list with a live Home preview and five explained, collapsible sections.

**Architecture:** `SettingsScreen` keeps a fixed header (back, storage error, preview) above a `LazyColumn` of section blocks; the open section is UI state held with `rememberSaveable`. Labels, descriptions and summary words live in `SettingsEntry`; which prompt rows exist comes from `ShellProfile.promptOptions`. Home's badge and status line move to shared `ui` files so the preview draws exactly what Home draws.

**Tech Stack:** Kotlin, Jetpack Compose Foundation (no Material), Robolectric Compose tests, Kover.

**Spec:** `docs/superpowers/specs/2026-11-12-settings-accordion-design.md`

## Global Constraints

- Compose must not branch on DOS versus Unix.
- Every label, key, test tag and description is named once (enum or constant) and read by screen and tests.
- Composables ≤ 60 non-blank body lines, private functions ≤ 50, public ≤ 40; ≤ 5 branches.
- Tappable elements ≥ 48dp; `FontFamily.Monospace`; Foundation only.
- Kover 100% line coverage; Kotlin warnings are errors.
- Gate: `./gradlew testDebugUnitTest koverVerifyDebug lintDebug assembleDebug`.

## Review Focus

- Switching shell while the prompt section is open: the rows must swap (Unix ↔ DOS) without closing the section. Test in Task 4.
- Notification access revoked while the notifications section is open: badge rows disappear, the grant action appears. Test in Task 4.
- Username and hostname cleared: the preview and the prompt summary drop the separator (`~$`). Test in Task 3.
- Rotation: the open section survives (`rememberSaveable` with an enum). Covered by `StateRestorationTester` in Task 4.
- DOS writes descriptions and summaries uppercase. Test in Task 4.

---

### Task 1: Prompt options owned by the shell

**Files:**
- Create: `app/src/main/java/com/gybra/terminallauncher/shell/PromptOption.kt`
- Modify: `shell/ShellProfile.kt`, `shell/unix/UnixShellProfile.kt`, `shell/dos/DosShellProfile.kt`
- Test: existing Unix/DOS profile tests

**Interfaces:**
- Produces: `enum class PromptOption { USERNAME, HOSTNAME, SYMBOL, DRIVE, PATH }`; `ShellProfile.promptOptions: List<PromptOption>`. Unix: `USERNAME, HOSTNAME, SYMBOL, PATH`. DOS: `DRIVE, PATH`.

- [ ] Write failing tests asserting both lists, run, implement, run, commit `feat: let each shell name the prompt options it honours`.

### Task 2: Share Home's badge and status line

**Files:**
- Create: `ui/NotificationBadge.kt` (`internal fun NotificationBadge(count: Int, background: String?, text: String?, pressed: Boolean, size: BadgeSize)`), `ui/StatusLine.kt` (`internal fun StatusLine(clock: String?, battery: String?)`)
- Modify: `ui/home/HomeScreen.kt` (remove the private copies and `badgeColor`)

- [ ] Move without behavior change; Home tests stay green; commit `refactor: share the notification badge and status line`.

### Task 3: Sections, descriptions and summaries

**Files:**
- Create: `ui/settings/SettingsSection.kt` — `enum class SettingsSection(entry: SettingsEntry)` with `APPEARANCE, HOME_SCREEN, GESTURES, PROMPT, NOTIFICATIONS`, and `fun SettingsUiState.summary(section: SettingsSection): String`
- Modify: `SettingsEntry.kt` — add `description` and `summary` (defaults to label); add `HOME_SCREEN`, `GESTURES`, `PROMPT`, `NOTIFICATIONS`, `ALL_OFF("all off")`, `OFF("off")`; remove `UNIX`, `DOS`; `NOTIFICATION_ENABLED` label becomes `granted`; `SHOW_CLOCK` summary `clock`, `SHOW_BATTERY` `battery`, `IMMERSIVE_MODE` `immersive`, `DOUBLE_TAP_TO_LOCK` `double tap locks`, `BADGE_SIZE` `size`.
- Modify: `SettingsUiState.kt` — `fun shellContext(): ShellContext` (Home location).
- Test: `ui/settings/SettingsSectionTest.kt`

Summaries (before `ShellProfile.formatMessage`): `unix · solarized`; enabled home toggles joined by ` · ` or `all off`; `double tap locks` or `off`; `shellProfile.prompt(shellContext())`; `granted · size 2` or `off`.

- [ ] Tests for each summary incl. both-empty identity (`~$`) and DOS uppercase; implement; commit `feat: describe and summarise every settings section`.

### Task 4: Accordion screen with preview

**Files:**
- Modify: `ui/settings/SettingsScreen.kt` (screen + accordion), Create: `ui/settings/SettingsPreview.kt`, `ui/settings/SettingsRows.kt` (row composables)
- Modify: `ui/TestTag.kt` add `SETTINGS_PREVIEW`
- Test: rewrite `SettingsScreenTest.kt`; update `LauncherAppTest.kt` (APPEARANCE lookups) and `ReadmeScreenshots.kt` (open appearance for the screenshot)

Rows: choice chips (`FlowRow`, tag `entry.optionKey(option)`, `Role.RadioButton`), toggle `[x]`/`[ ]`, text field with `>` and underline (content description = label), badge colour with collapsible palette, notification access status + grant action. Preview sample: clock `12:00`, `formatBattery(BatteryStatus(50, false))`, app `InstalledApp("sample", "messages")`, badge count 3. Headers: `Role.Button`, state description `expanded`/`collapsed`.

- [ ] Tests: sections collapsed at start; opening shows rows + descriptions; opening another closes the first; tapping open header closes it; prompt rows follow shell while open; badge rows hidden without access; preview follows prompt/status/badge; DOS uppercase; every action still forwarded; open section restored after recreation.
- [ ] Implement; run settings + launcher tests; commit `feat: open settings as explained sections with a live preview`.

### Task 5: Change highlight

**Files:** `ui/settings/SettingsPreview.kt`

`ChangeHighlight(value: Any?, content)` draws a foreground border fading 1→0 alpha over 1000 ms when `value` changes after first composition. Keys: prompt line → prompt string; status line → (clock, battery); badge → (size, background, text); whole preview → (theme, status line visible, notification access).

- [ ] Test that the border is absent at start and present right after a change (paused clock); implement; commit `feat: mark what a setting changed in the preview`.

### Task 6: Docs and gate

- [ ] Update the Settings part of `docs/design.md`; regenerate `docs/screenshots/settings.png` via `ReadmeScreenshots`; commit `docs: describe the explained settings`.
- [ ] Functions-shrinking pass, branch-standards audit, required Opus review, Gradle gate, PR.
