# Contributing to Terminal Launcher

Thanks for contributing. The project is intentionally small, safe, and understandable without proprietary tooling or AI assistance.

## Before coding

1. Pick or open one English-language issue with clear acceptance criteria.
2. Wait until prerequisite issues have been merged.
3. Start from the latest `main`.
4. Create one focused branch named `<type>/<issue>-<short-slug>`, for example `feat/7-live-app-search`.

Do not push directly to `main`. Each issue is delivered through one pull request, and the PR body must contain `Closes #<issue>` so GitHub closes the issue only after merge.

## Development workflow

Use conventional commits such as:

```text
feat(search): add exact application matching
test(search): cover ambiguous matches
docs(contributing): explain CLI setup
```

Before opening a pull request, run:

```sh
./gradlew testDebugUnitTest koverVerifyDebug lintDebug assembleDebug
```

The required `build-and-test` GitHub check must be green. Only the repository owner merges pull requests. Start the next issue from `main` only after the previous pull request has been reviewed and merged.

## Change expectations

- Keep pull requests focused on one issue.
- Add tests before or alongside behavior changes.
- Preserve 100% coverage for testable application logic.
- Keep Android framework access behind explicit boundaries.
- Keep composables stateless when practical: state down, events up.
- Do not add dependencies, abstractions, or modules without demonstrated need.
- Do not add real shell execution, unsafe arbitrary intents, networking, analytics, accounts, or other out-of-scope features.
- Update user and contributor documentation when behavior or workflows change.
- Regenerate the README pictures when the interface changes. `ReadmeScreenshots` renders Home and the settings through Robolectric and writes `docs/screenshots/`, so running the test suite updates them and `git status` reports what moved.

## Review

Reviewers check correctness, Android lifecycle safety, architecture boundaries, accessibility, tests, coverage, function density, duplication, and alignment with [AGENTS.md](AGENTS.md). Resolve every review conversation before merge.
