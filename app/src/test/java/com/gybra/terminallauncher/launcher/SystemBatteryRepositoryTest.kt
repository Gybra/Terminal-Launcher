package com.gybra.terminallauncher.launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Looper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBatteryManager

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Test
    fun `observes the level the device publishes when collection starts`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        batteryShadow(context).setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 42)

        assertEquals(
            BatteryStatus(percentage = 42, charging = false),
            repository(context).observeStatus().first(),
        )
    }

    @Test
    fun `observes the level again when the device broadcasts a change`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val shadow = batteryShadow(context)
        shadow.setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 42)
        val statuses = mutableListOf<BatteryStatus?>()
        val collection = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository(context).observeStatus().toList(statuses)
        }

        shadow.setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 41)
        shadow.setIsCharging(true)
        context.sendBatteryChange()
        collection.cancel()

        assertEquals(
            listOf(
                BatteryStatus(percentage = 42, charging = false),
                BatteryStatus(percentage = 41, charging = true),
            ),
            statuses,
        )
    }

    @Test
    fun `reports an unchanged battery once`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        batteryShadow(context).setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 42)
        val statuses = mutableListOf<BatteryStatus?>()
        val collection = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository(context).observeStatus().toList(statuses)
        }

        context.sendBatteryChange()
        context.sendBatteryChange()
        collection.cancel()

        assertEquals(listOf(BatteryStatus(percentage = 42, charging = false)), statuses)
    }

    @Test
    fun `stops listening to the device when collection ends`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val collection = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository(context).observeStatus().collect { }
        }

        assertEquals(1, batteryReceiverCount(context))
        collection.cancel()

        assertEquals(0, batteryReceiverCount(context))
    }

    private fun TestScope.repository(context: Context): SystemBatteryRepository =
        SystemBatteryRepository(context, UnconfinedTestDispatcher(testScheduler))

    private fun batteryReceiverCount(application: Application): Int =
        shadowOf(application).registeredReceivers.count { receiver ->
            receiver.intentFilter.hasAction(Intent.ACTION_BATTERY_CHANGED)
        }

    private fun Context.sendBatteryChange() {
        sendBroadcast(Intent(Intent.ACTION_BATTERY_CHANGED))
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun batteryShadow(context: Context): ShadowBatteryManager =
        Shadow.extract(context.getSystemService(BatteryManager::class.java))
}
