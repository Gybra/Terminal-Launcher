# Notification Badges Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Display event-driven, per-package counts of active dismissible notifications in pinned apps and search, with opt-in access and customizable badge colors.

**Architecture:** Android's notification listener owns notification events and a process-local count stream; HomeViewModel consumes immutable counts and Compose renders them. Existing DataStore preferences own optional badge color overrides; Settings exposes access status and the system settings action. No polling, persistent notification data, or notification text.

**Tech Stack:** Android NotificationListenerService (API 28+), Kotlin Flow, Compose Foundation, AndroidX DataStore, Robolectric/JVM and Compose tests.

**Spec:** `docs/superpowers/specs/2026-09-25-notification-badges-design.md`

## Global Constraints

- Minimum SDK API 28; one Gradle application module; no new dependencies without demonstrated need.
- Only currently active, dismissible notifications; do not count group summary alongside its children; show `99+` visually above 99.
- No polling, timers, persisted notification content or counts; missing/revoked/disconnected access must hide stale badges.
- Keep Android APIs outside composables; preserve existing shell and Home rendering boundaries.
- Required local gate: `./gradlew testDebugUnitTest koverVerifyDebug lintDebug assembleDebug`; Kover 100% for testable application logic.

## Review Focus

- Notification update changes eligibility or package: old contribution disappears, new contribution appears.
- Group child removed while summary stays: avoid dropping an eligible summary incorrectly.
- Listener reconnects after missed events: snapshot replaces stale counts rather than adding to them.
- Permission revoked while Home is visible: old badges disappear even without a callback.
- Color override with malformed input or theme change: reject invalid color and keep theme default available.

## File map

- New `launcher/NotificationCounts.kt`: small, in-memory eligibility/aggregation state and immutable count flow; no Compose.
- New `launcher/TerminalNotificationListenerService.kt`: Android callback adapter, snapshot/reconnect/disconnect; manifest registration.
- Modify `ui/home/HomeViewModel.kt`, `HomeUiState.kt`, `HomeScreen.kt`: wire counts, render shared AppRow badge for search and pinned apps only.
- Modify existing preferences model/repository and Settings state/actions/ViewModel/Screen/Entry: optional colors and notification-access guidance.
- Modify Activity composition root to connect Android-backed state, and theme colors only where needed to derive accessible badge contrast.
- Add focused tests beside existing launcher, home, settings and preferences tests.

---

### Task 1: Count active eligible notifications without polling

**Files:** Create `app/src/main/java/com/gybra/terminallauncher/launcher/NotificationCounts.kt`, `TerminalNotificationListenerService.kt`; modify `app/src/main/AndroidManifest.xml`; create `app/src/test/java/com/gybra/terminallauncher/launcher/NotificationCountsTest.kt` and an Android adapter test if callback behavior cannot be covered via the pure core.

**Interfaces:** Produce `StateFlow<Map<String, Int>>` counts (empty when disconnected); snapshot replacement and keyed post/remove events using notification key, package, clearable flag and group-summary/group key. Keep all framework `StatusBarNotification` and `Notification` conversions in the service. Never collect or retain title/body.

- [ ] Write tests first for empty/disconnected, clearable vs ongoing, multiple packages, updates, removals, group summary plus child, summary after last child, duplicate event, snapshot replacement and disconnect.
- [ ] Run focused JVM tests and observe expected red failure.
- [ ] Implement the smallest in-memory state and publish a new immutable map only if a count actually changes. Use service callbacks and `activeNotifications` once on connection; clear on disconnect. Register service with `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` and `android.service.notification.NotificationListenerService` intent action; do not introduce a background job.
- [ ] Run focused JVM/adapter tests green; commit `feat: count active dismissible notifications`.

### Task 2: Show counts only on current Home app rows

**Files:** Modify `app/src/main/java/com/gybra/terminallauncher/ui/home/HomeViewModel.kt`, `HomeUiState.kt`, `HomeScreen.kt`; wire in the Activity composition root; modify `app/src/test/java/com/gybra/terminallauncher/ui/home/HomeViewModelTest.kt`, `HomeScreenTest.kt`.

**Interfaces:** Consume Task 1's count Flow; add `notificationCounts: Map<String, Int>` to `HomeUiState`. `AppRow` takes a nullable positive count for current pinned/search rows only; history/shortcuts pass none.

- [ ] Write ViewModel and Compose tests first: app count maps by package, zero hides badge, `100 -> 99+`, search/pinned both show it, historical output and shortcuts do not, row still launches app, accessibility includes count, no reordering.
- [ ] Run focused tests and observe red failure.
- [ ] Collect counts with the ViewModel's owning scope, combine with existing Home state; attach one badge beside the name inside the existing row. Preserve 48dp row height, pressed colors and long-click; cap visual count without capping accessibility count.
- [ ] Run focused tests green; commit `feat: render notification counts on app rows`.

### Task 3: Settings access and optional colors

**Files:** Modify `app/src/main/java/com/gybra/terminallauncher/preferences/LauncherPreferences.kt`, `PreferencesRepository.kt`, `DataStorePreferencesRepository.kt`; `ui/settings/SettingsActions.kt`, `SettingsEntry.kt`, `SettingsUiState.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt`; Activity composition root; adjust `ui/theme/TerminalThemeProvider.kt` only if badge defaults cannot be derived from existing terminal colors. Modify corresponding tests in `app/src/test/.../preferences`, `ui/settings`, `ui/theme`.

**Interfaces:** Persist two nullable validated ARGB hex color strings, where null means theme default. Expose notification access as current Android status, and open Android notification-listener access settings through the existing Android boundary. The UI must not ask for access on Home.

- [ ] Write failing tests for default theme colors, valid/invalid hex overrides, resetting either override, persistence, system-theme changes, denied/granted/revoked permission and failed settings intent resolution.
- [ ] Run focused tests red.
- [ ] Implement the smallest settings controls: text color entries with visible theme-default/reset option and a badge preview; access status and explicit system-settings action. Check actual enabled-listener access when Settings resumes; do not equate ordinary notification POST permission with listener access. Resolve/fail safely if Settings activity is unavailable. Use a readable default color pair from existing theme colors, validating user choices before saving.
- [ ] Run focused tests green; commit `feat: configure notification badge access and colors`.

### Task 4: Integration, battery and delivery gate

**Files:** Existing tests and spec only if actual behavior requires correction; no extra production layers.

- [ ] Test listener connect/post/update/remove/disconnect/reconnect -> Home count -> both visible rows, and theme/override states end to end. Check launcher restart and revoked permission do not show stale counts.
- [ ] Review full diff against spec and `AGENTS.md`; perform diff-scoped function-shrinking and branch-standards audits; confirm no timers, periodic workers, or notification content retention with repository search.
- [ ] Request the mandatory interactive Herdr Grok 4.6 high-effort review on the bounded diff, read findings/verdict, fix valid findings and re-review as needed.
- [ ] Run `./gradlew testDebugUnitTest koverVerifyDebug lintDebug assembleDebug` after review; inspect failures before any fix. For battery claims, measure idle device behavior against a build without the listener; if no device measurement is available, report that the no-drain claim remains unverified.
- [ ] Only after gates pass, create a focused PR with `Closes #118`, document review verdict and any measurement limitation. Await green `build-and-test` and owner merge.
