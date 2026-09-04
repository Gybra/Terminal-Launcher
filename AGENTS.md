# Repository guidelines

These rules apply to humans and coding agents. Optimize for a small codebase that a new contributor can understand, build, test, and modify without AI assistance.

## Language and communication

- Write source code, comments, KDoc, commits, branches, issues, pull requests, and repository documentation in English.
- Use plain, specific names. Avoid generic suffixes such as `Util`, `Helper`, `Manager`, or `Processor` unless the type genuinely owns that role.
- Document non-obvious constraints and decisions. Do not narrate code that is already self-explanatory.

## GitHub workflow

- Never push implementation work directly to `main`.
- Work on exactly one issue at a time.
- Refresh `main`, then create `<type>/<issue>-<short-slug>` where type is `feat`, `fix`, `refactor`, `test`, `docs`, or `chore`.
- Use Conventional Commits and keep commits logical and independently green.
- A pull request must contain `Closes #<issue>` and stay focused on that issue.
- Do not open a pull request until Required final review steps 1–5 are complete. User urgency, a request to ship, and a green CI check do not skip them.
- Do not start a dependent issue until the previous pull request is reviewed, CI is green, and the repository owner merges it.
- Never merge with a failing, missing, skipped, or stale required check. Resolve every review conversation first.

## Kotlin style

- Follow the official Kotlin coding conventions and the repository `.editorconfig`; use four spaces and no wildcard imports.
- Prefer `val`, immutable collections, immutable UI state, pure functions, and expression bodies when they remain readable.
- Use named arguments when multiple adjacent parameters share a type or when a Boolean argument would be ambiguous.
- Keep nullability explicit. Prefer guard clauses and sealed/data types over sentinel values and deeply nested conditionals.
- Name a string that identifies or labels something. Compose list keys, test tags, and user-facing labels come from an enum or a named constant, so a screen and its test read the same value instead of repeating a literal, and renaming one is a compile-time change. A literal is left in place only where it is written once and read nowhere else.
- Public boundary declarations require explicit visibility and explicit non-`Unit` return types. Overrides may rely on inherited visibility. Add KDoc only for public contracts or non-obvious behavior.
- Keep one principal public type per file. Closely related private declarations may share its file.
- Order class members so the public workflow reads top-down; place private implementation details near the code that uses them.
- Do not use `!!` outside tests. Avoid global mutable state, platform types escaping Android adapters, broad exception swallowing, and coroutine launches without an owning scope.
- A suspend API must be main-safe. The type performing blocking work owns dispatcher selection.

## Function and component size

- A public or override function should stay at or below 40 non-blank body lines; a private function at or below 50.
- A composable function should stay at or below 60 non-blank body lines. A screen should orchestrate smaller composables rather than own unrelated regions.
- Any function with more than five branches, nesting deeper than three levels, or more than two distinct responsibilities must be split by concern.
- Extract helpers only when they can be named by intent. Do not create `partOne`/`partTwo`, pass-through wrappers, or single-use helpers requiring more than four parameters.
- Refactors must preserve behavior. Keep bug fixes and mechanical function shrinking in separate commits.

Before each pull request, perform a diff-scoped functions-shrinking pass. Re-enumerate changed functions after edits and stop only when no threshold violations remain and verification is green.

## Architecture

- Keep a single Gradle application module through v0.1.
- UI is not shell logic. Compose must not branch on DOS versus Unix; `ShellProfile` owns prompt, path, command alias, application-name, help, and list formatting.
- Compose is a renderer: immutable state flows down and typed events flow up. Screen-level ViewModels coordinate repositories and domain services.
- Android framework APIs belong at explicit boundaries such as repositories, launchers, receivers, and the Activity composition root.
- Command Engine and Search Engine must not depend on Compose. New commands register through the command registry without changes to `HomeScreen`, `HomeViewModel`, or the parser.
- Prefer constructor injection and small manual factories. Do not add Hilt, Koin, or another dependency-injection framework.
- Search for an existing model, component, formatter, repository, or service before creating one. Centralize behavior only when there is proven duplication or a stable domain boundary.
- Avoid speculative abstractions. Every interface must protect a boundary, enable substitution in tests, or have multiple concrete consumers planned in the active issue.

Before each pull request, perform a diff-scoped branch-standards audit: enumerate every changed declaration, compare it with repository rules and relevant siblings, prove duplication with search results, fix violations, and rerun all verifiers until clean.

## Android and Compose

- Minimum SDK is API 28. Target and compile against the latest stable Android SDK supported by the pinned AGP version.
- Keep `MainActivity` a thin composition root. Do not store application data in Activities or composables.
- Use lifecycle-aware Flow collection and structured concurrency. Never keep an Activity `Context` in a ViewModel.
- Prefer Foundation and Compose primitives. Use Material 3 only when it adds necessary behavior or accessibility.
- Use `FontFamily.Monospace`; do not add custom fonts in v0.1.
- Provide stable keys for dynamic Compose lists. Keep tappable content accessible and large enough to operate reliably.
- Package discovery uses MAIN plus LAUNCHER queries and the narrowest package visibility declaration. Never request `QUERY_ALL_PACKAGES` without an approved issue and policy review.

## Testing and coverage

- Behavior-bearing changes default to test-first: add or strengthen a failing test, observe the expected failure, implement the smallest change, then refactor while green.
- Unit-test pure Kotlin and Android adapters on the JVM. Use Robolectric only when Android framework behavior is part of the contract; use Compose/device tests for UI behavior when appropriate.
- Kover must report 100% line coverage for testable application logic. Do not weaken thresholds.
- Coverage exclusions are limited to generated Android code, compiler-generated Compose glue, and thin lifecycle composition roots. Every new exclusion requires written justification in the pull request and owner approval.
- Tests cover happy paths, boundaries, empty input/state, ambiguity, exceptions, and cross-layer integration when applicable.
- Test names describe behavior and expected outcome. Tests must be deterministic and independent of installed local applications.
- Required local gate: `./gradlew testDebugUnitTest koverVerifyDebug lintDebug assembleDebug`.

## Security and scope

- This is a launcher with a terminal metaphor, never a terminal emulator.
- Forbidden: `Runtime.exec`, `ProcessBuilder`, `/bin/sh`, `su`, root commands, shell passthrough, arbitrary command execution, and arbitrary Intent URI execution.
- Every command must be registered explicitly and map to controlled Android functionality.
- Handle missing activities, package changes, permission denial, and unsupported APIs without crashing normal flows.
- Do not add networking, analytics, proprietary crash reporting, backend services, accounts, databases, plugins, AI, macros, or unrelated device controls unless an accepted roadmap issue explicitly authorizes them.
- Never log package lists or user input in production unless an approved privacy review requires it.

## Dependencies and build

- Use Gradle Kotlin DSL and the committed Gradle Wrapper. Android Studio must never be required to build, test, lint, or package the APK.
- Pin stable dependency versions; never use dynamic versions.
- Add a dependency only when the standard library, Android SDK, AndroidX, or existing dependencies cannot reasonably solve the active issue. Explain additions in the pull request.
- Keep the build warning-free. Treat Kotlin compiler warnings as errors.
- Never commit SDK paths, signing keys, credentials, build outputs, IDE metadata, or generated reports.

## Required final review

For every pull request:

1. Inspect the complete diff against `main` and remove unrelated changes.
2. Run the functions-shrinking pass.
3. Run the branch-standards audit for naming, reuse, duplication, boundaries, tests, and documentation.
4. Before the Gradle gate, review the bounded branch diff against this file. That review is not optional and has no substitute: not another model, not `pi --print`, not an in-thread self-review, and not a green CI check. Launch it only as an interactive Pi Coding Agent in a dedicated Herdr pane, model `xai/grok-4.6`, thinking effort `high`:
   - `herdr pane split --current --direction right --cwd "$PWD" --no-focus`
   - `herdr agent start <name> --kind pi --pane <id> -- --model xai/grok-4.6 --thinking high`
   - `herdr agent prompt <name> <review text>`
   Preserve this working directory and leave user focus unchanged. Keep the pane open until that agent writes a findings list and a verdict (`approve` or `request changes`). A hung process, an empty pane, or an exit status without that writeup is not a completed review: stop and report the blocker; do not open the PR. Read the output, verify every finding against the code, and fix every valid finding before delivery. The PR body must state that this review ran and what it verdicted.
5. Run the required local Gradle gate.
6. Confirm the PR links and closes exactly one issue, CI `build-and-test` is green, and only the repository owner performs the merge.
