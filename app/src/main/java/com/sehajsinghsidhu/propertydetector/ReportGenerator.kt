package com.sehajsinghsidhu.propertydetector

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportGenerator(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun generateCSV(trip: Trip, detections: List<Detection>): File {
        val file = File(context.filesDir, "report_${trip.tripId}.csv")
        FileWriter(file).use { writer ->
            // Trip summary
            writer.append("TRIP SUMMARY\n")
            writer.append("Trip ID,${trip.tripId}\n")
            writer.append("Start Time,${dateFormat.format(Date(trip.startTime))}\n")
            writer.append("End Time,${dateFormat.format(Date(trip.endTime))}\n")
            writer.append("Duration (mins),${trip.duration / 60000}\n")
            writer.append("Distance (km),${String.format("%.2f", trip.distance / 1000)}\n")
            writer.append("Total Detections,${detections.size}\n")
            writer.append("\n")

            // Breakdown by type
            writer.append("BREAKDOWN BY TYPE\n")
            val grouped = detections.groupBy { it.propertyType }
            grouped.forEach { (type, list) ->
                writer.append("$type,${list.size}\n")
            }
            writer.append("\n")

            // Detection details
            writer.append("DETECTIONS\n")
            writer.append("Type,Latitude,Longitude,Timestamp,Confidence,Image\n")
            detections.forEach { detection ->
                writer.append("${detection.propertyType},")
                writer.append("${detection.latitude},")
                writer.append("${detection.longitude},")
                writer.append("${dateFormat.format(Date(detection.timestamp))},")
                writer.append("${String.format("%.2f", detection.confidence)},")
                writer.append("${detection.imagePath}\n")
            }
        }
        return file
    }

    fun generatePDF(trip: Trip, detections: List<Detection>): File {
        val file = File(context.filesDir, "report_${trip.tripId}.pdf")
        val writer = com.itextpdf.kernel.pdf.PdfWriter(file)
        val pdf = com.itextpdf.kernel.pdf.PdfDocument(writer)
        val document = com.itextpdf.layout.Document(pdf)

        document.add(com.itextpdf.layout.element.Paragraph("Property Detection Report")
            .setFontSize(20f).setBold())
        document.add(com.itextpdf.layout.element.Paragraph("Trip ID: ${trip.tripId}"))
        document.add(com.itextpdf.layout.element.Paragraph("Start: ${dateFormat.format(Date(trip.startTime))}"))
        document.add(com.itextpdf.layout.element.Paragraph("End: ${dateFormat.format(Date(trip.endTime))}"))
        document.add(com.itextpdf.layout.element.Paragraph("Duration: ${trip.duration / 60000} minutes"))
        document.add(com.itextpdf.layout.element.Paragraph("Distance: ${String.format("%.2f", trip.distance / 1000)} km"))
        document.add(com.itextpdf.layout.element.Paragraph("Total Detections: ${detections.size}"))
        document.add(com.itextpdf.layout.element.Paragraph(" "))

        val grouped = detections.groupBy { it.propertyType }
        document.add(com.itextpdf.layout.element.Paragraph("Breakdown by Type:").setBold())
        grouped.forEach { (type, list) ->
            document.add(com.itextpdf.layout.element.Paragraph("  $type: ${list.size}"))
        }

        document.close()
        return file
    }

    fun generateTripSummary(trip: Trip, detections: List<Detection>): String {
        val grouped = detections.groupBy { it.propertyType }
        val sb = StringBuilder()

        sb.append("Trip Summary\n")
        sb.append("Duration: ${trip.duration / 60000} minutes\n")
        sb.append("Distance: ${String.format("%.2f", trip.distance / 1000)} km\n")
        sb.append("Total Detections: ${detections.size}\n\n")
        sb.append("Breakdown:\n")
        grouped.forEach { (type, list) ->
            sb.append("  $type: ${list.size}\n")
        }

        return sb.toString()
    }
}