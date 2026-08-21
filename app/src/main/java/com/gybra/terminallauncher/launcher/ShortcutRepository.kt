package com.gybra.terminallauncher.launcher

public interface ShortcutRepository {
    /** Reads the shortcuts [packageName] publishes, or reports Android refusing the question. */
    public suspend fun publishedBy(packageName: String): PublishedShortcuts
}
