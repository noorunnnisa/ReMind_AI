package com.example.remind_ai.Stage3

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.remind_ai.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class Stage3DashboardActivity : AppCompatActivity() {

    private lateinit var cardBehaviorHealthLog: CardView
    private lateinit var cardDailyChecklist: CardView
    private lateinit var cardSoothingContent: CardView
    private lateinit var cardFallDetection: CardView
    private lateinit var cardEmergencyAlert: CardView
    private lateinit var cardPatientMonitor: CardView

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var patientListener: ListenerRegistration? = null
    private var alertListener: ListenerRegistration? = null

    private lateinit var patientId: String
    private var patientName: String = "Patient"

    private val caregiverId: String
        get() = auth.currentUser?.uid ?: "caregiver_demo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caregiver_stage_03)

        patientId = intent.getStringExtra("patientId") ?: "patient_demo"
        patientName = intent.getStringExtra("patientName") ?: "Patient"

        cardBehaviorHealthLog = findViewById(R.id.cardBehaviorHealthLog)
        cardDailyChecklist = findViewById(R.id.cardDailyChecklist)
        cardSoothingContent = findViewById(R.id.cardSoothingContent)
        cardFallDetection = findViewById(R.id.cardFallDetection)
        cardEmergencyAlert = findViewById(R.id.cardEmergencyAlert)
        cardPatientMonitor = findViewById(R.id.cardPatientMonitor)

        setupCardClicks()
        listenForPatientUpdates()
        listenForEmergencyAlerts()
    }

    private fun setupCardClicks() {
        cardBehaviorHealthLog.setOnClickListener {
            openModule(BehaviorHealthLogsC3Activity::class.java)
        }

        cardDailyChecklist.setOnClickListener {
            openModule(DailyChecklistC3Activity::class.java)
        }

        cardSoothingContent.setOnClickListener {
            openModule(SoothingContentCaretakerActivity::class.java)
        }

        cardFallDetection.setOnClickListener {
            openModule(CaregiverFallDetectionActivity::class.java)
        }

        cardEmergencyAlert.setOnClickListener {
            Toast.makeText(
                this,
                "Emergency alerts are monitored in real time",
                Toast.LENGTH_SHORT
            ).show()
        }

        cardPatientMonitor.setOnClickListener {
            Toast.makeText(
                this,
                "Patient Monitor feature coming next",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openModule(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.putExtra("patientId", patientId)
        intent.putExtra("patientName", patientName)
        intent.putExtra("caregiverId", caregiverId)
        startActivity(intent)
    }

    private fun listenForPatientUpdates() {
        patientListener?.remove()

        patientListener = firestore.collection("patients")
            .document(patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        this,
                        "Patient update error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

                patientName =
                    snapshot.getString("fullName")
                        ?: snapshot.getString("name")
                                ?: patientName

                val stageValue = snapshot.get("stage")
                val stage = when (stageValue) {
                    is Number -> stageValue.toInt()
                    is String -> stageValue.filter { it.isDigit() }.toIntOrNull() ?: 3
                    else -> 3
                }

                if (stage != 3) {
                    Toast.makeText(
                        this,
                        "$patientName is now Stage $stage",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun listenForEmergencyAlerts() {
        alertListener?.remove()

        alertListener = firestore.collection("patient_alerts")
            .whereEqualTo("patientId", patientId)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                for (change in snapshots.documentChanges) {
                    val message =
                        change.document.getString("message") ?: "Emergency alert received"

                    Toast.makeText(
                        this,
                        "$patientName: $message",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        patientListener?.remove()
        alertListener?.remove()
    }
}