package com.gybra.terminallauncher.ui.privacy

/** Copy for the separate optional-access disclosure; Android permission is requested afterwards. */
public object AccessibilityDisclosureText {
    public const val TITLE: String = "Optional accessibility gestures"
    public const val AGREE: String = "Agree and open Android settings"
    public const val NOT_NOW: String = "Not now"
    public val BODY: String = """
        Terminal Launcher uses Android's AccessibilityService only to:
        • Lock the screen when you double tap empty space.
        • Open notifications when you swipe down on the left.
        • Open quick settings when you swipe down on the right.
        • Open Overview when you swipe up and Home cannot scroll further.

        This service does not read screen content or accessibility events, inject touch gestures, or collect or share personal data. Your consent choice is saved only on this device.

        These gestures are optional. You can search for and launch apps without them. You can turn the service off at any time in Android's accessibility settings.

        Choosing Agree saves your consent, then opens Android settings. You still decide there whether to enable the service.
    """.trimIndent()
}
