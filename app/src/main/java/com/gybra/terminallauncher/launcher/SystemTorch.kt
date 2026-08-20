package com.gybra.terminallauncher.launcher

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Drives the torch through `CameraManager.setTorchMode`, which needs no camera permission and
 * opens no camera. A device without a flash unit, or one that refuses the call, reports
 * [TorchState.UNAVAILABLE] instead of failing.
 *
 * The state mirrors what the launcher last set, because Android reports the torch back only
 * through a callback this small adapter does not keep.
 */
public class SystemTorch(
    private val context: Context,
) : Torch {
    private var enabled = false

    override suspend fun toggle(): TorchState = withContext(Dispatchers.IO) {
        val cameraManager = context.getSystemService(CameraManager::class.java)
            ?: return@withContext TorchState.UNAVAILABLE
        val cameraId = cameraManager.flashCameraId() ?: return@withContext TorchState.UNAVAILABLE

        try {
            cameraManager.setTorchMode(cameraId, !enabled)
            enabled = !enabled
            if (enabled) TorchState.ON else TorchState.OFF
        } catch (_: CameraAccessException) {
            TorchState.UNAVAILABLE
        } catch (_: IllegalArgumentException) {
            TorchState.UNAVAILABLE
        }
    }

    private fun CameraManager.flashCameraId(): String? = try {
        cameraIdList.firstOrNull { cameraId ->
            getCameraCharacteristics(cameraId)[CameraCharacteristics.FLASH_INFO_AVAILABLE] == true
        }
    } catch (_: CameraAccessException) {
        null
    }
}
