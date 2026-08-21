# Terminal Launcher

Android reduced to a prompt.

Terminal Launcher is a minimal, open-source Android Home application written in Kotlin and Jetpack Compose. It presents installed applications as text and launches only Android activities discovered through `PackageManager`. It is a terminal metaphor, not a real shell.

## Features

Terminal Launcher v0.1 is a complete, self-contained Home application.

**Home and appearance**

- registers as an Android Home application and renders edge to edge, in system monospace type, hiding the system bars until the user asks to keep them;
- persists every preference in DataStore: shell, terminal theme, clock, battery, immersive mode, username, hostname, prompt symbol, prompt path visibility, DOS drive letter, pinned applications, pinned shortcuts, and how often and how recently each application is launched;
- offers independent System, Green, Amber, and Monochrome terminal themes, and a status line carrying an optional live clock and the battery level, which is shown by default;
- offers a minimal settings screen for shell, theme, clock, battery, immersive mode, double tap to lock, username, hostname, prompt symbol, prompt path visibility, and DOS drive letter;
- answers the requests applications make to pin one of their shortcuts to Home, such as a browser adding a website, and keeps nothing until the user accepts;
- locks the screen on a double tap, the way the power button does, once the user turns on the accessibility service Android requires for it.

**Shell metaphor**

- DOS and Unix shell profiles own prompts, paths, application names, lists, help, command aliases, message style, and the shape of the prompt cursor, so no DOS or Unix branch exists in Compose;
- customizes the prompt cosmetically: Unix identity and end symbol, DOS drive letter, and path visibility in both shells;
- shows a focusable prompt with keyboard input, Enter submission, and a focus-aware blinking cursor shaped by the selected shell;
- keeps an in-memory terminal history of the twenty most recent submitted lines with their output.

**Search and commands**

- searches installed applications while you type, ranking exact, then prefix, then substring, then fuzzy label matches, at most five at a time;
- orders equally strong matches by a documented score: pinned applications first, then the most launched, then the most recently launched;
- launches on Enter only when the query is unambiguous, and keeps every other match listed;
- runs explicitly registered commands only, resolving each shell alias to a stable command identifier;
- reports the battery, switches the torch, opens the Android, Wi-Fi, Bluetooth, and application details screens, asks Android to uninstall an application, and restarts itself, each through one documented Android API or explicit Intent;
- launches persisted application aliases, which never shadow a registered command;
- refreshes the application list when packages are added, changed, or removed, and drops an uninstalled package from the pinned ones while a package being updated keeps its pin.

**Quality**

- builds, tests, lints, and packages from the command line without Android Studio;
- enforces 100% line coverage for testable application logic on every pull request.

Further work is tracked in the [public roadmap](https://github.com/Gybra/Terminal-Launcher/issues).

## Using the launcher

Type at the prompt. Matching applications appear right above it as you type; tap one to launch it, or press Enter when a single application matches. Ambiguous input stays on screen with its matches rather than launching something arbitrary.

A label matches exactly, by prefix, by substring, or fuzzily when the typed characters appear in it in order, and a literal match always outranks a fuzzy one. Matches of the same strength are ordered by a score worth 50 for a pinned application plus one point per launch up to 20, then by the most recent launch, then by name. Ranking reads only the installed applications and the stored launch history, so the same input always produces the same list.

Anything that is not a registered command is treated as a search. Command names are matched without case, and each shell accepts its own aliases:

| What it does | Unix | DOS |
| --- | --- | --- |
| List installed applications | `ls`, `dir` | `DIR` |
| Describe the available commands and how they are called | `help` | `HELP` |
| Pin an application to Home | `pin <application>` | `PIN <APPLICATION>` |
| Remove an application from Home | `unpin <application>` | `UNPIN <APPLICATION>` |
| List, pin, and remove application shortcuts | `shortcuts <application>` | `SHORTCUTS <APPLICATION>` |
| Clear the terminal history | `clear`, `cls`, `clr` | `CLS` |
| Name an application | `alias <name> <application>` | `ALIAS <NAME> <APPLICATION>` |
| Open the Android details of an application | `info <application>` | `INFO <APPLICATION>` |
| Ask Android to uninstall an application | `uninstall <application>` | `UNINSTALL <APPLICATION>` |
| Report the battery level | `battery` | `BATTERY` |
| Turn the torch on or off | `torch` | `TORCH` |
| Open the Android settings | `android` | `ANDROID` |
| Open the Wi-Fi settings | `wifi` | `WIFI` |
| Open the Bluetooth settings | `bluetooth` | `BLUETOOTH` |
| Restart the launcher | `restart` | `RESTART` |
| Open the settings screen | `settings` | `SETTINGS` |

`help` writes every registered command with its description, and under it the ways it is invoked, so the arguments a command takes are visible before running it. A command writes those same lines back when it is called the wrong way.

`alias` names an application so submitting that name launches it, for example `alias browser firefox`. Aliases persist across restarts, are matched without case, and are replaced by defining the same name again. A registered command is always resolved before an alias, and a name any shell already uses for a command is refused rather than allowed to shadow it, so no alias can ever take over `ls`, `clear`, or any other command.

`pin`, `unpin`, and `alias` accept an exact or unique application name; an ambiguous name is answered with the matching applications so a longer name can be given. Output, confirmations, and errors are written in the style of the selected shell: lowercase names on Unix, decorative `.EXE` names and uppercase messages on DOS.

An application can also ask the launcher to keep one of its own shortcuts, which is what a browser does when it adds a website to Home. The launcher answers with a confirmation naming the shortcut and the application that asked for it, and keeps nothing until it is accepted. An accepted shortcut is listed on Home under the pinned applications, written as `new tab` on Unix and `NEW TAB.LNK` on DOS, and tapping it asks Android to start it. A shortcut is started by the package and the identifier Android published for it, never by an Intent the launcher builds or the asking application supplies. Uninstalling an application removes the shortcuts it published, while updating it keeps them.

The same shortcuts are reachable from the prompt, without waiting for the application to offer them. `shortcuts <application>` lists what the application publishes and starts the listed shortcut that is tapped, `shortcuts pin <application> <shortcut>` keeps one on Home, and `shortcuts unpin <application> <shortcut>` removes a pinned one, whichever way it was pinned. The application is named by the first word and the shortcut by the rest of the line, a shortcut name matching more than one is answered with the candidates, and Android answers the question only while Terminal Launcher is the Home application, which is reported as a message rather than a crash.

A double tap on the empty area of Home locks the screen, the way the power button does, so fingerprint and face unlock keep working afterwards. It works only while the launcher accessibility service is on, which Android grants nowhere but in its own settings: the `Double tap to lock` setting sends the user there, and the setting reads whether the service is connected rather than a stored answer. Turning it off turns the service off, so the privilege never outlives the feature, and turning the service off in the Android settings stops the gesture too. The rows, the prompt, and scrolling keep working as they did, because a tap Home already handles never reaches the gesture.

The prompt can be customized from the settings screen, and the customization is cosmetic only. The Unix prompt is written as `username@hostname:path$`, where the username and the hostname are kept usable as prompt tokens by dropping whitespace and control characters and keeping at most sixteen characters, and where the end symbol is `$`, `%`, or `>`. The DOS prompt is written as `C:\HOME>` on the chosen drive letter, `A`, `C`, or `D`. Hiding the path shortens the Unix prompt to `username@hostname$` and the DOS prompt to `C:\>`. A username or hostname cleared in the settings is left out of the prompt with its separator, so clearing both leaves `~$`. The cursor is not a setting: the shell decides its shape, and DOS fills the character cell with a block while Unix marks the position with the line under it, so choosing a shell changes the cursor with everything else.

No drive, path, or symbol reaches a filesystem or grants a privilege: nothing exists behind them, and the launcher navigates no directories. The path shown is a fixed label for where the launcher is, not a working directory.

Home keeps one status line fixed above everything else, where scrolling never takes it away, with the clock on the left and the battery on the right, each written in the style of the selected shell: `42% charging` on Unix and `42% CHARGING` on DOS. The clock and the battery are each turned off in the settings screen, both are shown by default, hiding one leaves the other on its own side, and hiding both leaves no line at all. The battery follows the device instead of being read once, and a device reporting no level shows none rather than a number the launcher made up.

Home is read from the bottom, the way a terminal is. The prompt is anchored under everything else and stays there: the software keyboard pushes it up instead of covering it, and the rows above it never move it. Between the status line and the prompt, the pinned applications, the pinned shortcuts, the terminal history, and the search results share one scrolling region that rests on the bottom, so a Home holding little keeps it within reach of the thumb rather than hanging from the top of the screen. Whatever is printed or listed last is scrolled into view, so the answer to a command sits next to the line that asked for it.

Colour carries one distinction and only one: what can be started is written in the full terminal colour, and what is merely written, the status line, what a command printed, and the lines already submitted, is arrested to the second colour of the theme. A startable row is tall enough to be operated with a finger and centres its text in that height, while the written lines keep the tight spacing of a terminal, so a block of output reads as one block instead of a list of sentences. Pressing a startable row swaps the two colours for as long as it is held, the way a TTY marks a selection, which is the whole of the feedback a touch gets and costs the launcher no colour outside the selected theme.

Settings are opened with the `settings` command, which `help` lists along with the others.

`Immersive mode`, on by default, keeps the status and navigation bars hidden and brings them back only as transient bars on a swipe. Turning it off leaves the system bars on screen, and Home keeps its content clear of them.

The terminal history lives in memory only and starts empty after every process restart, by design.

The Android utility commands map to controlled Android functionality and nothing else. `battery` reads `BatteryManager`, which needs no permission, and says so when the device publishes no level. The status line reads the same `BatteryManager`, on every battery broadcast the device sends, and listens only while Home is on screen. `torch` calls `CameraManager.setTorchMode`, which needs no camera permission and opens no camera, and says so on a device with no flash unit. `android`, `wifi`, `bluetooth`, and `info` start one documented `Settings` action each. `uninstall` starts `ACTION_DELETE`, so Android shows its own confirmation naming the application and the user answers it; the launcher removes nothing itself. `restart` recreates the launcher the way a configuration change does, killing no process and spawning none. A device without one of these destinations, or an Android that refuses one, leaves the prompt working instead of crashing.

## Safety model

Locking the screen is the one privileged Android capability the launcher can hold. It is an accessibility service declaring the least it can: no window content, no gestures, and no accessibility event, so nothing on screen is ever read; the service carries no behavior beyond the lock. It is turned on only in the Android accessibility settings, is never requested at startup, and turns itself off the moment the setting is turned off. `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` is the only call it makes, which is the lock the power button performs: the launcher holds no device-admin privilege, and never forces the PIN that `DevicePolicyManager.lockNow()` would.

The project never executes real shell commands. `Runtime.exec`, `ProcessBuilder`, `/bin/sh`, `su`, shell passthrough, arbitrary Intent URIs, and unregistered commands are outside the architecture. Prompt input is tokenized and matched against explicitly registered commands only, and anything else falls back to application search. Every command maps to a controlled Android API or an explicit safe Intent: an Intent is never built from prompt input, and the only package name that ever reaches one is a package discovered through `PackageManager` and resolved to exactly one application. Every future command must meet the same rule.

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
                          \-> BatteryRepository -----> BatteryManager and battery broadcasts
                          \-> Torch -----------------> CameraManager torch mode
  submitted line -> SystemScreenLauncher ----> documented Settings and package Intents
          tap or Enter -> AppLauncher ----------> explicit package launch Intent
             shortcut tap -> ShortcutLauncher --> LauncherApps shortcut start
       shortcuts command -> ShortcutRepository -> LauncherApps published shortcuts
     pin request -> ShortcutPinRequests -------> LauncherApps pin item request
           double tap -> DeviceLock ------------> accessibility lock screen action
```

- `launcher`: installed-application model, repository boundary, PackageManager adapter, package-change monitoring, app launcher, battery reading and observation, torch boundary, the screen lock with its accessibility service, the named system destinations, and the shortcut model with its reading, pin request, and start boundaries;
- `preferences`: immutable launcher settings, repository boundary, and DataStore adapter;
- `command`: stable command identifiers, prompt tokenizing, explicit registration, execution, and the registered help, application-list, history-clearing, pinning, aliasing, settings, and Android utility commands;
- `search`: Compose-independent label matching and deterministic result ranking;
- `shell`: shell context, locations, profile selection, and all DOS/Unix presentation rules;
- `theme`: shell-independent terminal theme and color definitions;
- `ui`: terminal typography, theme provider, the destination the launcher renders, and the named list keys, labels, and test tags the screens and their tests share;
- `ui/home`: immutable UI state, screen-level ViewModel, terminal history, and stateless Compose rendering;
- `ui/settings`: immutable settings state, preference coordination, and stateless controls;
- `ui/pin`: immutable confirmation state, the answer to one pin request, and its stateless screen;
- `MainActivity` and `PinShortcutActivity`: Android composition roots only.

The repository, the package monitor, and the launcher own Android integration. Compose receives immutable state and emits user events, and what a submitted line asks the launcher to do travels back as a typed action. The command engine and the search engine never depend on Compose, so a new command registers through the command registry without touching Home.

## Coverage policy

Kover enforces 100% line coverage for testable application logic. Android lifecycle glue, Compose compiler-generated code, and generated Android classes are explicitly excluded because host-side unit coverage is not a meaningful measure for those surfaces. Exclusions must remain narrow, documented, and reviewer-approved. UI behavior should use Compose or device tests when introduced.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) and [AGENTS.md](AGENTS.md) before making changes. Issues, branches, commits, pull requests, code, and documentation are written in English.

## License

Licensed under the [Apache License 2.0](LICENSE).
