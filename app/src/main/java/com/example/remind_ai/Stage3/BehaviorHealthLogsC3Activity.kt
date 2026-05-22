package com.example.remind_ai.Stage3

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.remind_ai.R


class BehaviorHealthLogsC3Activity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var spMood: Spinner
    private lateinit var etSleep: EditText
    private lateinit var spAppetite: Spinner
    private lateinit var spBehavior: Spinner
    private lateinit var etNotes: EditText
    private lateinit var btnSaveLog: MaterialButton
    private lateinit var logsContainer: LinearLayout

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_behaviourandhealthlog_c03)

        btnBack = findViewById(R.id.btnBack)
        spMood = findViewById(R.id.spMood)
        etSleep = findViewById(R.id.etSleep)
        spAppetite = findViewById(R.id.spAppetite)
        spBehavior = findViewById(R.id.spBehavior)
        etNotes = findViewById(R.id.etNotes)
        btnSaveLog = findViewById(R.id.btnSaveLog)
        logsContainer = findViewById(R.id.logsContainer)

        db = FirebaseFirestore.getInstance()

        setupSpinners()
        loadLogsFromFirestore()

        btnBack.setOnClickListener {
            finish()
        }

        btnSaveLog.setOnClickListener {
            saveLog()
        }
    }

    private fun setupSpinners() {
        setSpinnerData(
            spMood,
            listOf("Calm", "Happy", "Confused", "Sad", "Anxious", "Aggressive", "Restless")
        )

        setSpinnerData(
            spAppetite,
            listOf("Normal", "Low", "High", "Skipped Meal", "Refused Food")
        )

        setSpinnerData(
            spBehavior,
            listOf("Normal", "Confused", "Restless", "Aggressive", "Wandering", "Sleepy", "Unresponsive")
        )
    }

    private fun setSpinnerData(spinner: Spinner, items: List<String>) {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            items
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun saveLog() {
        val mood = spMood.selectedItem.toString()
        val sleep = etSleep.text.toString().trim()
        val appetite = spAppetite.selectedItem.toString()
        val behavior = spBehavior.selectedItem.toString()
        val notes = etNotes.text.toString().trim()

        if (sleep.isEmpty()) {
            etSleep.error = "Enter sleep hours"
            return
        }

        val log = hashMapOf(
            "patientName" to "Ahmed Ali",
            "stage" to "Stage 3",
            "mood" to mood,
            "sleep" to sleep,
            "appetite" to appetite,
            "behavior" to behavior,
            "notes" to notes,
            "createdAt" to Timestamp.now()
        )

        btnSaveLog.isEnabled = false

        db.collection("behavior_health_logs")
            .add(log)
            .addOnSuccessListener {
                Toast.makeText(this, "Health log saved", Toast.LENGTH_SHORT).show()

                etSleep.text.clear()
                etNotes.text.clear()
                spMood.setSelection(0)
                spAppetite.setSelection(0)
                spBehavior.setSelection(0)

                loadLogsFromFirestore()
                btnSaveLog.isEnabled = true
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save log", Toast.LENGTH_SHORT).show()
                btnSaveLog.isEnabled = true
            }
    }

    private fun loadLogsFromFirestore() {
        db.collection("behavior_health_logs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                logsContainer.removeAllViews()

                for (document in result) {
                    val mood = document.getString("mood") ?: ""
                    val sleep = document.getString("sleep") ?: ""
                    val appetite = document.getString("appetite") ?: ""
                    val behavior = document.getString("behavior") ?: ""
                    val notes = document.getString("notes") ?: ""
                    val createdAt = document.getTimestamp("createdAt")

                    val date = if (createdAt != null) {
                        SimpleDateFormat("MMM dd", Locale.getDefault()).format(createdAt.toDate())
                    } else {
                        "Today"
                    }

                    val title = "$date - $behavior"
                    val detail = "Mood: $mood, Sleep: $sleep hours, Appetite: $appetite" +
                            if (notes.isNotEmpty()) "\nNotes: $notes" else ""

                    addLogCard(title, detail)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load logs", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addLogCard(title: String, detail: String) {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(10)
            }
            radius = dpToPx(18).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.WHITE)
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
        card.addView(layout)

        logsContainer.addView(card)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
