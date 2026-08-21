package com.gybra.terminallauncher.launcher

/** A [ShortcutPinRequest] that answers what a test asks for and counts the answers it gave. */
class FakeShortcutPinRequest(
    override val shortcut: AppShortcut = AppShortcut(
        packageName = "org.example.browser",
        id = "new-tab",
        label = "New tab",
    ),
    private val accepted: Boolean = true,
) : ShortcutPinRequest {
    var accepts: Int = 0
        private set

    override fun accept(): Boolean {
        accepts += 1

        return accepted
    }
}
