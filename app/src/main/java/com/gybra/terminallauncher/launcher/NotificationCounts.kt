package com.gybra.terminallauncher.launcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Process-local view of the active notification set, replaced on listener reconnection. */
public class NotificationCounts {
    private val entries = mutableMapOf<String, NotificationEntry>()
    private val mutableValues = MutableStateFlow<Map<String, Int>>(emptyMap())
    public val values: StateFlow<Map<String, Int>> = mutableValues

    public fun replace(notifications: List<NotificationEntry>) {
        entries.clear()
        notifications.forEach { entry -> entries[entry.key] = entry }
        publish()
    }

    public fun post(entry: NotificationEntry) {
        entries[entry.key] = entry
        publish()
    }

    public fun remove(key: String) {
        if (entries.remove(key) != null) publish()
    }

    public fun clear() {
        entries.clear()
        publish()
    }

    private fun publish() {
        val groupedChildren = entries.values.asSequence()
            .filter { entry -> entry.clearable && !entry.summary && entry.group != null }
            .map { entry -> entry.packageName to entry.group }
            .toSet()
        val updated = entries.values.asSequence()
            .filter { entry -> entry.clearable }
            .filterNot { entry -> entry.summary &&
                (entry.packageName to entry.group) in groupedChildren }
            .groupingBy { entry -> entry.packageName }
            .eachCount()
        if (updated != mutableValues.value) mutableValues.value = updated
    }
}
