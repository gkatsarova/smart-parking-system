package com.planzy.smartparkingsystem.data

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.net.HttpURLConnection
import java.net.URL


data class ParkingState(
    val free: Int = 0,
    val occupied: Int = 0,
    val total: Int = 20,
    val lastUpdate: Long = 0L,
    val confidence: Int = 0,
)

sealed class CaptureResult {
    object Success : CaptureResult()
    data class Error(val message: String) : CaptureResult()
}

sealed class FirebaseResult {
    data class Data(val state: ParkingState) : FirebaseResult()
    data class Failure(val message: String) : FirebaseResult()
}


class ParkingRepository {

    private val db = FirebaseDatabase.getInstance().getReference("parking")

    fun observeParkingState(): Flow<FirebaseResult> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(FirebaseResult.Failure("No data"))
                    return
                }
                val state = ParkingState(
                    free = snapshot.child("free").getValue(Int::class.java) ?: 0,
                    occupied = snapshot.child("occupied").getValue(Int::class.java) ?: 0,
                    total = snapshot.child("total").getValue(Int::class.java) ?: 20,
                    lastUpdate = snapshot.child("lastUpdate").getValue(Long::class.java) ?: 0L,
                    confidence = snapshot.child("confidence").getValue(Int::class.java) ?: 0,
                )
                trySend(FirebaseResult.Data(state))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(FirebaseResult.Failure(error.message))
            }
        }

        db.addValueEventListener(listener)
        awaitClose { db.removeEventListener(listener) }
    }

    suspend fun triggerCapture(esp32Ip: String): CaptureResult =
        withContext(Dispatchers.IO) {
            try {
                val url  = URL("http://$esp32Ip/capture")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000

                val code = conn.responseCode
                conn.disconnect()
                Log.i("ParkingRepo", "ESP32 response: HTTP $code")

                if (code == 200) CaptureResult.Success
                else CaptureResult.Error("ESP32 response HTTP $code")

            } catch (e: Exception) {
                Log.e("ParkingRepo", "Capture error: ${e.message}")
                CaptureResult.Error(e.message ?: "unknown error")
            }
        }
}