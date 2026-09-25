package com.gybra.terminallauncher.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

public class NotificationCountsTest {
    private val counts = NotificationCounts()

    @Test
    public fun `counts only dismissible notifications and replaces snapshot on reconnect`() {
        counts.replace(listOf(entry("a"), entry("b", clearable = false)))
        assertEquals(mapOf("app" to 1), counts.values.value)
        counts.replace(listOf(entry("c", packageName = "other")))
        assertEquals(mapOf("other" to 1), counts.values.value)
        counts.clear()
        assertEquals(emptyMap<String, Int>(), counts.values.value)
    }

    @Test
    public fun `update and removal change only affected package`() {
        counts.replace(listOf(entry("a"), entry("b", packageName = "other")))
        counts.post(entry("a", packageName = "other"))
        assertEquals(mapOf("other" to 2), counts.values.value)
        counts.remove("b")
        assertEquals(mapOf("other" to 1), counts.values.value)
        counts.remove("missing")
        assertEquals(mapOf("other" to 1), counts.values.value)
    }

    @Test
    public fun `group summary is not counted alongside dismissible children`() {
        counts.replace(listOf(entry("summary", summary = true, group = "g"), entry("child", group = "g")))
        assertEquals(mapOf("app" to 1), counts.values.value)
        counts.remove("child")
        assertEquals(mapOf("app" to 1), counts.values.value)
    }

    @Test
    public fun `duplicate events and non-clearable updates do not publish duplicate counts`() {
        counts.post(entry("a"))
        val previous = counts.values.value
        counts.post(entry("a"))
        assertEquals(previous, counts.values.value)
        counts.post(entry("a", clearable = false))
        assertEquals(emptyMap<String, Int>(), counts.values.value)
        counts.post(entry("a", clearable = true))
        assertEquals(mapOf("app" to 1), counts.values.value)
    }

    @Test
    public fun `group summaries of distinct packages remain independent`() {
        counts.replace(listOf(
            entry("summary", summary = true, group = "g"),
            entry("child", packageName = "other", group = "g"),
        ))
        assertEquals(mapOf("app" to 1, "other" to 1), counts.values.value)
    }

    private fun entry(
        key: String,
        packageName: String = "app",
        clearable: Boolean = true,
        summary: Boolean = false,
        group: String? = null,
    ): NotificationEntry = NotificationEntry(key, packageName, clearable, summary, group)
}
