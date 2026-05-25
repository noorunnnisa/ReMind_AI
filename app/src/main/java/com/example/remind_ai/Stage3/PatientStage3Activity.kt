package com.example.remind_ai.Stage3

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.Uri
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.remind_ai.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class PatientStage3Activity : AppCompatActivity(), SensorEventListener {

    private lateinit var btnEmergencyHelp: MaterialButton
    private lateinit var btnProfile: ImageButton
    private lateinit var navCaregiver: LinearLayout
    private lateinit var soothingContentLayout: LinearLayout
    private lateinit var tvStatus: TextView

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private var mediaPlayer: MediaPlayer? = null

    private val patientId: String?
        get() = auth.currentUser?.uid

    private var isMonitoring = false
    private var possibleFreeFall = false
    private var impactDetected = false
    private var impactTime = 0L
    private var stillnessStartTime = 0L
    private var previousAcceleration = 9.8

    private val freeFallThreshold = 3.0
    private val impactThreshold = 25.0
    private val inactivityDurationMs = 8000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stage_03)

        btnEmergencyHelp = findViewById(R.id.btnEmergencyHelp)
        btnProfile = findViewById(R.id.btnProfile)
        navCaregiver = findViewById(R.id.navCaregiver)
        soothingContentLayout = findViewById(R.id.soothingContentLayout)
        tvStatus = findViewById(R.id.tvEmergencyStatus)

        createOrUpdatePatientRecord()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer != null) startFallMonitoring()

        loadSoothingContent()

        btnEmergencyHelp.setOnClickListener {
            sendEmergencyAlert("manual", "Emergency pressed", 0.0)
        }

        navCaregiver.setOnClickListener {
            generateConnectCode()
        }
    }

    private fun loadSoothingContent() {
        realtimeDb.child("soothing_content")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    soothingContentLayout.removeAllViews()

                    if (!snapshot.exists()) {
                        tvStatus.text = "No soothing content available"
                        return
                    }

                    tvStatus.text = "Safety monitoring is active"

                    for (child in snapshot.children) {
                        val title = child.child("title").value?.toString() ?: "No Title"
                        val type = child.child("type").value?.toString() ?: ""
                        val desc = child.child("description").value?.toString() ?: ""
                        val url = child.child("fileUrl").value?.toString() ?: ""

                        val card = MaterialCardView(this@PatientStage3Activity).apply {
                            radius = 24f
                            cardElevation = 6f
                            setCardBackgroundColor(Color.WHITE)

                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 0, 0, 20)
                            }
                        }

                        val row = LinearLayout(this@PatientStage3Activity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            setPadding(28, 28, 28, 28)
                        }

                        val icon = ImageView(this@PatientStage3Activity).apply {
                            layoutParams = LinearLayout.LayoutParams(70, 70).apply {
                                setMargins(0, 0, 22, 0)
                            }

                            setImageResource(
                                when (type) {
                                    "audio", "quran", "dua_audio" -> android.R.drawable.ic_media_play
                                    "image" -> android.R.drawable.ic_menu_gallery
                                    "video" -> android.R.drawable.ic_media_ff
                                    else -> android.R.drawable.ic_menu_info_details
                                }
                            )

                            setColorFilter(Color.parseColor("#4A3FA0"))
                        }

                        val textBox = LinearLayout(this@PatientStage3Activity).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            )
                        }

                        val titleText = TextView(this@PatientStage3Activity).apply {
                            text = title
                            textSize = 17f
                            setTextColor(Color.parseColor("#1F2937"))
                            setTypeface(null, Typeface.BOLD)
                        }

                        val descText = TextView(this@PatientStage3Activity).apply {
                            text = desc
                            textSize = 14f
                            setTextColor(Color.parseColor("#4B5563"))
                        }

                        textBox.addView(titleText)
                        textBox.addView(descText)

                        row.addView(icon)
                        row.addView(textBox)

                        card.addView(row)

                        card.setOnClickListener {
                            when (type) {
                                "audio", "quran", "dua_audio" -> {
                                    if (url.isNotEmpty()) playAudio(url)
                                    else Toast.makeText(
                                        this@PatientStage3Activity,
                                        desc.ifEmpty { title },
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                "image", "video" -> {
                                    if (url.isNotEmpty()) openMedia(url)
                                    else Toast.makeText(
                                        this@PatientStage3Activity,
                                        "No media found",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                "dua", "text" -> {
                                    Toast.makeText(
                                        this@PatientStage3Activity,
                                        desc.ifEmpty { title },
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }

                        soothingContentLayout.addView(card)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    tvStatus.text = "Failed to load content"
                }
            })
    }

    private fun playAudio(url: String) {
        stopAudio()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener { it.start() }
            prepareAsync()
        }
    }

    private fun stopAudio() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun openMedia(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun createOrUpdatePatientRecord() {
        val id = patientId ?: return

        val data = hashMapOf(
            "patientId" to id,
            "stage" to 3,
            "status" to "Stable"
        )

        firestore.collection("patients")
            .document(id)
            .set(data, SetOptions.merge())
    }

    private fun generateConnectCode() {
        val code = "RM${Random.nextInt(100000, 999999)}"

        AlertDialog.Builder(this)
            .setTitle("Caregiver Code")
            .setMessage(code)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun startFallMonitoring() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        isMonitoring = true
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isMonitoring || event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acc = sqrt((x * x + y * y + z * z).toDouble())
        val move = abs(acc - previousAcceleration)

        detectFall(acc, move)

        previousAcceleration = acc
    }

    private fun detectFall(acc: Double, move: Double) {
        val now = System.currentTimeMillis()

        if (acc < freeFallThreshold) possibleFreeFall = true

        if (possibleFreeFall && acc > impactThreshold) {
            impactDetected = true
            impactTime = now
            stillnessStartTime = now
        }

        if (impactDetected && move < 0.6) {
            if (now - stillnessStartTime > inactivityDurationMs) {
                sendEmergencyAlert("fall", "Possible fall detected", acc)
                reset()
            }
        }
    }

    private fun reset() {
        possibleFreeFall = false
        impactDetected = false
        stillnessStartTime = 0L
    }

    private fun sendEmergencyAlert(type: String, msg: String, acc: Double) {
        val id = patientId ?: return

        val data = hashMapOf(
            "patientId" to id,
            "type" to type,
            "message" to msg,
            "acc" to acc
        )

        firestore.collection("alerts").add(data)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
    }
}