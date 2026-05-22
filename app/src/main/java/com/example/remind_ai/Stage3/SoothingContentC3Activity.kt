package com.example.remind_ai.Stage3

import android.app.AlertDialog
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.remind_ai.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.example.remind_ai.model.SoothingContent


class SoothingContentC3Activity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvNowPlayingTitle: TextView
    private lateinit var tvNowPlayingSubtitle: TextView
    private lateinit var tvPlayerStatus: TextView
    private lateinit var tvDuration: TextView
    private lateinit var btnPlayNow: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var btnAddContent: MaterialButton
    private lateinit var btnPlaySurah: MaterialButton
    private lateinit var spSurah: Spinner

    private lateinit var cardCalmMusic: MaterialCardView
    private lateinit var cardFamilyPhotos: MaterialCardView
    private lateinit var cardGentleVideo: MaterialCardView
    private lateinit var cardFavoriteDua: MaterialCardView

    private val dbRef = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance()

    private var mediaPlayer: MediaPlayer? = null

    private var selectedTitle = ""
    private var selectedType = ""
    private var selectedDescription = ""
    private var selectedDuration = ""
    private var selectedUrl = ""

    private var uploadType = "audio"
    private var selectedFileName = ""

    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadContentToFirebaseStorage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soothingcontent_c03)

        btnBack = findViewById(R.id.btnBack)
        tvNowPlayingTitle = findViewById(R.id.tvNowPlayingTitle)
        tvNowPlayingSubtitle = findViewById(R.id.tvNowPlayingSubtitle)
        tvPlayerStatus = findViewById(R.id.tvPlayerStatus)
        tvDuration = findViewById(R.id.tvDuration)
        btnPlayNow = findViewById(R.id.btnPlayNow)
        btnStop = findViewById(R.id.btnStop)
        btnAddContent = findViewById(R.id.btnAddContent)
        btnPlaySurah = findViewById(R.id.btnPlaySurah)
        spSurah = findViewById(R.id.spSurah)

        cardCalmMusic = findViewById(R.id.cardCalmMusic)
        cardFamilyPhotos = findViewById(R.id.cardFamilyPhotos)
        cardGentleVideo = findViewById(R.id.cardGentleVideo)
        cardFavoriteDua = findViewById(R.id.cardFavoriteDua)

        setupSurahSpinner()
        createDefaultContent()
        loadContentFromRealtimeDatabase("favorite_dua")

        btnBack.setOnClickListener {
            finish()
        }

        cardCalmMusic.setOnClickListener {
            loadContentFromRealtimeDatabase("calm_music")
        }

        cardFamilyPhotos.setOnClickListener {
            loadContentFromRealtimeDatabase("family_photos")
        }

        cardGentleVideo.setOnClickListener {
            loadContentFromRealtimeDatabase("gentle_video")
        }

        cardFavoriteDua.setOnClickListener {
            loadContentFromRealtimeDatabase("favorite_dua")
        }

        btnPlayNow.setOnClickListener {
            playSelectedContent()
        }

        btnStop.setOnClickListener {
            stopContent()
        }

        btnAddContent.setOnClickListener {
            showUploadOptions()
        }

        btnPlaySurah.setOnClickListener {
            playSelectedSurah()
        }
    }

    private fun setupSurahSpinner() {
        val surahs = (1..114).map { "Surah $it" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, surahs)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spSurah.adapter = adapter
    }

    private fun createDefaultContent() {
        val defaultContent = listOf(
            SoothingContent(
                id = "calm_music",
                patientId = "patient_demo",
                caregiverId = "caregiver_demo",
                title = "Calm Music",
                type = "audio",
                description = "Soft sounds for relaxation",
                fileUrl = "",
                fileName = "",
                uploadedAt = System.currentTimeMillis()
            ),
            SoothingContent(
                id = "family_photos",
                patientId = "patient_demo",
                caregiverId = "caregiver_demo",
                title = "Family Photos",
                type = "image",
                description = "Familiar faces and memories",
                fileUrl = "",
                fileName = "",
                uploadedAt = System.currentTimeMillis()
            ),
            SoothingContent(
                id = "gentle_video",
                patientId = "patient_demo",
                caregiverId = "caregiver_demo",
                title = "Gentle Video",
                type = "video",
                description = "Peaceful visuals for comfort",
                fileUrl = "",
                fileName = "",
                uploadedAt = System.currentTimeMillis()
            ),
            SoothingContent(
                id = "favorite_dua",
                patientId = "patient_demo",
                caregiverId = "caregiver_demo",
                title = "Favorite Dua",
                type = "audio",
                description = "Spiritual comfort content",
                fileUrl = "",
                fileName = "",
                uploadedAt = System.currentTimeMillis()
            )
        )

        for (content in defaultContent) {
            dbRef.child("soothing_content")
                .child(content.id)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        dbRef.child("soothing_content")
                            .child(content.id)
                            .setValue(content)
                    }
                }
        }
    }

    private fun loadContentFromRealtimeDatabase(contentId: String) {
        dbRef.child("soothing_content")
            .child(contentId)
            .get()
            .addOnSuccessListener { snapshot ->
                val content = snapshot.getValue(SoothingContent::class.java)

                if (content == null) {
                    Toast.makeText(this, "Content not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                selectedTitle = content.title
                selectedType = content.type
                selectedDescription = content.description
                selectedDuration = when (content.type) {
                    "image" -> "Photo"
                    "video" -> "Video"
                    "audio" -> "Audio"
                    else -> "Content"
                }
                selectedUrl = content.fileUrl

                tvNowPlayingTitle.text = selectedTitle
                tvNowPlayingSubtitle.text = selectedDescription
                tvDuration.text = selectedDuration
                tvPlayerStatus.text = "Ready to play"
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showUploadOptions() {
        val options = arrayOf("Upload Audio", "Upload Photo", "Upload Video")

        AlertDialog.Builder(this)
            .setTitle("Add Comfort Content")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        uploadType = "audio"
                        filePicker.launch("audio/*")
                    }
                    1 -> {
                        uploadType = "image"
                        filePicker.launch("image/*")
                    }
                    2 -> {
                        uploadType = "video"
                        filePicker.launch("video/*")
                    }
                }
            }
            .show()
    }

    private fun uploadContentToFirebaseStorage(uri: Uri) {
        tvPlayerStatus.text = "Uploading content..."

        selectedFileName = "soothing_content/${System.currentTimeMillis()}_$uploadType"
        val storageRef = storage.reference.child(selectedFileName)

        storageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Upload failed")
                }
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                saveUploadedContentToRealtimeDatabase(downloadUri.toString())
            }
            .addOnFailureListener { e ->
                tvPlayerStatus.text = "Upload failed"
                Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveUploadedContentToRealtimeDatabase(fileUrl: String) {
        val contentId = dbRef.child("soothing_content").push().key

        if (contentId == null) {
            Toast.makeText(this, "Failed to create content ID", Toast.LENGTH_SHORT).show()
            return
        }

        val title = when (uploadType) {
            "audio" -> "Uploaded Calm Audio"
            "image" -> "Uploaded Family Photo"
            "video" -> "Uploaded Gentle Video"
            else -> "Uploaded Content"
        }

        val content = SoothingContent(
            id = contentId,
            patientId = "patient_demo",
            caregiverId = "caregiver_demo",
            title = title,
            type = uploadType,
            description = "Caregiver uploaded comfort content",
            fileUrl = fileUrl,
            fileName = selectedFileName,
            uploadedAt = System.currentTimeMillis()
        )

        dbRef.child("soothing_content")
            .child(contentId)
            .setValue(content)
            .addOnSuccessListener {
                selectedTitle = content.title
                selectedType = content.type
                selectedDescription = content.description
                selectedDuration = when (content.type) {
                    "image" -> "Photo"
                    "video" -> "Video"
                    "audio" -> "Audio"
                    else -> "Content"
                }
                selectedUrl = content.fileUrl

                tvNowPlayingTitle.text = selectedTitle
                tvNowPlayingSubtitle.text = selectedDescription
                tvDuration.text = selectedDuration
                tvPlayerStatus.text = "Uploaded and ready"

                Toast.makeText(this, "Content saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun playSelectedSurah() {
        val surahNumber = spSurah.selectedItemPosition + 1
        tvPlayerStatus.text = "Loading Surah $surahNumber..."

        Thread {
            try {
                val apiUrl = "https://quranapi.pages.dev/api/audio/$surahNumber.json"
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                val reciterOne = json.getJSONObject("1")
                val audioUrl = reciterOne.getString("originalUrl")

                runOnUiThread {
                    selectedTitle = "Surah $surahNumber Recitation"
                    selectedType = "quran"
                    selectedDescription = "Quran recitation"
                    selectedDuration = "Surah Audio"
                    selectedUrl = audioUrl

                    tvNowPlayingTitle.text = selectedTitle
                    tvNowPlayingSubtitle.text = selectedDescription
                    tvDuration.text = selectedDuration

                    playAudioUrl(audioUrl)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvPlayerStatus.text = "Unable to load Surah"
                    Toast.makeText(this, "Quran API failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun playSelectedContent() {
        if (selectedUrl.isEmpty()) {
            Toast.makeText(this, "No URL found. Upload content first.", Toast.LENGTH_SHORT).show()
            return
        }

        when (selectedType) {
            "audio", "quran" -> playAudioUrl(selectedUrl)
            "image", "video" -> openMediaUrl(selectedUrl)
            else -> Toast.makeText(this, "Unsupported content type", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playAudioUrl(url: String) {
        stopContent()
        tvPlayerStatus.text = "Preparing audio..."

        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener {
                it.start()
                tvPlayerStatus.text = "Playing: $selectedTitle"
            }
            setOnCompletionListener {
                tvPlayerStatus.text = "Finished"
            }
            setOnErrorListener { _, _, _ ->
                tvPlayerStatus.text = "Playback failed"
                true
            }
            prepareAsync()
        }
    }

    private fun openMediaUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        tvPlayerStatus.text = "Opened: $selectedTitle"
    }

    private fun stopContent() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }

        mediaPlayer = null
        tvPlayerStatus.text = "Stopped"
    }

    override fun onDestroy() {
        super.onDestroy()
        stopContent()
    }
}
