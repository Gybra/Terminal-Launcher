package com.gybra.terminallauncher.launcher

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

public class SystemLauncherClock(
    private val currentTime: () -> LocalTime = LocalTime::now,
    private val currentEpochMillis: () -> Long = System::currentTimeMillis,
) : LauncherClock {
    override fun observeTime(): Flow<String> = flow {
        while (currentCoroutineContext().isActive) {
            val time = currentTime()
            emit(time.format(TIME_FORMATTER))
            delay(time.millisUntilNextMinute())
        }
    }

    override fun now(): Long = currentEpochMillis()

    private fun LocalTime.millisUntilNextMinute(): Long =
        MILLIS_PER_MINUTE - (second * MILLIS_PER_SECOND) - (nano / NANOS_PER_MILLI)

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val MILLIS_PER_MINUTE = 60L * MILLIS_PER_SECOND
        const val NANOS_PER_MILLI = 1_000_000L
        val TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    }
}
