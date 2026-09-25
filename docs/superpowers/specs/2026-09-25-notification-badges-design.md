# Per-app notification badges — design

## Goal

Show a current count of actionable notifications next to an application's name in Terminal Launcher without periodic background work. The count reflects notifications currently present in Android's notification center, not an unread history. No notification contents or counts are persisted.

## Home experience

- Show a filled badge immediately after the app name in pinned applications and app search results only when its count is positive. Show `99+` above 99. The app name and badge remain one tappable row; tapping opens the app, not a notification. Expose the count to accessibility services as part of the row.
- The badge never affects search ranking. Do not add badges to pinned shortcuts, command output, or historical results.
- Count each active dismissible notification once per application package. Exclude persistent/non-dismissible notifications and group summary notifications when their children are present, so a group does not inflate the number. When there are no eligible notifications, hide the badge.

## Permission and failure states

- Notification access is opt-in through Android settings. The launcher does not show permission prompts on Home. Settings show whether access is enabled and provide an explicit action to open Android's notification-access settings when it is not.
- When access is unavailable, revoked, or the notification listener is disconnected, hide counts rather than retain stale badges. On reconnection, rebuild counts from the currently active notifications before displaying them again. No placeholder zero badges.

## Appearance

- The badge uses theme-derived background and text colors by default. Settings offer independent optional overrides for each color and a way to restore each to its theme default. Keep readable contrast and make the count legible in every supported theme, including while a row is pressed. The badge itself does not introduce a separate tap target.

## Data flow and energy budget

- Use Android's notification-listener events for additions, updates and removals; take a snapshot on connection. Do not poll, schedule timers, repeatedly scan in the background, or retain notification content. Keep only the minimum in-memory state needed to derive package counts and avoid publishing unchanged counts to Home.
- Android notification access and eligibility decisions belong outside composables. Home's ViewModel combines current counts with existing application state; Compose only renders the result. Persist only the user's optional color choices using the existing preferences infrastructure.
- Event processing must remain cheap even with many installed apps and should not wake the launcher for visual updates when no count changes. Absolute zero battery cost is not guaranteed; evaluate the listener's actual impact on a device before calling it battery-neutral.

## Verification

- Unit-test notification eligibility, group handling, per-package aggregation, updates/removals and unchanged-event behavior; exercise Android listener behavior with framework tests where necessary.
- Check badge rendering in both Home locations, accessibility text, overflow, settings state and color defaults/overrides; verify revoked/disconnected access hides stale counts.
- Run the repository's required Gradle gate and measure idle battery behavior against a build without the listener on a device before claiming no noticeable drain.

## Out of scope

No notification content display, unread tracking, notification opening/dismissal from the launcher, per-shortcut badge, search reranking, polling, history of counts, or separate badge color picker dependency.
