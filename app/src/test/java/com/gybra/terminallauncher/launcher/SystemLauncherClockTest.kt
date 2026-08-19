package com.gybra.terminallauncher.launcher

import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SystemLauncherClockTest {
    @Test
    fun `emits formatted time on every tick`() = runTest {
        val times = ArrayDeque(
            listOf(
                LocalTime.of(8, 5, 59, 500_000_000),
                LocalTime.of(22, 10),
            ),
        )
        val clock = SystemLauncherClock(
            currentTime = times::removeFirst,
        )

        val values = async { clock.observeTime().take(2).toList() }
        runCurrent()
        advanceTimeBy(500L)
        runCurrent()

        assertEquals(listOf("08:05", "22:10"), values.await())
    }

    @Test
    fun `default clock emits a valid twenty four hour time`() = runTest {
        val value = SystemLauncherClock().observeTime().first()

        assertTrue(value.matches(Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]")))
    }

    @Test
    fun `formats midnight as zero hours`() = runTest {
        val value = SystemLauncherClock(
            currentTime = { LocalTime.MIDNIGHT },
        ).observeTime().first()

        assertEquals("00:00", value)
    }
}
