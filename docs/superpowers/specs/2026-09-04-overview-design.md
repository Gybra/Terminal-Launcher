---
date: 2026-09-04
topic: overview
---

# Open system Overview

## Problem Frame

Home is immersive, so Android Recents is awkward to reach. The launcher cannot list or control recents (no public API for the Overview task list, swipe-away, Honor lock, mini-window, or Clear all). The honest product is to open the system Overview the user already has.

## Requirements

**Gestures**

- R1. The gesture zone is the **lower half** of Home's current bounds (keyboard inset included). Rows in that half do not consume the swipe.
- R2. In that zone: swipe down on the left opens notifications; swipe down on the right opens quick settings; swipe up opens system Overview.
- R3. The **upper half** only scrolls. Shade no longer starts from the empty top.
- R4. Tap and long-press on rows stay as they are. A swipe counts only past touch slop, and only when it is more vertical than horizontal.
- R5. Gesture does nothing while the accessibility service is off (same as lock and shade today).

**Command**

- R6. A registered command `overview` (Unix) / `OVERVIEW` (DOS), group device, opens the same system Overview as the gesture.
- R7. Success leaves Home the way starting an app does: keyboard dismissed, submitted line recorded, no extra output.
- R8. If the service is off, or Android refuses the action, the command writes that Overview is unavailable and stays on Home.

**Privilege**

- R9. Uses the existing accessibility service only: one more `GLOBAL_ACTION_RECENTS`. Still no window content, no events, no gesture dispatch, no new permission, no new settings toggle.
- R10. Safety, design, architecture, the accessibility service description, `help`, and the README command list all name this fourth action and the inverted shade zone.

## Success Criteria

- From the pins or the search results above the prompt, swipe up opens Overview; swipe down opens shade / quick settings.
- A long pin list or a long search still scrolls from the upper half.
- `overview` appears in `help` and opens Overview when the service is on.
- With the service off, the gesture is inert and the command says so; Home does not crash.

## Scope Boundaries

- Not a recents list, not re-open from a launcher-owned task list.
- Not close, lock, mini-window, split, or `clear overview`.
- Not OEM-specific Overview actions.
- Not Usage Access, `killBackgroundProcesses`, or a broader accessibility service.
- Not a new settings row; the service stays the one “Double tap to lock” already enables.

## Key Decisions

- **Open system Overview, do not emulate it.** Third-party launchers cannot read or mutate Recents.
- **Lower-half zone (A).** Pins and search sit above the prompt. Gestures live there; scroll lives in the upper half. This inverts today’s shade zone on purpose.
- **Command plus gesture, same action.** Typing is discoverable; the swipe is the daily path.
- **Failure is quiet for gestures, spoken for the command.** Matches lock/shade vs `torch`.
- **Reuse shade and lock.** Invert the existing Home swipe detector; register one device command. No second gesture system, no Compose branch on shell.

## Dependencies / Assumptions

- API 28+ devices expose `GLOBAL_ACTION_RECENTS`.
- The accessibility service is already the gate for lock and shade; Overview reuses it.

## Outstanding Questions

### Resolve Before Planning

(none)

### Deferred to Planning

- Exact unavailable copy, as the shell already formats command messages.
- Whether README screenshots move, given no new on-screen chrome.

## Next Steps

-> Implementation plan after this spec is accepted.
