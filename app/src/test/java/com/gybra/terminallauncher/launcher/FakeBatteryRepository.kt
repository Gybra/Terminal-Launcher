package com.gybra.terminallauncher.launcher

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A [BatteryRepository] answering with a fixed reading, so tests never touch a device. */
class FakeBatteryRepository(
    status: BatteryStatus? = BatteryStatus(percentage = 42, charging = false),
) : BatteryRepository {
    private val observedStatus = MutableStateFlow(status)

    override suspend fun readStatus(): BatteryStatus? = observedStatus.value

    override fun observeStatus(): Flow<BatteryStatus?> = observedStatus

    fun emit(status: BatteryStatus?) {
        observedStatus.value = status
    }
}
