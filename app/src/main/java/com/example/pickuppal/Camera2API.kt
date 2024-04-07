package com.example.pickuppal

import android.content.Context
import android.graphics.Camera
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.HandlerThread
import android.util.Log
import android.widget.Toast
import java.util.logging.Handler


class Camera2API(private val context: Context) {
    private lateinit var backgroundHandlerThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    init {
        startBackgroundThread()
    }


    private fun startBackgroundThread() {
        backgroundHandlerThread = HandlerThread("CameraBackgroundThread")
        backgroundHandlerThread.start()
        backgroundHandler = Handler(backgroundHandlerThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundHandlerThread.quitSafely()
        try {
            backgroundHandlerThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun setupCamera(onImageAvailableListener: ImageReader.OnImageAvailableListener) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraIds = cameraManager.cameraIdList
            var cameraId: String? = null

            for (id in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                // choose lens facing back camera
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    break
                }
            }

            if (cameraId != null) {
                val previewSize = cameraManager.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?.getOutputSizes(ImageFormat.JPEG)
                    ?.maxByOrNull { it.height * it.width }

                val imageReader = ImageReader.newInstance(previewSize!!.width, previewSize.height, ImageFormat.JPEG, 1)
                imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)

                // Do something with the selected camera ID and ImageReader
                // cameraId can be used to open the camera later
            } else {
                Log.d("Camera", "No valid back camera found")
                Toast.makeText(context, "No valid camera found", Toast.LENGTH_SHORT).show()
            }

        } catch (e: CameraAccessException) {
            e.printStackTrace()
            Toast.makeText(context, "Cannot access camera", Toast.LENGTH_SHORT).show()
        }
    }


}