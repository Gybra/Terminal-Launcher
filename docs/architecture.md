# Architecture

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
         swipe down -> SystemShade -----------> accessibility notification and quick settings actions
         swipe up -> SystemOverview -------> accessibility recents action
```

- `launcher`: installed-application model, repository boundary, PackageManager adapter, package-change monitoring, app launcher, battery reading and observation, torch boundary, the screen lock with its accessibility service, the notification shade and quick settings, the named system destinations, and the shortcut model with its reading, pin request, and start boundaries;
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
