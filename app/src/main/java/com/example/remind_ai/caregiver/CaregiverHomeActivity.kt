package com.example.remind_ai.caregiver

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.remind_ai.R
import com.example.remind_ai.Stage3.Stage3DashboardActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class CaregiverHomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var btnProfile: ImageButton
    private lateinit var tvTotalPatients: TextView
    private lateinit var tvActiveAlerts: TextView
    private lateinit var tvStableNow: TextView
    private lateinit var emptyPatientsCard: CardView
    private lateinit var btnAddPatient: MaterialButton
    private lateinit var patientListContainer: LinearLayout

    private var linkedPatientsListener: ListenerRegistration? = null
    private val patientListeners = mutableListOf<ListenerRegistration>()
    private val alertListeners = mutableListOf<ListenerRegistration>()

    private val linkedPatientIds = mutableSetOf<String>()
    private val patientCards = mutableMapOf<String, PatientCardData>()
    private val patientAlertCounts = mutableMapOf<String, Int>()

    data class PatientCardData(
        val patientId: String,
        val patientName: String,
        val stage: String,
        val status: String,
        val lastUpdated: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caregiverhome)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        btnProfile = findViewById(R.id.btnProfile)
        tvTotalPatients = findViewById(R.id.tvTotalPatients)
        tvActiveAlerts = findViewById(R.id.tvActiveAlerts)
        tvStableNow = findViewById(R.id.tvStableNow)
        emptyPatientsCard = findViewById(R.id.emptyPatientsCard)
        btnAddPatient = findViewById(R.id.btnAddPatient)
        patientListContainer = findViewById(R.id.patientListContainer)

        btnAddPatient.setOnClickListener {
            startActivity(Intent(this, CaregiverConnectActivity::class.java))
        }

        btnProfile.setOnClickListener {
            Toast.makeText(this, "Profile screen can be added next", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        loadAssignedPatientsRealtime()
    }

    override fun onStop() {
        super.onStop()
        linkedPatientsListener?.remove()
        clearPatientRealtimeListeners()
    }

    private fun loadAssignedPatientsRealtime() {
        val caregiverUid = auth.currentUser?.uid

        if (caregiverUid == null) {
            Toast.makeText(this, "Caregiver not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        linkedPatientsListener?.remove()
        clearPatientRealtimeListeners()

        linkedPatientsListener = firestore.collection("caregivers")
            .document(caregiverUid)
            .collection("linkedPatients")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load patients: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                linkedPatientIds.clear()
                patientCards.clear()
                patientAlertCounts.clear()
                clearPatientRealtimeListeners()

                if (snapshot == null || snapshot.isEmpty) {
                    renderPatientCards()
                    return@addSnapshotListener
                }

                for (doc in snapshot.documents) {
                    val patientId = doc.getString("patientId") ?: doc.id
                    linkedPatientIds.add(patientId)
                    listenToPatient(patientId)
                    listenToPatientAlerts(patientId)
                }

                renderPatientCards()
            }
    }

    private fun listenToPatient(patientId: String) {
        val listener = firestore.collection("patients")
            .document(patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val patientName =
                    snapshot.getString("fullName")
                        ?: snapshot.getString("name")
                        ?: "Unknown Patient"

                val stageValue = snapshot.get("stage")
                val stage = when (stageValue) {
                    is Number -> "Stage ${stageValue.toInt()}"
                    is String -> stageValue
                    else -> "Stage 3"
                }

                val status = snapshot.getString("status") ?: "Stable"
                val lastUpdated = snapshot.getString("lastUpdated") ?: "Just now"

                patientCards[patientId] = PatientCardData(
                    patientId = patientId,
                    patientName = patientName,
                    stage = stage,
                    status = status,
                    lastUpdated = lastUpdated
                )

                renderPatientCards()
            }

        patientListeners.add(listener)
    }

    private fun listenToPatientAlerts(patientId: String) {
        val listener = firestore.collection("patient_alerts")
            .whereEqualTo("patientId", patientId)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                patientAlertCounts[patientId] = snapshot.size()
                renderPatientCards()
            }

        alertListeners.add(listener)
    }

    private fun renderPatientCards() {
        patientListContainer.removeAllViews()

        if (linkedPatientIds.isEmpty()) {
            tvTotalPatients.text = "0"
            tvActiveAlerts.text = "0"
            tvStableNow.text = "0"
            emptyPatientsCard.visibility = View.VISIBLE
            patientListContainer.visibility = View.GONE
            return
        }

        emptyPatientsCard.visibility = View.GONE
        patientListContainer.visibility = View.VISIBLE

        val totalPatients = linkedPatientIds.size
        val activeAlertsCount = patientAlertCounts.values.sum()
        val stableCount = linkedPatientIds.count { patientId ->
            val patient = patientCards[patientId]
            val alerts = patientAlertCounts[patientId] ?: 0
            patient?.status.equals("Stable", ignoreCase = true) && alerts == 0
        }

        tvTotalPatients.text = totalPatients.toString()
        tvActiveAlerts.text = activeAlertsCount.toString()
        tvStableNow.text = stableCount.toString()

        for (patientId in linkedPatientIds) {
            val patient = patientCards[patientId] ?: continue
            val alertCount = patientAlertCounts[patientId] ?: 0

            addPatientCard(
                patientId = patient.patientId,
                patientName = patient.patientName,
                stage = patient.stage,
                status = patient.status,
                alertCount = alertCount,
                lastUpdated = patient.lastUpdated
            )
        }
    }

    private fun clearPatientRealtimeListeners() {
        patientListeners.forEach { it.remove() }
        alertListeners.forEach { it.remove() }
        patientListeners.clear()
        alertListeners.clear()
    }

    private fun addPatientCard(
        patientId: String,
        patientName: String,
        stage: String,
        status: String,
        alertCount: Int,
        lastUpdated: String
    ) {
        val card = CardView(this).apply {
            radius = dpToPx(22f)
            cardElevation = dpToPx(3f)
            setCardBackgroundColor(Color.WHITE)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(dpToPx(20f).toInt(), dpToPx(14f).toInt(), dpToPx(20f).toInt(), 0)
            layoutParams = params
        }

        val root = RelativeLayout(this).apply {
            setPadding(dpToPx(16f).toInt(), dpToPx(16f).toInt(), dpToPx(16f).toInt(), dpToPx(16f).toInt())
        }

        if (alertCount > 0) {
            val alertBadge = TextView(this).apply {
                text = "ALERT"
                setTextColor(Color.WHITE)
                textSize = 11f
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                setBackgroundColor(Color.parseColor("#DC5858"))
                id = View.generateViewId()
            }

            val badgeParams = RelativeLayout.LayoutParams(dpToPx(64f).toInt(), dpToPx(26f).toInt())
            badgeParams.addRule(RelativeLayout.ALIGN_PARENT_END)
            alertBadge.layoutParams = badgeParams
            root.addView(alertBadge)
        }

        val patientIcon = ImageView(this).apply {
            id = View.generateViewId()
            setImageResource(R.drawable.profile)
            setBackgroundResource(R.drawable.profile_circle_bg)
            setPadding(dpToPx(12f).toInt(), dpToPx(12f).toInt(), dpToPx(12f).toInt(), dpToPx(12f).toInt())
        }

        val iconParams = RelativeLayout.LayoutParams(dpToPx(60f).toInt(), dpToPx(60f).toInt())
        if (alertCount > 0) iconParams.topMargin = dpToPx(16f).toInt()
        patientIcon.layoutParams = iconParams
        root.addView(patientIcon)

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            id = View.generateViewId()
        }

        val infoParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        infoParams.addRule(RelativeLayout.END_OF, patientIcon.id)
        infoParams.marginStart = dpToPx(14f).toInt()
        if (alertCount > 0) infoParams.topMargin = dpToPx(16f).toInt()
        infoLayout.layoutParams = infoParams

        val tvName = TextView(this).apply {
            text = patientName
            setTextColor(Color.parseColor("#2C3442"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(typeface, Typeface.BOLD)
        }

        val tvStage = TextView(this).apply {
            text = stage
            setTextColor(Color.parseColor("#6F54B5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        }

        val tvStatus = TextView(this).apply {
            text = if (alertCount > 0) "$alertCount Active Alert" else status
            setTextColor(
                if (alertCount > 0) Color.parseColor("#DC5858")
                else Color.parseColor("#64AA78")
            )
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
        }

        val tvUpdated = TextView(this).apply {
            text = "Updated: $lastUpdated"
            setTextColor(Color.parseColor("#6E7E8C"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }

        infoLayout.addView(tvName)
        infoLayout.addView(tvStage)
        infoLayout.addView(tvStatus)
        infoLayout.addView(tvUpdated)
        root.addView(infoLayout)

        val btnOpen = MaterialButton(this).apply {
            text = "Open"
            setTextColor(Color.WHITE)
            textSize = 14f
            isAllCaps = false
            cornerRadius = dpToPx(22f).toInt()
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#6F54B5"))
        }

        val buttonParams = RelativeLayout.LayoutParams(dpToPx(100f).toInt(), dpToPx(46f).toInt())
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_END)
        buttonParams.addRule(RelativeLayout.CENTER_VERTICAL)
        btnOpen.layoutParams = buttonParams

        btnOpen.setOnClickListener {
            val intent = Intent(this, Stage3DashboardActivity::class.java)
            intent.putExtra("patientId", patientId)
            intent.putExtra("patientName", patientName)
            startActivity(intent)
        }

        root.addView(btnOpen)
        card.addView(root)
        patientListContainer.addView(card)
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}