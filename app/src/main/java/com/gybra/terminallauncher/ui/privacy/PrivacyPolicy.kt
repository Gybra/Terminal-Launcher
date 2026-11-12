package com.gybra.terminallauncher.ui.privacy

/** Offline policy text; no network request or external account is needed to read it. */
public object PrivacyPolicy {
    public const val TITLE: String = "Terminal Launcher Privacy Policy"
    public const val SUPPORT_EMAIL: String = "assistenza@hexlabsoftware.it"
    public const val CONTACT: String = "Support and privacy questions: $SUPPORT_EMAIL"
    public val BODY: String = """
        Publisher: HexLab Software
        App: Terminal Launcher (com.gybra.terminallauncher)
        Updated: November 12, 2026

        Terminal Launcher is an Android home-screen launcher with a terminal-style interface, not a terminal emulator. It does not execute arbitrary shell commands.

        LOCAL DATA
        The launcher reads names and package identifiers of launchable installed apps to search, list, pin and open them. It saves your preferences, prompt display labels, pins, shortcuts, aliases, accessibility consent choice and the frequency/time of launches made through this launcher in its private local storage. Launch history is used to rank search results, not to monitor other app activity. When you ask for an app's shortcuts, the launcher reads the identifiers and labels that app publishes to launchers; labels may include personal names, such as conversations. Pinning a shortcut saves its app package, identifier and label locally.

        Prompt input and displayed command history are held in memory, not saved as a transcript. Commands that change settings, aliases or pins save those specific changes. Battery state and device time are read to display the optional status line and battery command.

        OPTIONAL ACCESSIBILITY SERVICE
        After separate in-app consent and your approval in Android settings, the service can lock the screen, open notifications, open quick settings and open Overview. It cannot retrieve screen content, subscribes to no accessibility events and does not inject touch gestures. It does not collect or share personal data through this service. You can decline these gestures and still search for and launch apps, and can disable the service in Android settings.

        OPTIONAL NOTIFICATION ACCESS
        If you enable notification access, the launcher uses notification identifiers, app packages, grouping and clearable/group-summary flags to show counts beside app names. It does not read notification titles, messages, senders or media. The counts and metadata remain in memory and are cleared when the listener disconnects or its process ends. You can revoke access in Android settings.

        DATA LEAVING THE APP
        Terminal Launcher has no Internet permission, analytics SDK, advertising SDK, application account or developer-operated backend. It does not upload your app list, local settings, prompt history, launch ranking or notification metadata to the developer. When you open another app, a shortcut, system settings or an uninstall confirmation, Android or the selected app handles that action under its own policies. Android and Google Play may independently process platform data under their own policies.

        STORAGE AND DELETION
        Saved settings remain until you change them, clear the app's storage or uninstall the app. Clearing storage or uninstalling removes local app data and cannot be undone by this app. The clear command clears displayed history, not all settings. The app disables Android backup in its manifest and declares backup/transfer exclusions; device vendors control system transfer behavior. No developer cloud backup or account-deletion workflow exists because there is no application account or backend.

        POLICY CHANGES
        This policy is updated when the app's data handling changes. The current text is available here without a network connection.
    """.trimIndent()
}
