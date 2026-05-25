package com.example.remind_ai.caregiver

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.remind_ai.R
import com.example.remind_ai.databinding.ActivityCaregiverconnectBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CaregiverConnectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaregiverconnectBinding

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCaregiverconnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnConnectPatient.setOnClickListener {
            val code = binding.etPatientCode.text.toString().trim()

            if (code.isEmpty()) {
                Toast.makeText(this, "Enter code", Toast.LENGTH_SHORT).show()
            } else {
                verifyCode(code)
            }
        }

        binding.btnSkipForNow.setOnClickListener {
            finish()
        }
    }

    private fun verifyCode(code: String) {

        val caregiverId = auth.currentUser?.uid ?: return

        db.collection("connection_codes")
            .document(code)
            .get()
            .addOnSuccessListener { doc ->

                if (!doc.exists()) {
                    Toast.makeText(this, "Invalid Code", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val patientId = doc.getString("patientId")

                if (patientId == null) {
                    Toast.makeText(this, "Invalid patient", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                linkPatient(caregiverId, patientId)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error verifying code", Toast.LENGTH_SHORT).show()
            }
    }

    private fun linkPatient(caregiverId: String, patientId: String) {

        val data = hashMapOf(
            "patientId" to patientId,
            "linkedAt" to System.currentTimeMillis()
        )

        db.collection("caregivers")
            .document(caregiverId)
            .collection("linkedPatients")
            .document(patientId)
            .set(data)
            .addOnSuccessListener {

                Toast.makeText(this, "Patient Connected!", Toast.LENGTH_SHORT).show()

                startActivity(
                    Intent(this, CaregiverHomeActivity::class.java)
                )
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Link failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}