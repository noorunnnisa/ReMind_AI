package com.example.remind_ai.Stage3

import android.app.AlertDialog
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.remind_ai.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class PatientStage3Activity : AppCompatActivity(), SensorEventListener {

    private lateinit var btnEmergencyHelp: MaterialButton
    private lateinit var tvEmergencyStatus: TextView
    private lateinit var btnProfile: ImageButton
    private lateinit var navCaregiver: LinearLayout

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val patientId: String?
        get() = auth.currentUser?.uid

    private var isMonitoring = false
    private var possibleFreeFall = false
    private var impactDetected = false
    private var impactTime = 0L
    private var stillnessStartTime = 0L
    private var previousAcceleration = 9.8
    private var lastAlertTime = 0L

    private val freeFallThreshold = 3.0
    private val impactThreshold = 25.0
    private val inactivityDurationMs = 8000L
    private val alertCooldownMs = 30000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_stage3)

        btnEmergencyHelp = findViewById(R.id.btnEmergencyHelp)
        tvEmergencyStatus = findViewById(R.id.tvEmergencyStatus)
        btnProfile = findViewById(R.id.btnProfile)

        // Your XML id is navCaregiver
        navCaregiver = findViewById(R.id.navCaregiver)

        createOrUpdatePatientRecord()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            tvEmergencyStatus.text = "Fall sensor not available. Emergency button is still active."
        } else {
            startFallMonitoring()
        }

        btnEmergencyHelp.setOnClickListener {
            sendEmergencyAlert(
                alertType = "manual_emergency",
                message = "Patient pressed Emergency Help button.",
                acceleration = 0.0
            )
        }

        navCaregiver.setOnClickListener {
            generateAndShowCaregiverConnectCode()
        }
    }

    private fun createOrUpdatePatientRecord() {
        val currentPatientId = patientId

        if (currentPatientId == null) {
            Toast.makeText(this, "Patient not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val patientData = mapOf(
            "patientId" to currentPatientId,
            "stage" to 3,
            "role" to "patient",
            "status" to "Stable",
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("patients")
            .document(currentPatientId)
            .set(patientData, SetOptions.merge())
            .addOnFailureListener { e ->
                Toast.makeText(this, "Patient setup failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun generateAndShowCaregiverConnectCode() {
        val currentPatientId = patientId

        if (currentPatientId == null) {
            Toast.makeText(this, "Patient not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val code = generateConnectCode()

        val data = mapOf(
            "patientId" to currentPatientId,
            "connectCode" to code,
            "stage" to 3,
            "role" to "patient",
            "connectCodeUpdatedAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("patients")
            .document(currentPatientId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                showConnectCodeDialog(code)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Code failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun generateConnectCode(): String {
        return "RM${Random.nextInt(100000, 999999)}"
    }

    private fun showConnectCodeDialog(code: String) {
        AlertDialog.Builder(this)
            .setTitle("Caregiver Connect Code")
            .setMessage("Share this code with your caregiver:\n\n$code")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun startFallMonitoring() {
        val sensor = accelerometer ?: return

        isMonitoring = true

        sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        tvEmergencyStatus.text =
            "Safety monitoring is active. Caregiver will be notified if help is needed."
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isMonitoring || event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt((x * x + y * y + z * z).toDouble())
        val movementChange = abs(acceleration - previousAcceleration)
        val now = System.currentTimeMillis()

        detectFall(acceleration, movementChange, now)

        previousAcceleration = acceleration
    }

    private fun detectFall(acceleration: Double, movementChange: Double, now: Long) {
        if (now - lastAlertTime < alertCooldownMs) return

        if (acceleration < freeFallThreshold) {
            possibleFreeFall = true
            tvEmergencyStatus.text = "Sudden drop detected. Checking safety..."
            return
        }

        if (possibleFreeFall && acceleration > impactThreshold) {
            impactDetected = true
            impactTime = now
            stillnessStartTime = now
            tvEmergencyStatus.text = "Strong impact detected. Monitoring movement..."
            return
        }

        if (impactDetected) {
            if (movementChange < 0.6) {
                if (stillnessStartTime == 0L) {
                    stillnessStartTime = now
                }

                val stillFor = now - stillnessStartTime

                if (stillFor >= inactivityDurationMs) {
                    sendEmergencyAlert(
                        alertType = "fall_detected",
                        message = "Possible fall detected automatically by phone movement sensor.",
                        acceleration = acceleration
                    )

                    resetFallState()
                }
            } else {
                stillnessStartTime = now
            }

            if (now - impactTime > 15000L) {
                resetFallState()
                tvEmergencyStatus.text =
                    "Safety monitoring is active. Caregiver will be notified if help is needed."
            }
        }
    }

    private fun sendEmergencyAlert(alertType: String, message: String, acceleration: Double) {
        val currentPatientId = patientId

        if (currentPatientId == null) {
            Toast.makeText(this, "Patient not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val now = System.currentTimeMillis()
        lastAlertTime = now

        btnEmergencyHelp.isEnabled = false
        tvEmergencyStatus.text = "Sending alert to caregiver..."

        val alertRef = firestore.collection("patient_alerts").document()
        val alertId = alertRef.id

        val alert = mapOf(
            "id" to alertId,
            "patientId" to currentPatientId,
            "alertType" to alertType,
            "severity" to "high",
            "message" to message,
            "acceleration" to acceleration,
            "timestamp" to now,
            "formattedTime" to formatDateTime(now),
            "status" to "active"
        )

        alertRef.set(alert)
            .addOnSuccessListener {
                btnEmergencyHelp.isEnabled = true

                if (alertType == "fall_detected") {
                    tvEmergencyStatus.text = "Possible fall detected. Caregiver has been alerted."
                    Toast.makeText(this, "Fall alert sent to caregiver", Toast.LENGTH_LONG).show()
                } else {
                    tvEmergencyStatus.text = "Emergency alert sent to caregiver."
                    Toast.makeText(this, "Emergency alert sent", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                btnEmergencyHelp.isEnabled = true
                tvEmergencyStatus.text = "Alert failed: ${e.message}"
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun resetFallState() {
        possibleFreeFall = false
        impactDetected = false
        impactTime = 0L
        stillnessStartTime = 0L
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onResume() {
        super.onResume()
        if (isMonitoring) {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isMonitoring) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    private fun formatDateTime(timestamp: Long): String {
        return SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}