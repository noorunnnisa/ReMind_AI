package com.example.remind_ai.Stage3

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.remind_ai.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CaregiverFallDetectionActivity : AppCompatActivity() {

    // XML views
    private lateinit var btnBack: ImageView
    private lateinit var btnProfile: ImageButton
    private lateinit var tvStatus: TextView
    private lateinit var tvLastChecked: TextView
    private lateinit var tvActiveAlertTitle: TextView
    private lateinit var tvActiveAlertMessage: TextView
    private lateinit var btnMarkHandled: MaterialButton
    private lateinit var switchFallDetection: Switch
    private lateinit var eventsContainer: LinearLayout
    private lateinit var btnTestDetection: MaterialButton

    // Firebase Realtime Database root reference
    private val dbRef = FirebaseDatabase.getInstance().reference

    // Demo IDs for now. Later replace these with actual linked patient/caregiver IDs.
    private val caregiverId = "caregiver_demo"
    private val patientId = "patient_demo"

    // Controls whether caregiver wants to receive fall/emergency alerts
    private var receiveAlerts = true

    // Stores currently active alert ID so caregiver can mark it handled
    private var currentActiveAlertId: String? = null

    // Firebase listener reference so we can remove it when screen closes
    private var alertsListener: ValueEventListener? = null

    // Prevents switch listener from firing before saved settings are loaded
    private var isSwitchInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make sure XML file name matches this layout name
        setContentView(R.layout.activity_falldetection_c03)

        // Connect Kotlin variables with XML IDs
        btnBack = findViewById(R.id.btnBack)
        btnProfile = findViewById(R.id.btnProfile)
        tvStatus = findViewById(R.id.tvStatus)
        tvLastChecked = findViewById(R.id.tvLastChecked)
        tvActiveAlertTitle = findViewById(R.id.tvActiveAlertTitle)
        tvActiveAlertMessage = findViewById(R.id.tvActiveAlertMessage)
        btnMarkHandled = findViewById(R.id.btnMarkHandled)
        switchFallDetection = findViewById(R.id.switchFallDetection)
        eventsContainer = findViewById(R.id.eventsContainer)
        btnTestDetection = findViewById(R.id.btnTestDetection)

        // Back button closes this screen
        btnBack.setOnClickListener {
            finish()
        }

        // Placeholder profile click
        btnProfile.setOnClickListener {
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
        }

        // Main active alert button. It handles the latest active alert.
        btnMarkHandled.setOnClickListener {
            val alertId = currentActiveAlertId
            if (alertId != null) {
                markAlertAsHandled(alertId)
            }
        }

        // Demo button creates a fake fall alert in Firebase
        btnTestDetection.setOnClickListener {
            createSimulatedFallAlert()
        }

        // Toggle alert receiving on/off
        switchFallDetection.setOnCheckedChangeListener { _, isChecked ->
            // Ignore automatic switch changes before Firebase settings load
            if (!isSwitchInitialized) return@setOnCheckedChangeListener

            receiveAlerts = isChecked
            saveCaregiverAlertSettings()

            if (isChecked) {
                setConnectedState()
                listenForPatientAlerts()
            } else {
                stopListeningForAlerts()
                setAlertsOffState()
            }
        }

        // Set default UI, then load saved caregiver settings from Firebase
        setConnectedState()
        loadCaregiverSettings()
    }

    private fun loadCaregiverSettings() {
        // Load caregiver alert preference from Firebase
        dbRef.child("caregiver_settings")
            .child(caregiverId)
            .child("fall_alerts")
            .get()
            .addOnSuccessListener { snapshot ->
                val savedReceiveAlerts = snapshot.child("receiveFallAlerts").getValue(Boolean::class.java)

                // If setting does not exist, default is true
                receiveAlerts = savedReceiveAlerts ?: true

                // This updates the switch UI
                switchFallDetection.isChecked = receiveAlerts

                // Now switch listener can be allowed to work
                isSwitchInitialized = true

                if (receiveAlerts) {
                    listenForPatientAlerts()
                } else {
                    setAlertsOffState()
                }
            }
            .addOnFailureListener {
                // If settings fail to load, keep alerts ON by default
                isSwitchInitialized = true
                receiveAlerts = true
                switchFallDetection.isChecked = true
                listenForPatientAlerts()
            }
    }

    private fun listenForPatientAlerts() {
        if (!receiveAlerts) return

        // Remove old listener first to avoid duplicate listeners
        stopListeningForAlerts()

        alertsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                eventsContainer.removeAllViews()

                // If no alerts exist for caregiver, show normal connected state
                if (!snapshot.exists()) {
                    setConnectedState()
                    showEmptyEvent()
                    return
                }

                // Convert Firebase snapshot children into a sorted list, newest first
                val alerts = snapshot.children.toList().sortedByDescending {
                    it.child("timestamp").getValue(Long::class.java) ?: 0L
                }

                // Find newest active alert
                val activeAlert = alerts.firstOrNull {
                    val status = it.child("status").getValue(String::class.java) ?: "active"
                    status == "active"
                }

                // Show active alert separately at the top card
                if (activeAlert != null) {
                    showActiveAlert(activeAlert)
                } else {
                    setConnectedState()
                }

                // Show all recent alerts in the list
                for (alert in alerts) {
                    val id = alert.child("id").getValue(String::class.java) ?: alert.key.orEmpty()
                    val alertType = alert.child("alertType").getValue(String::class.java) ?: "alert"
                    val message = alert.child("message").getValue(String::class.java) ?: "Patient alert"
                    val status = alert.child("status").getValue(String::class.java) ?: "active"
                    val timestamp = alert.child("timestamp").getValue(Long::class.java) ?: 0L

                    val title = buildAlertTitle(alertType, timestamp)
                    val detail = "$message\nStatus: $status"

                    addEventCard(title, detail, id, status)
                }

                tvLastChecked.text = "Last checked: ${formatDateTime(System.currentTimeMillis())}"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@CaregiverFallDetectionActivity,
                    "Failed to load alerts: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Listen only alerts assigned to this caregiver
        dbRef.child("patient_alerts")
            .orderByChild("caregiverId")
            .equalTo(caregiverId)
            .addValueEventListener(alertsListener!!)
    }

    private fun showActiveAlert(alert: DataSnapshot) {
        // Extract alert data from Firebase snapshot
        val id = alert.child("id").getValue(String::class.java) ?: alert.key.orEmpty()
        val alertType = alert.child("alertType").getValue(String::class.java) ?: "alert"
        val message = alert.child("message").getValue(String::class.java) ?: "Patient needs help."
        val timestamp = alert.child("timestamp").getValue(Long::class.java) ?: 0L

        currentActiveAlertId = id

        // Update main status card
        tvStatus.text = "Alert Active"
        tvStatus.setTextColor(Color.parseColor("#AD1457"))

        // Update active alert card title based on alert type
        tvActiveAlertTitle.text = when (alertType) {
            "fall_detected" -> "Fall Detected"
            "manual_emergency" -> "Emergency Help"
            "simulated_fall" -> "Test Fall Alert"
            else -> "Patient Alert"
        }

        tvActiveAlertMessage.text = "$message\nTime: ${formatDateTime(timestamp)}"

        // Show handled button only when there is active alert
        btnMarkHandled.visibility = View.VISIBLE
    }

    private fun setConnectedState() {
        // Normal state when there is no active alert
        currentActiveAlertId = null

        tvStatus.text = "Connected"
        tvStatus.setTextColor(Color.parseColor("#64AA78"))
        tvLastChecked.text = "Waiting for patient alerts"

        tvActiveAlertTitle.text = "No Active Alert"
        tvActiveAlertMessage.text = "No emergency or fall alert is active right now."
        btnMarkHandled.visibility = View.GONE
    }

    private fun setAlertsOffState() {
        // State when caregiver disables receiving alerts
        currentActiveAlertId = null

        tvStatus.text = "Alerts Off"
        tvStatus.setTextColor(Color.parseColor("#6E7E8C"))
        tvLastChecked.text = "Fall alerts are turned off"

        tvActiveAlertTitle.text = "Alerts Disabled"
        tvActiveAlertMessage.text = "Turn on Receive Fall Alerts to monitor patient emergency alerts."
        btnMarkHandled.visibility = View.GONE

        eventsContainer.removeAllViews()
        showEmptyEvent()
    }

    private fun showEmptyEvent() {
        // Display placeholder text if there are no alerts
        eventsContainer.removeAllViews()

        val emptyView = TextView(this).apply {
            text = "No alerts received yet."
            gravity = android.view.Gravity.CENTER
            setPadding(dpToPx(18), dpToPx(18), dpToPx(18), dpToPx(18))
            textSize = 14f
            setTextColor(Color.parseColor("#6E7E8C"))
        }

        eventsContainer.addView(emptyView)
    }

    private fun createSimulatedFallAlert() {
        // This creates a fake alert so you can test caregiver alert screen
        val now = System.currentTimeMillis()
        val alertId = dbRef.child("patient_alerts").push().key

        if (alertId == null) {
            Toast.makeText(this, "Failed to create test alert", Toast.LENGTH_SHORT).show()
            return
        }

        val alert = mapOf(
            "id" to alertId,
            "patientId" to patientId,
            "caregiverId" to caregiverId,
            "alertType" to "simulated_fall",
            "severity" to "high",
            "message" to "Simulated fall alert created for caregiver testing.",
            "timestamp" to now,
            "formattedTime" to formatDateTime(now),
            "status" to "active"
        )

        dbRef.child("patient_alerts")
            .child(alertId)
            .setValue(alert)
            .addOnSuccessListener {
                Toast.makeText(this, "Test fall alert created", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun markAlertAsHandled(alertId: String) {
        // Mark alert as handled in Firebase instead of deleting it
        val updates = mapOf<String, Any>(
            "status" to "handled",
            "handledAt" to System.currentTimeMillis()
        )

        dbRef.child("patient_alerts")
            .child(alertId)
            .updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Alert marked as handled", Toast.LENGTH_SHORT).show()
                setConnectedState()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveCaregiverAlertSettings() {
        // Save caregiver preference for receiving fall alerts
        val settings = mapOf(
            "receiveFallAlerts" to receiveAlerts,
            "updatedAt" to System.currentTimeMillis()
        )

        dbRef.child("caregiver_settings")
            .child(caregiverId)
            .child("fall_alerts")
            .setValue(settings)
    }

    private fun addEventCard(title: String, detail: String, alertId: String, status: String) {
        // Active alerts are shown in light pink, handled alerts in purple shade
        val cardColor = if (status == "active") "#FCE4EC" else "#F6F4FF"

        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(10)
            }
            radius = dpToPx(18).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.parseColor(cardColor))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(Color.parseColor("#2C3442"))
            setTypeface(null, Typeface.BOLD)
        }

        val detailView = TextView(this).apply {
            text = detail
            textSize = 13f
            setTextColor(Color.parseColor("#6E7E8C"))
            setPadding(0, dpToPx(4), 0, 0)
        }

        layout.addView(titleView)
        layout.addView(detailView)

        // Add a small clickable action for active alerts
        if (status == "active") {
            val handledButton = TextView(this).apply {
                text = "Mark as handled"
                textSize = 14f
                setTextColor(Color.parseColor("#4A3FA0"))
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dpToPx(10), 0, 0)
                setOnClickListener {
                    markAlertAsHandled(alertId)
                }
            }

            layout.addView(handledButton)
        }

        card.addView(layout)
        eventsContainer.addView(card)
    }

    private fun buildAlertTitle(alertType: String, timestamp: Long): String {
        val time = formatDateTime(timestamp)

        return when (alertType) {
            "fall_detected" -> "Fall Detected - $time"
            "manual_emergency" -> "Emergency Help - $time"
            "simulated_fall" -> "Test Fall Alert - $time"
            else -> "Patient Alert - $time"
        }
    }

    private fun formatDateTime(timestamp: Long): String {
        if (timestamp == 0L) return "Unknown time"
        return SimpleDateFormat("MMM dd - hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun stopListeningForAlerts() {
        // Remove Firebase listener to prevent memory leaks and duplicate callbacks
        alertsListener?.let {
            dbRef.child("patient_alerts").removeEventListener(it)
        }
        alertsListener = null
    }

    override fun onDestroy() {
        super.onDestroy()

        // Always remove Firebase listener when screen closes
        stopListeningForAlerts()
    }
}
