package com.sehajsinghsidhu.propertydetector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DetectionManager(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val ocrProcessor = OCRProcessor()
    private var lastDetectionTime = 0L
    private val detectionIntervalMs = 2000L

    init {
        setupDetector()
    }

    private fun setupDetector() {
        try {
            val assetFileDescriptor = context.assets.openFd("signboard_model.tflite")
            val inputStream = assetFileDescriptor.createInputStream()
            val modelBytes = inputStream.readBytes()

            val buffer = ByteBuffer.allocateDirect(modelBytes.size)
            buffer.order(ByteOrder.nativeOrder())
            buffer.put(modelBytes)

            interpreter = Interpreter(buffer)

            val inputShape = interpreter?.getInputTensor(0)?.shape()
            val outputShape = interpreter?.getOutputTensor(0)?.shape()

            android.util.Log.d("DetectionManager", "Input shape: ${inputShape?.contentToString()}")
            android.util.Log.d("DetectionManager", "Output shape: ${outputShape?.contentToString()}")

            android.util.Log.d("DetectionManager", "TFLite model loaded successfully")

        } catch (e: Exception) {
            android.util.Log.e("DetectionManager", "Model failed to load: ${e.message}")
            e.printStackTrace()
        }
    }

    fun processFrame(
        imageProxy: ImageProxy,
        onForSaleDetected: (propertyType: String, confidence: Float, bitmap: Bitmap) -> Unit
    ) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastDetectionTime < detectionIntervalMs) {
            imageProxy.close()
            return
        }

        lastDetectionTime = currentTime

        try {
            val bitmap = imageProxyToBitmap(imageProxy) ?: run {
                imageProxy.close()
                return
            }

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true)

            val inputBuffer = ByteBuffer.allocateDirect(1 * 640 * 640 * 3 * 4)
            inputBuffer.order(ByteOrder.nativeOrder())

            for (y in 0 until 640) {
                for (x in 0 until 640) {
                    val pixel = resizedBitmap.getPixel(x, y)

                    inputBuffer.putFloat(((pixel shr 16 and 0xFF) / 255.0f))
                    inputBuffer.putFloat(((pixel shr 8 and 0xFF) / 255.0f))
                    inputBuffer.putFloat(((pixel and 0xFF) / 255.0f))
                }
            }

            val output = Array(1) { Array(5) { FloatArray(8400) } }

            interpreter?.run(inputBuffer, output)

            var bestConfidence = 0f

            for (i in 0 until 8400) {

                val x = output[0][0][i]
                val y = output[0][1][i]
                val w = output[0][2][i]
                val h = output[0][3][i]
                val confidence = output[0][4][i]

                if (i < 5) {
                    android.util.Log.d(
                        "DetectionManager",
                        "Detection $i -> x:$x y:$y w:$w h:$h conf:$confidence"
                    )
                }

                if (confidence > bestConfidence) {
                    bestConfidence = confidence
                }
            }

            android.util.Log.d("DetectionManager", "Best confidence: $bestConfidence")

            if (bestConfidence > 0.05f) {

                ocrProcessor.processImage(bitmap) { isForSale, _, propertyType ->

                    if (isForSale) {
                        onForSaleDetected(propertyType, bestConfidence, bitmap)
                    }
                }
            }


        } catch (e: Exception) {
            android.util.Log.e("DetectionManager", "Frame processing error: ${e.message}")
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 80, out)
            val imageBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    fun shutdown() {
        interpreter?.close()
        ocrProcessor.close()
    }
}