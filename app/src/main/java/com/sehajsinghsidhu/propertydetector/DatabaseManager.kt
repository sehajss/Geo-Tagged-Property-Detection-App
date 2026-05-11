package com.sehajsinghsidhu.propertydetector

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Trip(
    val id: Long = 0,
    val tripId: String,
    val startTime: Long,
    var endTime: Long = 0,
    var distance: Float = 0f,
    var duration: Long = 0
)

data class Detection(
    val id: Long = 0,
    val tripId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val propertyType: String,
    val imagePath: String,
    val confidence: Float
)

class DatabaseManager(context: Context) : SQLiteOpenHelper(context, "propertydetector.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE trips (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                trip_id TEXT,
                start_time INTEGER,
                end_time INTEGER,
                distance REAL,
                duration INTEGER
            )
        """)

        db.execSQL("""
            CREATE TABLE detections (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                trip_id TEXT,
                latitude REAL,
                longitude REAL,
                timestamp INTEGER,
                property_type TEXT,
                image_path TEXT,
                confidence REAL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS trips")
        db.execSQL("DROP TABLE IF EXISTS detections")
        onCreate(db)
    }

    fun insertTrip(trip: Trip): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("trip_id", trip.tripId)
            put("start_time", trip.startTime)
            put("end_time", trip.endTime)
            put("distance", trip.distance)
            put("duration", trip.duration)
        }
        return db.insert("trips", null, values)
    }

    fun updateTrip(trip: Trip) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("end_time", trip.endTime)
            put("distance", trip.distance)
            put("duration", trip.duration)
        }
        db.update("trips", values, "trip_id = ?", arrayOf(trip.tripId))
    }

    fun insertDetection(detection: Detection): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("trip_id", detection.tripId)
            put("latitude", detection.latitude)
            put("longitude", detection.longitude)
            put("timestamp", detection.timestamp)
            put("property_type", detection.propertyType)
            put("image_path", detection.imagePath)
            put("confidence", detection.confidence)
        }
        return db.insert("detections", null, values)
    }

    fun getDetectionsForTrip(tripId: String): List<Detection> {
        val db = readableDatabase
        val detections = mutableListOf<Detection>()
        val cursor = db.query(
            "detections", null,
            "trip_id = ?", arrayOf(tripId),
            null, null, null
        )
        while (cursor.moveToNext()) {
            detections.add(
                Detection(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    tripId = cursor.getString(cursor.getColumnIndexOrThrow("trip_id")),
                    latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                    propertyType = cursor.getString(cursor.getColumnIndexOrThrow("property_type")),
                    imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path")),
                    confidence = cursor.getFloat(cursor.getColumnIndexOrThrow("confidence"))
                )
            )
        }
        cursor.close()
        return detections
    }
    fun deleteTrip(tripId: String) {

        writableDatabase.delete(
            "detections",
            "trip_id = ?",
            arrayOf(tripId)
        )

        writableDatabase.delete(
            "trips",
            "trip_id = ?",
            arrayOf(tripId)
        )
    }
    fun getAllTrips(): List<Trip> {
        val db = readableDatabase
        val trips = mutableListOf<Trip>()

        val cursor = db.query(
            "trips",
            null,
            null,
            null,
            null,
            null,
            "start_time DESC"
        )

        while (cursor.moveToNext()) {
            trips.add(
                Trip(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    tripId = cursor.getString(cursor.getColumnIndexOrThrow("trip_id")),
                    startTime = cursor.getLong(cursor.getColumnIndexOrThrow("start_time")),
                    endTime = cursor.getLong(cursor.getColumnIndexOrThrow("end_time")),
                    distance = cursor.getFloat(cursor.getColumnIndexOrThrow("distance")),
                    duration = cursor.getLong(cursor.getColumnIndexOrThrow("duration"))
                )
            )
        }

        cursor.close()
        return trips
    }
    fun getTrip(tripId: String): Trip? {
        val db = readableDatabase
        val cursor = db.query(
            "trips", null,
            "trip_id = ?", arrayOf(tripId),
            null, null, null
        )
        return if (cursor.moveToFirst()) {
            Trip(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                tripId = cursor.getString(cursor.getColumnIndexOrThrow("trip_id")),
                startTime = cursor.getLong(cursor.getColumnIndexOrThrow("start_time")),
                endTime = cursor.getLong(cursor.getColumnIndexOrThrow("end_time")),
                distance = cursor.getFloat(cursor.getColumnIndexOrThrow("distance")),
                duration = cursor.getLong(cursor.getColumnIndexOrThrow("duration"))
            ).also { cursor.close() }
        } else {
            cursor.close()
            null
        }
    }
}