package com.gybra.terminallauncher.launcher

/** A [BatteryRepository] answering with a fixed reading, so tests never touch a device. */
class FakeBatteryRepository(
    private val status: BatteryStatus? = BatteryStatus(percentage = 42, charging = false),
) : BatteryRepository {
    override suspend fun readStatus(): BatteryStatus? = status
}
