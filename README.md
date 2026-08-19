# Terminal Launcher

Android reduced to a prompt.

Terminal Launcher is a minimal, open-source Android Home application written in Kotlin and Jetpack Compose. It presents installed applications as text and launches only Android activities discovered through `PackageManager`. It is a terminal metaphor, not a real shell.

## Current status

The current implementation provides:

- Android Home registration;
- a text-only list of launchable applications;
- tap-to-launch behavior;
- a full-screen, edge-to-edge Compose UI;
- system monospace typography;
- DataStore-backed launcher preferences;
- persisted shell, terminal theme, clock, username, hostname, and pinned-package settings;
- a Home screen that reacts to preference changes and shows only installed pinned apps;
- DOS and Unix shell profiles for prompts, paths, application names, lists, and command aliases;
- reactive shell-specific application naming without DOS/Unix branches in Compose;
- an optional live clock and shell-formatted static prompt on Home;
- independent System, Green, Amber, and Monochrome terminal color themes;
- a minimal settings screen for shell, theme, clock, username, and hostname preferences;
- a focusable prompt with keyboard input, Enter submission, and a focus-aware blinking cursor;
- command-line builds without Android Studio;
- JVM tests and a mandatory 100% coverage gate for application logic.

Live application search and command execution are tracked in the [public roadmap](https://github.com/Gybra/Terminal-Launcher/issues) and are intentionally deferred to their focused issues.

## Safety model

The project never executes real shell commands. `Runtime.exec`, `ProcessBuilder`, `/bin/sh`, `su`, shell passthrough, arbitrary Intent URIs, and unregistered commands are outside the architecture. Every future command must map to a controlled Android API or an explicit safe Intent.

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

Then choose Terminal Launcher when Android asks for the default Home application, or change the Home app in Android system settings.

## Architecture

The v0.1 codebase is a single Gradle module with explicit boundaries:

```text
Compose UI -> HomeViewModel -> AppRepository --------> PackageManager
                          \-> PreferencesRepository -> DataStore
                          \-> ShellProfiles ---------> DOS / Unix formatting
                          \-> TerminalTheme ---------> shell-independent colors
                          \-> LauncherClock ---------> local system time
                         tap -> AppLauncher ----------> explicit package launch Intent
```

- `launcher`: installed-application model, repository boundary, PackageManager adapter, and app launcher;
- `preferences`: immutable launcher settings, repository boundary, and DataStore adapter;
- `shell`: shell context, locations, profile selection, and all DOS/Unix presentation rules;
- `theme`: shell-independent terminal theme and color definitions;
- `ui/home`: immutable UI state, screen-level ViewModel, and stateless Compose rendering;
- `ui/settings`: immutable settings state, preference coordination, and stateless controls;
- `MainActivity`: Android composition root only.

The repository and launcher own Android integration. Compose receives immutable state and emits user events. Later shell and command behavior must remain independent from Compose.

## Coverage policy

Kover enforces 100% line coverage for testable application logic. Android lifecycle glue, Compose compiler-generated code, and generated Android classes are explicitly excluded because host-side unit coverage is not a meaningful measure for those surfaces. Exclusions must remain narrow, documented, and reviewer-approved. UI behavior should use Compose or device tests when introduced.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) and [AGENTS.md](AGENTS.md) before making changes. Issues, branches, commits, pull requests, code, and documentation are written in English.

## License

Licensed under the [Apache License 2.0](LICENSE).
