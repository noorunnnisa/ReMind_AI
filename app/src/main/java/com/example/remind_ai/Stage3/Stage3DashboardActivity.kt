package com.example.remind_ai.Stage3

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.remind_ai.R

class Stage3DashboardActivity : AppCompatActivity() {

    private lateinit var cardBehaviorHealthLog: CardView
    private lateinit var cardDailyChecklist: CardView
    private lateinit var cardSoothingContent: CardView
    private lateinit var cardFallDetection: CardView
    private lateinit var cardEmergencyAlert: CardView
    private lateinit var cardPatientMonitor: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caregiver_stage_03)

        cardBehaviorHealthLog = findViewById(R.id.cardBehaviorHealthLog)
        cardDailyChecklist = findViewById(R.id.cardDailyChecklist)
        cardSoothingContent = findViewById(R.id.cardSoothingContent)
        cardFallDetection = findViewById(R.id.cardFallDetection)
        cardEmergencyAlert = findViewById(R.id.cardEmergencyAlert)
        cardPatientMonitor = findViewById(R.id.cardPatientMonitor)

        cardBehaviorHealthLog.setOnClickListener {
            startActivity(Intent(this, BehaviorHealthLogsC3Activity::class.java))
        }

        cardDailyChecklist.setOnClickListener {
            startActivity(Intent(this, DailyChecklistC3Activity::class.java))
        }

        cardSoothingContent.setOnClickListener {
            startActivity(Intent(this, SoothingContentC3Activity::class.java))
        }

        cardFallDetection.setOnClickListener {
            startActivity(Intent(this, CaregiverFallDetectionActivity::class.java))
        }

        cardEmergencyAlert.setOnClickListener {
            Toast.makeText(this, "Emergency Alert feature coming next", Toast.LENGTH_SHORT).show()
        }

        cardPatientMonitor.setOnClickListener {
            Toast.makeText(this, "Patient Monitor feature coming next", Toast.LENGTH_SHORT).show()
        }
    }
}
