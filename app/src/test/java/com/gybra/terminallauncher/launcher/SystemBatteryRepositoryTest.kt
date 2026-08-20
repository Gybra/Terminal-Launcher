package com.gybra.terminallauncher.launcher

import android.content.Context
import android.os.BatteryManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBatteryManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemBatteryRepositoryTest {
    @Test
    fun `reads the level and the charging state the device publishes`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        batteryShadow(context).apply {
            setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 42)
            setIsCharging(true)
        }

        assertEquals(
            BatteryStatus(percentage = 42, charging = true),
            SystemBatteryRepository(context).readStatus(),
        )
    }

    @Test
    fun `reads an empty and a full battery`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val shadow = batteryShadow(context)
        val repository = SystemBatteryRepository(context)

        shadow.setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 0)
        assertEquals(BatteryStatus(percentage = 0, charging = false), repository.readStatus())

        shadow.setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 100)
        assertEquals(BatteryStatus(percentage = 100, charging = false), repository.readStatus())
    }

    @Test
    fun `reports no status when the device publishes a level outside the scale`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val repository = SystemBatteryRepository(context)

        batteryShadow(context).setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, -1)
        assertNull(repository.readStatus())

        batteryShadow(context).setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 101)
        assertNull(repository.readStatus())
    }

    @Test
    fun `reports no status on a device without the battery service`() = runTest {
        val context: Context = mock()
        whenever(context.getSystemService(BatteryManager::class.java)).thenReturn(null)

        assertNull(SystemBatteryRepository(context).readStatus())
    }

    private fun batteryShadow(context: Context): ShadowBatteryManager =
        Shadow.extract(context.getSystemService(BatteryManager::class.java))
}
