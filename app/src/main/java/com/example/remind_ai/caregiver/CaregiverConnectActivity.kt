package com.example.remind_ai.caregiver

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.remind_ai.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CaregiverConnectActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var btnBack: ImageButton
    private lateinit var etPatientCode: TextInputEditText
    private lateinit var btnConnectPatient: MaterialButton
    private lateinit var btnSkipForNow: MaterialButton

    private val caregiverId: String?
        get() = auth.currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caregiverconnect)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        btnBack = findViewById(R.id.btnBack)
        etPatientCode = findViewById(R.id.etPatientCode)
        btnConnectPatient = findViewById(R.id.btnConnectPatient)
        btnSkipForNow = findViewById(R.id.btnSkipForNow)

        btnBack.setOnClickListener {
            finish()
        }

        btnConnectPatient.setOnClickListener {
            connectPatientByCode()
        }

        btnSkipForNow.setOnClickListener {
            startActivity(Intent(this, CaregiverHomeActivity::class.java))
            finish()
        }
    }

    private fun connectPatientByCode() {
        val code = etPatientCode.text.toString().trim().uppercase()
        val currentCaregiverId = caregiverId

        if (code.isEmpty()) {
            Toast.makeText(this, "Please enter patient code", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentCaregiverId == null) {
            Toast.makeText(this, "Caregiver not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        btnConnectPatient.isEnabled = false
        btnConnectPatient.text = "Connecting..."

        firestore.collection("patients")
            .whereEqualTo("connectCode", code)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    btnConnectPatient.isEnabled = true
                    btnConnectPatient.text = "Connect Patient"
                    Toast.makeText(this, "Invalid patient code", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val patientDoc = result.documents.first()
                val patientId = patientDoc.id

                val patientName =
                    patientDoc.getString("fullName")
                        ?: patientDoc.getString("name")
                        ?: "Unknown Patient"

                val stageValue = patientDoc.get("stage")
                val stage = when (stageValue) {
                    is Number -> "Stage ${stageValue.toInt()}"
                    is String -> stageValue
                    else -> "Stage 3"
                }

                val now = System.currentTimeMillis()

                val caregiverPatientData = hashMapOf(
                    "patientId" to patientId,
                    "patientName" to patientName,
                    "stage" to stage,
                    "linkedAt" to now
                )

                val patientCaregiverData = hashMapOf(
                    "caregiverId" to currentCaregiverId,
                    "linkedAt" to now
                )

                val batch = firestore.batch()

                val caregiverPatientRef = firestore.collection("caregivers")
                    .document(currentCaregiverId)
                    .collection("linkedPatients")
                    .document(patientId)

                val patientCaregiverRef = firestore.collection("patients")
                    .document(patientId)
                    .collection("linkedCaregivers")
                    .document(currentCaregiverId)

                val patientRef = firestore.collection("patients")
                    .document(patientId)

                batch.set(caregiverPatientRef, caregiverPatientData)
                batch.set(patientCaregiverRef, patientCaregiverData)
                batch.update(patientRef, mapOf("linkedCaregiverId" to currentCaregiverId))

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Patient connected successfully", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, CaregiverHomeActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        btnConnectPatient.isEnabled = true
                        btnConnectPatient.text = "Connect Patient"
                        Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                btnConnectPatient.isEnabled = true
                btnConnectPatient.text = "Connect Patient"
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}