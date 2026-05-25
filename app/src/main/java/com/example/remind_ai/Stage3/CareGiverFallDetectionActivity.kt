package com.example.remind_ai.Stage3

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.remind_ai.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class CaregiverFallDetectionActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvLastChecked: TextView
    private lateinit var tvActiveAlertTitle: TextView
    private lateinit var tvActiveAlertMessage: TextView
    private lateinit var btnMarkHandled: MaterialButton
    private lateinit var switchFallDetection: Switch
    private lateinit var eventsContainer: LinearLayout
    private lateinit var btnTestDetection: MaterialButton

    private val dbRef = FirebaseDatabase.getInstance().reference

    private val caregiverId = "caregiver_demo"

    private var receiveAlerts = true
    private var currentActiveAlertId: String? = null
    private var alertsListener: ValueEventListener? = null
    private var isSwitchInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_falldetection_c03)

        tvStatus = findViewById(R.id.tvStatus)
        tvLastChecked = findViewById(R.id.tvLastChecked)
        tvActiveAlertTitle = findViewById(R.id.tvActiveAlertTitle)
        tvActiveAlertMessage = findViewById(R.id.tvActiveAlertMessage)
        btnMarkHandled = findViewById(R.id.btnMarkHandled)
        switchFallDetection = findViewById(R.id.switchFallDetection)
        eventsContainer = findViewById(R.id.eventsContainer)
        btnTestDetection = findViewById(R.id.btnTestDetection)

        btnMarkHandled.setOnClickListener {
            currentActiveAlertId?.let { id ->
                markAlertAsHandled(id)
            }
        }

        btnTestDetection.setOnClickListener {
            createSimulatedFallAlert()
        }

        switchFallDetection.setOnCheckedChangeListener { _, isChecked ->
            if (!isSwitchInitialized) return@setOnCheckedChangeListener

            receiveAlerts = isChecked

            if (isChecked) {
                listenForPatientAlerts()
            } else {
                stopListeningForAlerts()
                setAlertsOffState()
            }
        }

        loadCaregiverSettings()
    }

    // ---------------- LOAD SETTINGS ----------------

    private fun loadCaregiverSettings() {

        dbRef.child("caregiver_settings")
            .child(caregiverId)
            .child("fall_alerts")
            .get()
            .addOnSuccessListener { snapshot ->

                receiveAlerts =
                    snapshot.child("receiveFallAlerts").getValue(Boolean::class.java) ?: true

                switchFallDetection.isChecked = receiveAlerts
                isSwitchInitialized = true

                if (receiveAlerts) listenForPatientAlerts()

            }.addOnFailureListener {
                receiveAlerts = true
                isSwitchInitialized = true
                switchFallDetection.isChecked = true
                listenForPatientAlerts()
            }
    }

    // ---------------- LISTENER ----------------

    private fun listenForPatientAlerts() {

        stopListeningForAlerts()

        alertsListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                eventsContainer.removeAllViews()

                if (!snapshot.exists()) {
                    setConnectedState()
                    showEmptyEvent()
                    return
                }

                val alerts = snapshot.children.sortedByDescending {
                    it.child("timestamp").getValue(Long::class.java) ?: 0L
                }

                val activeAlert = alerts.firstOrNull {
                    it.child("status").value == "active"
                }

                if (activeAlert != null) {
                    showActiveAlert(activeAlert)
                } else {
                    setConnectedState()
                }

                for (alert in alerts) {

                    val id = alert.child("id").value?.toString()
                        ?: alert.key.orEmpty()

                    val type = alert.child("alertType").value?.toString() ?: "alert"
                    val msg = alert.child("message").value?.toString() ?: ""
                    val status = alert.child("status").value?.toString() ?: "active"
                    val time = alert.child("timestamp").getValue(Long::class.java) ?: 0L

                    addEventCard(
                        buildTitle(type, time),
                        "$msg\nStatus: $status",
                        id,
                        status
                    )
                }

                tvLastChecked.text =
                    "Last checked: ${formatTime(System.currentTimeMillis())}"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CaregiverFallDetectionActivity,
                    error.message, Toast.LENGTH_LONG).show()
            }
        }

        dbRef.child("patient_alerts")
            .addValueEventListener(alertsListener!!)
    }

    // ---------------- ACTIVE ALERT ----------------

    private fun showActiveAlert(alert: DataSnapshot) {

        val id = alert.child("id").value?.toString() ?: ""
        val type = alert.child("alertType").value?.toString() ?: "alert"
        val msg = alert.child("message").value?.toString() ?: ""
        val time = alert.child("timestamp").getValue(Long::class.java) ?: 0L

        currentActiveAlertId = id

        tvStatus.text = "Alert Active"
        tvStatus.setTextColor(Color.RED)

        tvActiveAlertTitle.text = buildTitle(type, time)
        tvActiveAlertMessage.text = msg

        btnMarkHandled.visibility = View.VISIBLE
    }

    // ---------------- STATES ----------------

    private fun setConnectedState() {
        currentActiveAlertId = null

        tvStatus.text = "Connected"
        tvStatus.setTextColor(Color.GREEN)

        tvActiveAlertTitle.text = "No Active Alert"
        tvActiveAlertMessage.text = "System is monitoring patient"

        btnMarkHandled.visibility = View.GONE
    }

    private fun setAlertsOffState() {
        currentActiveAlertId = null

        tvStatus.text = "Alerts OFF"
        tvStatus.setTextColor(Color.GRAY)

        eventsContainer.removeAllViews()
        showEmptyEvent()
    }

    private fun showEmptyEvent() {
        eventsContainer.removeAllViews()

        val tv = TextView(this)
        tv.text = "No alerts yet"
        tv.gravity = android.view.Gravity.CENTER
        tv.setPadding(20, 20, 20, 20)

        eventsContainer.addView(tv)
    }

    // ---------------- CARDS ----------------

    private fun addEventCard(title: String, detail: String, id: String, status: String) {

        val card = MaterialCardView(this)
        card.setCardBackgroundColor(
            if (status == "active") Color.parseColor("#FCE4EC")
            else Color.parseColor("#F3E5F5")
        )

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(20, 20, 20, 20)

        val t1 = TextView(this)
        t1.text = title
        t1.setTypeface(null, Typeface.BOLD)

        val t2 = TextView(this)
        t2.text = detail

        layout.addView(t1)
        layout.addView(t2)

        card.addView(layout)
        eventsContainer.addView(card)
    }

    // ---------------- ACTIONS ----------------

    private fun markAlertAsHandled(alertId: String) {

        dbRef.child("patient_alerts")
            .child(alertId)
            .child("status")
            .setValue("handled")

        Toast.makeText(this, "Marked handled", Toast.LENGTH_SHORT).show()
    }

    private fun createSimulatedFallAlert() {

        val id = dbRef.child("patient_alerts").push().key ?: return

        val data = mapOf(
            "id" to id,
            "caregiverId" to caregiverId,
            "alertType" to "simulated_fall",
            "message" to "Test alert",
            "status" to "active",
            "timestamp" to System.currentTimeMillis()
        )

        dbRef.child("patient_alerts")
            .child(id)
            .setValue(data)
    }

    // ---------------- HELPERS ----------------

    private fun buildTitle(type: String, time: Long): String {
        return "$type - ${formatTime(time)}"
    }

    private fun formatTime(time: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault())
            .format(Date(time))
    }

    private fun stopListeningForAlerts() {
        alertsListener?.let {
            dbRef.child("patient_alerts").removeEventListener(it)
        }
        alertsListener = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListeningForAlerts()
    }
}