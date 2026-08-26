# Design

How Terminal Launcher behaves and why, kept out of the [README](../README.md) so the front page
stays a front page. The commands each shell accepts are listed
[there](../README.md#commands).

## Using the launcher

Type at the prompt. Matching applications appear right above it as you type; tap one to launch it, or press Enter when a single application matches. Enter always consumes the line, the way a desktop terminal does: the line joins the terminal history under the prompt that submitted it, the prompt is cleared, and an answer is written even when nothing was launched. The results say what they are: how many were found, or that none were. A name matching nothing is answered with `no application matches`, and a name matching several is answered with its candidates, which stay tappable rather than launching something arbitrary. The commands that resolve a name, such as `pin` and `alias`, answer an unresolved name the same way and list the same startable candidates. Enter on an empty prompt does nothing, since a blank line would only push printed output out of the history. Tapping a row clears the prompt as well, since the tap answered what was typed: the pinned applications, the pinned shortcuts, whatever a command listed, and the search results all behave the same, and the history stays, so `clear` keeps its meaning. Starting something puts the keyboard away and releases the prompt with it, whether a row was tapped or a submitted line resolved, since Home is handing the screen over and a prompt still focused brings the keyboard straight back on the way home. A command that only prints its answer leaves both alone, because the terminal has not gone anywhere. Anything else taking the screen releases it too, the recents switcher, the lock, or an application started from somewhere else, since Android gives the keyboard back to a field that is still focused when the launcher returns.

Holding a row writes the command it offers into the prompt instead of starting it: `unpin` for an application Home keeps, `pin` for one it only found, and the `shortcuts` command with its pinning or removing form for a shortcut. The line arrives ready to read and edit, with the prompt focused and the cursor at the end, and nothing runs until it is submitted. The command word is written in the case of the shell, so DOS writes `UNPIN`, while the name is left as the application carries it, since the parser resolves that name rather than the decorated one a list writes. Home teaches its own commands while it is used, without a menu.

A label matches exactly, by prefix, by substring, or fuzzily when the typed characters appear in it in order, and a literal match always outranks a fuzzy one. Matches of the same strength are ordered by a score worth 50 for a pinned application plus one point per launch up to 20, then by the most recent launch, then by name. Ranking reads only the installed applications and the stored launch history, so the same input always produces the same list.

## Commands and aliases

`help` writes every registered command under the group it acts on, the applications, Home, the device, and the launcher itself, one group after another with a blank line between them, and under each command the ways it is invoked, indented by two spaces, so the arguments a command takes are visible before running it. Every command declares its group, so one cannot be registered without saying where it is written, and the groups are written in the order they are declared rather than the order the commands were registered. A command writes those same invocation lines back when it is called the wrong way.

`alias` names an application so submitting that name launches it, for example `alias browser firefox`. Aliases persist across restarts, are matched without case, and are replaced by defining the same name again. A registered command is always resolved before an alias, and a name any shell already uses for a command is refused rather than allowed to shadow it, so no alias can ever take over `ls`, `clear`, or any other command.

`pin`, `unpin`, and `alias` accept an exact or unique application name; an ambiguous name is answered with the matching applications so a longer name can be given. Output, confirmations, and errors are written in the style of the selected shell: lowercase names on Unix, decorative `.EXE` names and uppercase messages on DOS.

## Pinned applications and shortcuts

An application can also ask the launcher to keep one of its own shortcuts, which is what a browser does when it adds a website to Home. The launcher answers with a confirmation naming the shortcut and the application that asked for it, and keeps nothing until it is accepted. An accepted shortcut is listed on Home under the pinned applications, written as `new tab` on Unix and `NEW TAB.LNK` on DOS, and tapping it asks Android to start it. A shortcut is started by the package and the identifier Android published for it, never by an Intent the launcher builds or the asking application supplies. Uninstalling an application removes the shortcuts it published, while updating it keeps them.

The same shortcuts are reachable from the prompt, without waiting for the application to offer them. `shortcuts <application>` lists what the application publishes and starts the listed shortcut that is tapped, `shortcuts pin <application> <shortcut>` keeps one on Home, and `shortcuts unpin <application> <shortcut>` removes a pinned one, whichever way it was pinned. The application is named by the first word and the shortcut by the rest of the line, a shortcut name matching more than one is answered with the candidates, and Android answers the question only while Terminal Launcher is the Home application, which is reported as a message rather than a crash.

## Installed applications

The application list refreshes when packages are added, changed, or removed. An uninstalled package is dropped from the pinned ones, while a package being updated keeps its pin, since an update is not a removal.

## Locking the screen

A double tap on the empty area of Home locks the screen, the way the power button does, so fingerprint and face unlock keep working afterwards. It works only while the launcher accessibility service is on, which Android grants nowhere but in its own settings: the `Double tap to lock` setting sends the user there, and the setting reads whether the service is connected rather than a stored answer. Turning it off turns the service off, so the privilege never outlives the feature, and turning the service off in the Android settings stops the gesture too. The rows, the prompt, and scrolling keep working as they did, because a tap Home already handles never reaches the gesture.

## The prompt

The prompt can be customized from the settings screen, and the customization is cosmetic only. The Unix prompt is written as `username@hostname:path$`, where the username and the hostname are kept usable as prompt tokens by dropping whitespace and control characters and keeping at most sixteen characters, and where the end symbol is `$`, `%`, or `>`. The DOS prompt is written as `C:\HOME>` on the chosen drive letter, `A`, `C`, or `D`. Hiding the path shortens the Unix prompt to `username@hostname$` and the DOS prompt to `C:\>`. A username or hostname cleared in the settings is left out of the prompt with its separator, so clearing both leaves `~$`. The cursor is not a setting: the shell decides its shape, and DOS fills the character cell with a block while Unix marks the position with the line under it, so choosing a shell changes the cursor with everything else.

No drive, path, or symbol reaches a filesystem or grants a privilege: nothing exists behind them, and the launcher navigates no directories. The path shown is a fixed label for where the launcher is, not a working directory.

## The status line

Home keeps one status line fixed above everything else, where scrolling never takes it away, with the clock on the left and the battery on the right, each written in the style of the selected shell: `42% charging` on Unix and `42% CHARGING` on DOS. The clock and the battery are each turned off in the settings screen, both are shown by default, hiding one leaves the other on its own side, and hiding both leaves no line at all. The battery follows the device instead of being read once, and a device reporting no level shows none rather than a number the launcher made up.

## How Home is laid out

Home is read from the bottom, the way a terminal is. The prompt is anchored under everything else and stays there: the software keyboard pushes it up instead of covering it, and the rows above it never move it. Between the status line and the prompt, the pinned applications, the pinned shortcuts, the terminal history, and the search results share one scrolling region that rests on the bottom, so a Home holding little keeps it within reach of the thumb rather than hanging from the top of the screen. Whatever is printed or listed last is scrolled into view, so the answer to a command sits next to the line that asked for it.

The pinned block says what it is. The shell writes a line above it naming what it lists, the way its own listing command names a directory: Unix writes `~/pinned:` as `ls` writes the path it was asked to list, while DOS writes `Directory of C:\HOME\PINNED` and closes the block with the file count `DIR` prints, on the drive the settings chose whether or not the prompt shows the path. Applications and shortcuts share the one section, since DOS already tells them apart through `.EXE` and `.LNK` and a second heading would take half of the space above the prompt. A Home with nothing pinned writes no section at all, and the lines are inert: they carry the arrested colour and answer no touch.

What the typed line matches is framed the same way, right above the prompt that is matching it. Unix announces the results before writing them, counting them as `1 match:` or `3 matches:`, while DOS closes them with `3 File(s) found` and answers an empty search with `File not found`, the message it already answers a failed listing with. The announcement appears as soon as the prompt holds anything, which includes the moment a command is being typed, since the search runs on every line and a command name usually matches no application: saying `no matches` there stays true, and Enter runs the command all the same. The pinned block stays where it is while a search runs, since nothing a terminal printed erases itself, and the two announcements are what tell the lists apart.

Colour carries one distinction and only one: what can be started is written in the full terminal colour, and what is merely written, the status line, what a command printed, and the lines already submitted, is arrested to the second colour of the theme. A startable row is tall enough to be operated with a finger and centres its text in that height, while the written lines keep the tight spacing of a terminal, so a block of output reads as one block instead of a list of sentences. Pressing a startable row swaps the two colours for as long as it is held, the way a TTY marks a selection, which is the whole of the feedback a touch gets and costs the launcher no colour outside the selected theme.

Each theme is three colours: the background, the colour what can be started is written in, and the second colour the written lines are arrested to, which always sits between the other two so the hierarchy reads the same whichever theme is chosen. Green, Amber, and Monochrome are phosphors on black, System follows the platform brightness, C64 reproduces the light blue on blue of the machine it is named after, dimmer than the others by design, and Solarized is the dark palette of that name, the one to reach for when the screen is read for long. The system bar icons follow the background luminance, so a theme brings no code with it beyond its three colours.

## Settings

Settings are opened with the `settings` command, which `help` lists along with the others. The screen reads in the selected shell, the way Home does: every section title, option, toggle, and the back row is written by `ShellProfile`, so DOS reads `< BACK`, `APPEARANCE`, and `[*] SHOW CLOCK` while Unix reads them lowercase. Choosing the other shell rewrites the screen where it stands, and Compose branches on nothing.

`Immersive mode`, on by default, keeps the status and navigation bars hidden and brings them back only as transient bars on a swipe. Turning it off leaves the system bars on screen, and Home keeps its content clear of them.

A Home with nothing pinned, nothing listed, and nothing printed writes one inert line inviting the `help` command, formatted by the shell, so it reads in upper case under DOS. It is a function of what Home holds rather than a stored state: it leaves as soon as something is pinned or a line is submitted, and it comes back when `clear` empties an unpinned Home.

## The terminal history

Home keeps the twenty most recent submitted lines with their output, and with the applications and shortcuts they listed, which stay startable. The terminal history lives in memory only and starts empty after every process restart, by design.

## Android utility commands

The Android utility commands map to controlled Android functionality and nothing else. `battery` reads `BatteryManager`, which needs no permission, and says so when the device publishes no level. The status line reads the same `BatteryManager`, on every battery broadcast the device sends, and listens only while Home is on screen. `torch` calls `CameraManager.setTorchMode`, which needs no camera permission and opens no camera, and says so on a device with no flash unit. `android`, `wifi`, `bluetooth`, and `info` start one documented `Settings` action each. `uninstall` starts `ACTION_DELETE`, so Android shows its own confirmation naming the application and the user answers it; the launcher removes nothing itself. `restart` recreates the launcher the way a configuration change does, killing no process and spawning none. A device without one of these destinations, or an Android that refuses one, leaves the prompt working instead of crashing.
