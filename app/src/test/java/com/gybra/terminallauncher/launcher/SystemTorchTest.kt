package com.gybra.terminallauncher.launcher

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemTorchTest {
    @Test
    fun `turns the torch on and off again on a camera that has a flash`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val cameraManager = context.getSystemService(CameraManager::class.java)
        val shadow: ShadowCameraManager = Shadow.extract(cameraManager)
        shadow.addCamera("0", cameraWithFlash(available = true))
        val torch = SystemTorch(context)

        assertEquals(TorchState.ON, torch.toggle())
        assertTrue(shadow.getTorchMode("0"))

        assertEquals(TorchState.OFF, torch.toggle())
    }

    @Test
    fun `picks the camera that has a flash`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val cameraManager = context.getSystemService(CameraManager::class.java)
        val shadow: ShadowCameraManager = Shadow.extract(cameraManager)
        shadow.addCamera("front", cameraWithFlash(available = false))
        shadow.addCamera("back", cameraWithFlash(available = true))

        assertEquals(TorchState.ON, SystemTorch(context).toggle())

        assertTrue(shadow.getTorchMode("back"))
    }

    @Test
    fun `reports a device whose cameras have no flash`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val cameraManager = context.getSystemService(CameraManager::class.java)
        val shadow: ShadowCameraManager = Shadow.extract(cameraManager)
        shadow.addCamera("front", cameraWithFlash(available = false))

        assertEquals(TorchState.UNAVAILABLE, SystemTorch(context).toggle())
    }

    @Test
    fun `reports a device with no camera at all`() = runTest {
        assertEquals(
            TorchState.UNAVAILABLE,
            SystemTorch(RuntimeEnvironment.getApplication()).toggle(),
        )
    }

    @Test
    fun `reports a device without the camera service`() = runTest {
        val context: Context = mock()
        whenever(context.getSystemService(CameraManager::class.java)).thenReturn(null)

        assertEquals(TorchState.UNAVAILABLE, SystemTorch(context).toggle())
    }

    @Test
    fun `reports a camera list Android refuses to read`() = runTest {
        val cameraManager: CameraManager = mock()
        whenever(cameraManager.cameraIdList).thenThrow(
            CameraAccessException(CameraAccessException.CAMERA_DISABLED),
        )

        assertEquals(TorchState.UNAVAILABLE, SystemTorch(contextWith(cameraManager)).toggle())
    }

    @Test
    fun `reports a torch Android refuses to switch`() = runTest {
        val cameraManager = flashCameraManager()
        doThrow(CameraAccessException(CameraAccessException.CAMERA_IN_USE))
            .whenever(cameraManager)
            .setTorchMode(any(), any())

        assertEquals(TorchState.UNAVAILABLE, SystemTorch(contextWith(cameraManager)).toggle())
    }

    @Test
    fun `reports a torch Android no longer knows`() = runTest {
        val cameraManager = flashCameraManager()
        doThrow(IllegalArgumentException("unknown camera"))
            .whenever(cameraManager)
            .setTorchMode(any(), any())

        assertEquals(TorchState.UNAVAILABLE, SystemTorch(contextWith(cameraManager)).toggle())
    }

    private fun flashCameraManager(): CameraManager {
        val cameraManager: CameraManager = mock()
        whenever(cameraManager.cameraIdList).thenReturn(arrayOf("0"))
        whenever(cameraManager.getCameraCharacteristics("0"))
            .thenReturn(cameraWithFlash(available = true))

        return cameraManager
    }

    private fun contextWith(cameraManager: CameraManager): Context {
        val context: Context = mock()
        whenever(context.getSystemService(CameraManager::class.java)).thenReturn(cameraManager)

        return context
    }

    private fun cameraWithFlash(available: Boolean): CameraCharacteristics {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadow: ShadowCameraCharacteristics = Shadow.extract(characteristics)
        shadow.set(CameraCharacteristics.FLASH_INFO_AVAILABLE, available)

        return characteristics
    }
}
