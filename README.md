# Terminal Launcher

Android reduced to a prompt.

Terminal Launcher is a minimal, open-source Android Home application written in Kotlin and Jetpack Compose. It presents installed applications as text and launches only Android activities discovered through `PackageManager`. It is a terminal metaphor, not a real shell.

## Features

Terminal Launcher v0.1 is a complete, self-contained Home application.

**Home and appearance**

- registers as an Android Home application and renders full screen, edge to edge, in system monospace type;
- persists every preference in DataStore: shell, terminal theme, clock, username, hostname, and pinned applications;
- offers independent System, Green, Amber, and Monochrome terminal themes and an optional live clock;
- offers a minimal settings screen for shell, theme, clock, username, and hostname.

**Shell metaphor**

- DOS and Unix shell profiles own prompts, paths, application names, lists, help, command aliases, and message style, so no DOS or Unix branch exists in Compose;
- shows a focusable prompt with keyboard input, Enter submission, and a focus-aware blinking cursor;
- keeps an in-memory terminal history of the twenty most recent submitted lines with their output.

**Search and commands**

- searches installed applications while you type, ranking exact, then prefix, then substring label matches, at most five at a time;
- launches on Enter only when the query is unambiguous, and keeps every other match listed;
- runs explicitly registered commands only, resolving each shell alias to a stable command identifier;
- refreshes the application list when packages are added, changed, or removed, and drops an uninstalled package from the pinned ones while a package being updated keeps its pin.

**Quality**

- builds, tests, lints, and packages from the command line without Android Studio;
- enforces 100% line coverage for testable application logic on every pull request.

Persistent application aliases, fuzzy search with usage-aware ranking, and prompt customization are v0.2; controlled Android utility commands are v0.3. Both are tracked in the [public roadmap](https://github.com/Gybra/Terminal-Launcher/issues) and are intentionally outside v0.1.

## Using the launcher

Type at the prompt. Matching applications appear under it as you type; tap one to launch it, or press Enter when a single application matches. Ambiguous input stays on screen with its matches rather than launching something arbitrary.

Anything that is not a registered command is treated as a search. Command names are matched without case, and each shell accepts its own aliases:

| What it does | Unix | DOS |
| --- | --- | --- |
| List installed applications | `ls`, `dir` | `DIR` |
| Describe the available commands | `help` | `HELP` |
| Pin an application to Home | `pin <application>` | `PIN <APPLICATION>` |
| Remove an application from Home | `unpin <application>` | `UNPIN <APPLICATION>` |
| Clear the terminal history | `clear`, `cls` | `CLS` |
| Open the settings screen | `settings` | `SETTINGS` |

`pin` and `unpin` accept an exact or unique application name; an ambiguous name is answered with the matching applications so a longer name can be given. Output, confirmations, and errors are written in the style of the selected shell: lowercase names on Unix, decorative `.EXE` names and uppercase messages on DOS.

The terminal history lives in memory only and starts empty after every process restart, by design.

## Safety model

The project never executes real shell commands. `Runtime.exec`, `ProcessBuilder`, `/bin/sh`, `su`, shell passthrough, arbitrary Intent URIs, and unregistered commands are outside the architecture. Prompt input is tokenized and matched against explicitly registered commands only, and anything else falls back to application search. Every future command must map to a controlled Android API or an explicit safe Intent.

## Build without Android Studio

Prerequisites:

- JDK 17;
- Android SDK command-line tools;
- Android SDK Platform 37.0;
- Android SDK Build Tools 36.0.0.

On macOS with Homebrew:

```sh
brew install openjdk@17 android-commandlinetools
export JAVA_HOME="$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="$(brew --prefix)/share/android-commandlinetools"
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;36.0.0"
```

Point Gradle to the SDK using an ignored local file:

```sh
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
```

Build and verify the project:

```sh
./gradlew testDebugUnitTest koverVerifyDebug lintDebug assembleDebug
```

The installable APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a USB-connected device with platform tools:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Select Terminal Launcher as Home

Press the Home button once and choose Terminal Launcher when Android asks which Home application to use, or open the Android system settings and change the default Home application there. Android also offers to make the choice permanent; picking "Always" keeps Terminal Launcher as Home until it is changed back in the system settings.

## Architecture

The v0.1 codebase is a single Gradle module with explicit boundaries:

```text
Compose UI -> HomeViewModel -> AppRepository --------> PackageManager
                          \-> PreferencesRepository -> DataStore
                          \-> PackageMonitor --------> package broadcasts
                          \-> AppSearchEngine -------> ranked application matches
                          \-> CommandExecutor -------> explicitly registered commands
                          \-> ShellProfiles ---------> DOS / Unix formatting
                          \-> TerminalTheme ---------> shell-independent colors
                          \-> LauncherClock ---------> local system time
          tap or Enter -> AppLauncher ----------> explicit package launch Intent
```

- `launcher`: installed-application model, repository boundary, PackageManager adapter, package-change monitoring, and app launcher;
- `preferences`: immutable launcher settings, repository boundary, and DataStore adapter;
- `command`: stable command identifiers, prompt tokenizing, explicit registration, execution, and the registered help, application-list, history-clearing, pinning, and settings commands;
- `search`: Compose-independent label matching and deterministic result ranking;
- `shell`: shell context, locations, profile selection, and all DOS/Unix presentation rules;
- `theme`: shell-independent terminal theme and color definitions;
- `ui`: terminal typography, theme provider, and the destination the launcher renders;
- `ui/home`: immutable UI state, screen-level ViewModel, terminal history, and stateless Compose rendering;
- `ui/settings`: immutable settings state, preference coordination, and stateless controls;
- `MainActivity`: Android composition root only.

The repository, the package monitor, and the launcher own Android integration. Compose receives immutable state and emits user events, and what a submitted line asks the launcher to do travels back as a typed action. The command engine and the search engine never depend on Compose, so a new command registers through the command registry without touching Home.

## Coverage policy

Kover enforces 100% line coverage for testable application logic. Android lifecycle glue, Compose compiler-generated code, and generated Android classes are explicitly excluded because host-side unit coverage is not a meaningful measure for those surfaces. Exclusions must remain narrow, documented, and reviewer-approved. UI behavior should use Compose or device tests when introduced.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) and [AGENTS.md](AGENTS.md) before making changes. Issues, branches, commits, pull requests, code, and documentation are written in English.

## License

Licensed under the [Apache License 2.0](LICENSE).
