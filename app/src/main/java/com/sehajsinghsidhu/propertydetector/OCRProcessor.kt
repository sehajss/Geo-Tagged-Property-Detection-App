package com.sehajsinghsidhu.propertydetector

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OCRProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val keywords = listOf(
        "for sale", "sale", "plot", "house", "flat",
        "commercial", "property", "land", "site", "villa",
        "apartment", "office", "shop", "contact", "call"
    )

    fun processImage(
        bitmap: Bitmap,
        onResult: (isForSale: Boolean, detectedText: String, propertyType: String) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val detectedText = visionText.text.lowercase()
                val isForSale = keywords.any { keyword ->
                    detectedText.contains(keyword)
                }

                val propertyType = when {
                    detectedText.contains("plot") -> "Plot for Sale"
                    detectedText.contains("house") -> "House for Sale"
                    detectedText.contains("flat") || detectedText.contains("apartment") -> "Flat for Sale"
                    detectedText.contains("commercial") || detectedText.contains("shop") ||
                            detectedText.contains("office") -> "Commercial Space"
                    detectedText.contains("villa") -> "Villa for Sale"
                    detectedText.contains("land") || detectedText.contains("site") -> "Land for Sale"
                    isForSale -> "For Sale"
                    else -> "Signboard"
                }

                onResult(isForSale, visionText.text, propertyType)
            }
            .addOnFailureListener {
                onResult(false, "", "Unknown")
            }
    }

    fun close() {
        recognizer.close()
    }
}