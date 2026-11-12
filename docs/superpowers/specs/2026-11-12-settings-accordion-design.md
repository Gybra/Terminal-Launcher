# Settings accordion with explanations — design

## Why

The settings screen is one long list where every row has the same weight: section titles are
small arrested text, every radio option takes a full row, unrelated settings share the
`appearance` section, and the DOS and Unix prompt settings are both listed whichever shell is
active. Nothing says what a setting changes. The owner wants a screen with a clear first impression
that explains every setting, without leaving the terminal style.

## Goals

- First impression: a live preview of Home, then five collapsed sections, each summarising its
  current values.
- Every setting carries one line saying what it changes.
- Changing a setting visibly marks the part of the preview it changed.
- Only the prompt settings the active shell honours are listed.

## Non-goals

- No new settings, no change to how a setting is stored or applied.
- No persistence of which section is open across launches.
- No Material components, custom fonts, or expand/collapse animation.

## Anti-goals

- A screen that looks like a stock Android preferences page instead of a terminal.
- Compose branching on DOS versus Unix.

## Approaches considered

Three clickable mockups were compared on 2026-11-12:

- **A — grouped list with explanations.** Inverted section bars, bold label, a description under
  each setting, options laid out horizontally. Clear, but still one long list.
- **B — annotated config file.** `~/.termrc` (`C:\CONFIG.SYS` under DOS) with a `#` / `REM`
  comment above every key. The most terminal-like, but options are hidden until a value is tapped.
- **C — setup menu.** Five sections with summaries; each opens a sub-screen with a help panel.
  Clean first impression, one extra navigation level.

**Chosen: A + C.** C's collapsed sections with summaries, opened in place as an accordion, showing
A's rows with descriptions. C's help panel is dropped because A's descriptions make it redundant.
The owner approved the mockup `settings-mix` and asked for one section open at a time.

## The screen

From top to bottom:

1. `< back`, unchanged, and the storage error line when there is one.
2. **Live preview**, an inert dashed box captioned `preview`, kept on screen while the list
   scrolls. It writes what Home would write with the current settings:
   - the status line, following Home's rules: clock on the left and battery on the right, each only
     when its toggle is on, and no line when both are off. Values are fixed samples so the preview
     is deterministic;
   - the prompt, written by `ShellProfile.prompt` for the Home location;
   - one sample application row written by `ShellProfile.formatAppName`, with a notification badge
     of `3` in the chosen size and colours, shown only while notification access is granted.
3. **Five sections**, all collapsed when the screen opens. A collapsed section is a bordered row:
   `▸ name` on the left, its summary arrested on the right. An open section's header is inverted
   (foreground background, background text), shows `▾`, and its rows follow inside the border.
   Opening a section closes the one already open; tapping an open header closes it. The open
   section survives rotation.

| Section | Rows | Summary |
|---|---|---|
| appearance | shell, theme | `unix · solarized` |
| home screen | show clock, show battery, immersive mode | enabled ones joined by ` · `, or `all off` |
| gestures | double tap to lock | `double tap locks` or `off` |
| prompt | Unix: username, hostname, prompt symbol, show path. DOS: drive, show path | the prompt itself |
| notifications | notification access; with access granted also badge size, badge background, badge text | `granted · size 2` or `off` |

Every label, summary, and description goes through `ShellProfile.formatMessage`, so DOS writes
them uppercase, as today.

## Rows

Each row has a bold label and an arrested description under it:

- **Choice** (shell, theme, prompt symbol, drive, badge size): options laid out horizontally and
  wrapping, each at least 48dp high; the selected one inverted. Theme options carry a small swatch
  of that theme's background and foreground.
- **Toggle**: the whole row toggles; `[x]` (inverted) or `[ ]` on the right.
- **Text** (username, hostname): `>` followed by a field underlined in the arrested colour instead
  of today's filled box.
- **Badge colour**: the current colour and its name (or `default`) on the right; tapping opens the
  16-colour palette and `< theme default` under the row; it stays open until tapped again.
- **Notification access**: shows `granted` or `off`; when off, the existing action opens Android's
  notification-access settings.

Descriptions (English source text, final wording is the builder's):

| Setting | Description |
|---|---|
| shell | how the launcher talks: unix writes lowercase and user@host, dos writes uppercase and a drive |
| theme | colours of the whole launcher |
| show clock | time at the top of home |
| show battery | battery level at the top of home |
| immersive mode | hides the android status and navigation bars |
| double tap to lock | double tap on empty space turns the screen off; opens android accessibility settings |
| username | name before the @ in the prompt |
| hostname | name after the @ in the prompt |
| prompt symbol | character that ends the prompt |
| drive | letter at the start of the dos prompt |
| show path | adds the current folder to the prompt |
| notification access | lets the launcher count notifications per app; opens android settings |
| badge size | size of the counter next to an app |
| badge background / text | counter colours; default follows the theme |

## Change highlight

When a value shown in the preview changes, the part of the preview that shows it gets a foreground
outline that fades out in about a second: the prompt for shell, prompt, and drive settings, the
status line side for clock and battery, the badge for badge settings, the whole preview for the
theme. Nothing highlights on first composition. A part that disappears (a toggle turned off)
highlights the whole preview.

## Constraints

- Compose must not branch on shell type. Which prompt rows are listed comes from `ShellProfile`,
  the same way the prompt itself does.
- Labels, keys, test tags, and descriptions are named once, in `SettingsEntry` or a sibling enum,
  and read by both the screen and the tests.
- The preview reuses Home's notification badge and status line instead of copying them; the
  settings badge preview that exists today is replaced by it.
- Section headers expose button role and expanded/collapsed state to accessibility services;
  every tappable element stays at least 48dp high.
- Repository function and composable size limits apply; `SettingsScreen.kt` is already 448 lines
  and is expected to split by region.

## Left to the builder

- How `ShellProfile` exposes the prompt options it honours.
- Where the shared badge and status line live, and the sample clock and battery values.
- File split of the settings screen and the exact highlight implementation.
- Final description wording within the meaning above.

## Verification

- Robolectric Compose tests: sections start collapsed; opening one shows its rows and
  descriptions; opening another closes the first; the prompt section lists Unix or DOS rows
  following the shell; badge rows are hidden without notification access; the preview follows
  shell, prompt, status, and badge settings; DOS writes the screen uppercase.
- Unit tests for the `ShellProfile` prompt options and the section summaries.
- Regenerate `docs/screenshots/settings.png` with `ReadmeScreenshots`, update the Settings part of
  `docs/design.md`, and run the required Gradle gate.
