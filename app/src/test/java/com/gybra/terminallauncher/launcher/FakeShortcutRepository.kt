package com.gybra.terminallauncher.launcher

/** A [ShortcutRepository] answering with what a test stored, so no `LauncherApps` is needed. */
class FakeShortcutRepository(
    private val published: Map<String, PublishedShortcuts> = emptyMap(),
    private val default: PublishedShortcuts = PublishedShortcuts.Available(emptyList()),
) : ShortcutRepository {
    val questions: MutableList<String> = mutableListOf()

    override suspend fun publishedBy(packageName: String): PublishedShortcuts {
        questions += packageName

        return published[packageName] ?: default
    }
}
