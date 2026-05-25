package com.example.remind_ai.Stage3

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
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
import android.content.res.ColorStateList

data class SoothingContent(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var type: String = "",
    var fileUrl: String = "",
    var createdAt: Long = 0L
)

data class SurahItem(val number: Int, val name: String)

data class DuaAudioItem(
    val title: String,
    val description: String,
    val fileUrl: String
)

class SoothingContentC3Activity : AppCompatActivity() {

    private lateinit var comfortLibraryLayout: LinearLayout
    private lateinit var tvNowPlayingTitle: TextView
    private lateinit var tvNowPlayingSubtitle: TextView
    private lateinit var tvPlayerStatus: TextView
    private lateinit var tvDuration: TextView
    private lateinit var spSurah: Spinner

    private val contentRef = FirebaseDatabase.getInstance().reference.child("soothing_content")

    private var mediaPlayer: MediaPlayer? = null
    private var lastSelectedContent: SoothingContent? = null

    private val surahList = listOf(
        SurahItem(1, "Al-Fatiha"), SurahItem(2, "Al-Baqarah"), SurahItem(3, "Aal-Imran"),
        SurahItem(4, "An-Nisa"), SurahItem(5, "Al-Ma'idah"), SurahItem(6, "Al-An'am"),
        SurahItem(7, "Al-A'raf"), SurahItem(8, "Al-Anfal"), SurahItem(9, "At-Tawbah"),
        SurahItem(10, "Yunus"), SurahItem(11, "Hud"), SurahItem(12, "Yusuf"),
        SurahItem(13, "Ar-Ra'd"), SurahItem(14, "Ibrahim"), SurahItem(15, "Al-Hijr"),
        SurahItem(16, "An-Nahl"), SurahItem(17, "Al-Isra"), SurahItem(18, "Al-Kahf"),
        SurahItem(19, "Maryam"), SurahItem(20, "Ta-Ha"), SurahItem(21, "Al-Anbiya"),
        SurahItem(22, "Al-Hajj"), SurahItem(23, "Al-Mu'minun"), SurahItem(24, "An-Nur"),
        SurahItem(25, "Al-Furqan"), SurahItem(26, "Ash-Shu'ara"), SurahItem(27, "An-Naml"),
        SurahItem(28, "Al-Qasas"), SurahItem(29, "Al-Ankabut"), SurahItem(30, "Ar-Rum"),
        SurahItem(31, "Luqman"), SurahItem(32, "As-Sajdah"), SurahItem(33, "Al-Ahzab"),
        SurahItem(34, "Saba"), SurahItem(35, "Fatir"), SurahItem(36, "Ya-Sin"),
        SurahItem(37, "As-Saffat"), SurahItem(38, "Sad"), SurahItem(39, "Az-Zumar"),
        SurahItem(40, "Ghafir"), SurahItem(41, "Fussilat"), SurahItem(42, "Ash-Shura"),
        SurahItem(43, "Az-Zukhruf"), SurahItem(44, "Ad-Dukhan"), SurahItem(45, "Al-Jathiyah"),
        SurahItem(46, "Al-Ahqaf"), SurahItem(47, "Muhammad"), SurahItem(48, "Al-Fath"),
        SurahItem(49, "Al-Hujurat"), SurahItem(50, "Qaf"), SurahItem(51, "Adh-Dhariyat"),
        SurahItem(52, "At-Tur"), SurahItem(53, "An-Najm"), SurahItem(54, "Al-Qamar"),
        SurahItem(55, "Ar-Rahman"), SurahItem(56, "Al-Waqi'ah"), SurahItem(57, "Al-Hadid"),
        SurahItem(58, "Al-Mujadilah"), SurahItem(59, "Al-Hashr"), SurahItem(60, "Al-Mumtahanah"),
        SurahItem(61, "As-Saff"), SurahItem(62, "Al-Jumu'ah"), SurahItem(63, "Al-Munafiqun"),
        SurahItem(64, "At-Taghabun"), SurahItem(65, "At-Talaq"), SurahItem(66, "At-Tahrim"),
        SurahItem(67, "Al-Mulk"), SurahItem(68, "Al-Qalam"), SurahItem(69, "Al-Haqqah"),
        SurahItem(70, "Al-Ma'arij"), SurahItem(71, "Nuh"), SurahItem(72, "Al-Jinn"),
        SurahItem(73, "Al-Muzzammil"), SurahItem(74, "Al-Muddaththir"), SurahItem(75, "Al-Qiyamah"),
        SurahItem(76, "Al-Insan"), SurahItem(77, "Al-Mursalat"), SurahItem(78, "An-Naba"),
        SurahItem(79, "An-Nazi'at"), SurahItem(80, "Abasa"), SurahItem(81, "At-Takwir"),
        SurahItem(82, "Al-Infitar"), SurahItem(83, "Al-Mutaffifin"), SurahItem(84, "Al-Inshiqaq"),
        SurahItem(85, "Al-Buruj"), SurahItem(86, "At-Tariq"), SurahItem(87, "Al-A'la"),
        SurahItem(88, "Al-Ghashiyah"), SurahItem(89, "Al-Fajr"), SurahItem(90, "Al-Balad"),
        SurahItem(91, "Ash-Shams"), SurahItem(92, "Al-Layl"), SurahItem(93, "Ad-Duha"),
        SurahItem(94, "Ash-Sharh"), SurahItem(95, "At-Tin"), SurahItem(96, "Al-Alaq"),
        SurahItem(97, "Al-Qadr"), SurahItem(98, "Al-Bayyinah"), SurahItem(99, "Az-Zalzalah"),
        SurahItem(100, "Al-Adiyat"), SurahItem(101, "Al-Qari'ah"), SurahItem(102, "At-Takathur"),
        SurahItem(103, "Al-Asr"), SurahItem(104, "Al-Humazah"), SurahItem(105, "Al-Fil"),
        SurahItem(106, "Quraysh"), SurahItem(107, "Al-Ma'un"), SurahItem(108, "Al-Kawthar"),
        SurahItem(109, "Al-Kafirun"), SurahItem(110, "An-Nasr"), SurahItem(111, "Al-Masad"),
        SurahItem(112, "Al-Ikhlas"), SurahItem(113, "Al-Falaq"), SurahItem(114, "An-Nas")
    )

    private val duaAudioList = listOf(
        DuaAudioItem("Ayat-ul-Kursi", "Protection and calm", quranAyahAudioUrl(262)),
        DuaAudioItem("Rabbana Atina", "Dua for goodness", quranAyahAudioUrl(208)),
        DuaAudioItem("Last Ayah of Baqarah", "Dua for mercy", quranAyahAudioUrl(293)),
        DuaAudioItem("Rabbana La Tuzigh Quloobana", "Dua for heart stability", quranAyahAudioUrl(301)),
        DuaAudioItem("Rabbana Zalamna Anfusana", "Dua for forgiveness", quranAyahAudioUrl(977)),
        DuaAudioItem("Rabbi Zidni Ilma", "Dua for knowledge", quranAyahAudioUrl(2353)),
        DuaAudioItem("Rabbi Inni Massaniyad Durr", "Dua in hardship", quranAyahAudioUrl(2466)),
        DuaAudioItem("La ilaha illa Anta Subhanaka", "Dua of Yunus AS", quranAyahAudioUrl(2470)),
        DuaAudioItem("Rabbi Ishrah Li Sadri", "Dua for ease", quranAyahAudioUrl(2273)),
        DuaAudioItem("Rabbi Inni Lima Anzalta", "Dua for need", quranAyahAudioUrl(3176))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soothingcontent_c03)

        comfortLibraryLayout = findViewById(R.id.comfortLibraryLayout)
        tvNowPlayingTitle = findViewById(R.id.tvNowPlayingTitle)
        tvNowPlayingSubtitle = findViewById(R.id.tvNowPlayingSubtitle)
        tvPlayerStatus = findViewById(R.id.tvPlayerStatus)
        tvDuration = findViewById(R.id.tvDuration)
        spSurah = findViewById(R.id.spSurah)

        setupSurahSpinner()
        setupClicks()
        listenForLibrary()
    }

    private fun setupSurahSpinner() {
        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            surahList.map { "${it.number}. ${it.name}" }
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: android.view.ViewGroup
            ): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(Color.parseColor("#2C3442"))
                view.textSize = 16f
                view.setPadding(16, 0, 16, 0)
                return view
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: android.view.ViewGroup
            ): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(Color.parseColor("#2C3442"))
                view.setBackgroundColor(Color.WHITE)
                view.textSize = 16f
                view.setPadding(24, 18, 24, 18)
                return view
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spSurah.adapter = adapter
    }

    private fun setupClicks() {
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnPlaySurah).setOnClickListener {
            val surah = surahList[spSurah.selectedItemPosition]

            val content = SoothingContent(
                id = contentRef.push().key ?: return@setOnClickListener,
                title = "Surah ${surah.name}",
                description = "Quran recitation",
                type = "quran",
                fileUrl = quranSurahAudioUrl(surah.number),
                createdAt = System.currentTimeMillis()
            )

            saveContent(content)
            playContentOnCaretaker(content)
        }

        findViewById<View>(R.id.btnPlayNow).setOnClickListener {
            val content = lastSelectedContent

            if (content == null) {
                Toast.makeText(this, "Pehle content select karein", Toast.LENGTH_SHORT).show()
            } else {
                playContentOnCaretaker(content)
            }
        }

        findViewById<View>(R.id.btnStop).setOnClickListener {
            stopAudio()
            tvPlayerStatus.text = "Stopped"
        }

        findViewById<View>(R.id.btnAddContent).setOnClickListener {
            showTypePicker()
        }
    }

    private fun showTypePicker() {
        val types = arrayOf(
            "Audio Dua",
            "Text Dua",
            "Picture URL",
            "Video URL",
            "Audio / Music URL"
        )

        AlertDialog.Builder(this)
            .setTitle("Add Comfort Content")
            .setItems(types) { _, which ->
                when (which) {
                    0 -> showAudioDuaPicker()
                    1 -> showAddContentDialog("dua")
                    2 -> showAddContentDialog("image")
                    3 -> showAddContentDialog("video")
                    4 -> showAddContentDialog("audio")
                }
            }
            .show()
    }

    private fun showAudioDuaPicker() {
        AlertDialog.Builder(this)
            .setTitle("Select Audio Dua")
            .setItems(duaAudioList.map { it.title }.toTypedArray()) { _, which ->
                val dua = duaAudioList[which]

                val content = SoothingContent(
                    id = contentRef.push().key ?: return@setItems,
                    title = dua.title,
                    description = dua.description,
                    type = "dua_audio",
                    fileUrl = dua.fileUrl,
                    createdAt = System.currentTimeMillis()
                )

                saveContent(content)
                playContentOnCaretaker(content)
            }
            .show()
    }

    private fun showAddContentDialog(type: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_soothing_content, null)

        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etSubtitle = view.findViewById<EditText>(R.id.etSubtitle)
        val etText = view.findViewById<EditText>(R.id.etText)
        val etFileUrl = view.findViewById<EditText>(R.id.etFileUrl)

        if (type == "dua") {
            etText.visibility = View.VISIBLE
            etFileUrl.visibility = View.GONE
        } else {
            etText.visibility = View.GONE
            etFileUrl.visibility = View.VISIBLE

            etFileUrl.hint = when (type) {
                "image" -> "Picture URL"
                "video" -> "Video URL"
                else -> "Audio URL"
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Content Details")
            .setView(view)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = etTitle.text.toString().trim()
                val subtitle = etSubtitle.text.toString().trim()
                val text = etText.text.toString().trim()
                val fileUrl = etFileUrl.text.toString().trim()

                if (title.isEmpty()) {
                    etTitle.error = "Title required"
                    return@setOnClickListener
                }

                if (type != "dua" && fileUrl.isEmpty()) {
                    etFileUrl.error = "URL required"
                    return@setOnClickListener
                }

                val content = SoothingContent(
                    id = contentRef.push().key ?: return@setOnClickListener,
                    title = title,
                    description = if (type == "dua") text.ifEmpty { subtitle } else subtitle.ifEmpty { type },
                    type = type,
                    fileUrl = if (type == "dua") "" else fileUrl,
                    createdAt = System.currentTimeMillis()
                )

                dialog.dismiss()
                saveContent(content)
                playContentOnCaretaker(content)
            }
        }

        dialog.show()
    }

    private fun saveContent(content: SoothingContent) {
        contentRef.child(content.id).setValue(content)
            .addOnSuccessListener {
                lastSelectedContent = content
                updateNowPlaying(content)
                Toast.makeText(this, "Content saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    error.message ?: "Failed to save content",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun listenForLibrary() {
        contentRef.orderByChild("createdAt")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = snapshot.children
                        .mapNotNull { it.getValue(SoothingContent::class.java) }
                        .reversed()

                    renderLibrary(items)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@SoothingContentC3Activity,
                        error.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun renderLibrary(items: List<SoothingContent>) {
        comfortLibraryLayout.removeAllViews()

        if (items.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No comfort content added yet"
                textSize = 15f
                setTextColor(Color.parseColor("#2C3442"))
                setPadding(0, dp(8), 0, dp(8))
            }

            comfortLibraryLayout.addView(emptyText)
            return
        }

        items.forEach { item ->
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dp(12))
                }

                radius = dp(18).toFloat()
                cardElevation = dp(4).toFloat()
                setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                strokeColor = Color.parseColor("#D6CCFF")
                strokeWidth = dp(1)

                setOnClickListener {
                    playContentOnCaretaker(item)
                }
            }

            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
            }

            val title = TextView(this).apply {
                text = item.title
                textSize = 18f
                setTextColor(Color.parseColor("#1F2937"))
                setTypeface(null, Typeface.BOLD)
            }

            val desc = TextView(this).apply {
                text = item.description
                textSize = 14f
                setTextColor(Color.parseColor("#4B5563"))
                setPadding(0, dp(4), 0, 0)
            }

            val typeText = TextView(this).apply {
                text = item.type.uppercase()
                textSize = 12f
                setTextColor(Color.parseColor("#4A3FA0"))
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(6), 0, dp(10))
            }

            val playButton = MaterialButton(this).apply {
                text = "Play / Open"
                isAllCaps = false
                textSize = 15f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#6F54B5"))
                setOnClickListener {
                    playContentOnCaretaker(item)
                }
            }

            val deleteButton = MaterialButton(this).apply {
                text = "Delete"
                isAllCaps = false
                textSize = 15f
                setTextColor(Color.parseColor("#6F54B5"))
                setBackgroundColor(Color.WHITE)
                strokeColor = ColorStateList.valueOf(Color.parseColor("#6F54B5"))
                strokeWidth = dp(1)

                setOnClickListener {
                    if (lastSelectedContent?.id == item.id) {
                        lastSelectedContent = null
                    }

                    contentRef.child(item.id).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(
                                this@SoothingContentC3Activity,
                                "Deleted",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener { error ->
                            Toast.makeText(
                                this@SoothingContentC3Activity,
                                error.message ?: "Delete failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }

            column.addView(title)
            column.addView(desc)
            column.addView(typeText)
            column.addView(playButton)
            column.addView(deleteButton)

            card.addView(column)
            comfortLibraryLayout.addView(card)
        }
    }

    private fun playContentOnCaretaker(content: SoothingContent) {
        lastSelectedContent = content
        updateNowPlaying(content)

        when (content.type) {
            "quran", "audio", "dua_audio" -> {
                if (content.fileUrl.isNotEmpty()) {
                    playAudio(content.fileUrl)
                } else {
                    Toast.makeText(this, "Audio URL missing", Toast.LENGTH_SHORT).show()
                }
            }

            "image", "video" -> {
                if (content.fileUrl.isNotEmpty()) {
                    openUrl(content.fileUrl)
                } else {
                    Toast.makeText(this, "File URL missing", Toast.LENGTH_SHORT).show()
                }
            }

            "dua" -> {
                Toast.makeText(this, content.description, Toast.LENGTH_LONG).show()
                tvPlayerStatus.text = "Text dua selected"
            }
        }
    }

    private fun playAudio(url: String) {
        stopAudio()
        tvPlayerStatus.text = "Loading..."

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)

                setOnPreparedListener {
                    it.start()
                    tvPlayerStatus.text = "Playing on caretaker screen"
                }

                setOnErrorListener { _, _, _ ->
                    tvPlayerStatus.text = "Audio error"
                    Toast.makeText(
                        this@SoothingContentC3Activity,
                        "Audio play nahi ho rahi. Internet/URL check karein.",
                        Toast.LENGTH_LONG
                    ).show()
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            tvPlayerStatus.text = "Audio error"
            Toast.makeText(this, e.message ?: "Audio error", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopAudio() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "URL open nahi ho raha", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateNowPlaying(content: SoothingContent) {
        tvNowPlayingTitle.text = content.title
        tvNowPlayingSubtitle.text = content.description
        tvDuration.text = content.type.uppercase()
    }

    private fun quranSurahAudioUrl(surahNumber: Int): String {
        return "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/$surahNumber.mp3"
    }

    private fun quranAyahAudioUrl(globalAyahNumber: Int): String {
        return "https://cdn.islamic.network/quran/audio/128/ar.alafasy/$globalAyahNumber.mp3"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
    }
}