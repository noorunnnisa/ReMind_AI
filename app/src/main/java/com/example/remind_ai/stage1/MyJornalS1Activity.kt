package com.example.remind_ai.stage1

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.remind_ai.R
import com.example.remind_ai.adapter.JournalAdapter
import com.example.remind_ai.model.JournalAnalysis
import com.example.remind_ai.model.JournalEntry
import com.example.remind_ai.repository.MoodAyahRepository
import com.example.remind_ai.repository.QuranAyahService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class MyJournalActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var etJournal: EditText
    private lateinit var btnSave: Button
    private lateinit var btnAnalyze: Button
    private lateinit var btnSaved: LinearLayout
    private lateinit var recyclerSavedJournals: RecyclerView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private val journalList = ArrayList<JournalEntry>()
    private lateinit var journalAdapter: JournalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_myjornal_s1)

        // INIT VIEWS
        btnBack = findViewById(R.id.btnBack)
        etJournal = findViewById(R.id.etJournal)
        btnSave = findViewById(R.id.btnSave)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        btnSaved = findViewById(R.id.btnSaved)
        recyclerSavedJournals = findViewById(R.id.recyclerSavedJournals)

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener { saveJournalOnly() }

        btnAnalyze.setOnClickListener { analyzeAndSaveJournal() }

        // Recycler setup
        journalAdapter = JournalAdapter(journalList)
        recyclerSavedJournals.layoutManager = LinearLayoutManager(this)
        recyclerSavedJournals.adapter = journalAdapter

        // Saved toggle button
        btnSaved.setOnClickListener {
            if (recyclerSavedJournals.visibility == View.GONE) {
                recyclerSavedJournals.visibility = View.VISIBLE
                fetchSavedJournals()
            } else {
                recyclerSavedJournals.visibility = View.GONE
            }
        }
    }

    // =========================
    // FETCH SAVED JOURNALS
    // =========================
    private fun fetchSavedJournals() {

        val uid = auth.currentUser?.uid ?: return

        db.child("journals")
            .child(uid)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    journalList.clear()

                    for (child in snapshot.children) {

                        val journal = child.getValue(JournalEntry::class.java)

                        if (journal != null) {
                            journalList.add(journal)
                        }
                    }

                    journalList.reverse()
                    journalAdapter.updateData(journalList)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // =========================
    // SAVE ONLY JOURNAL
    // =========================
    private fun saveJournalOnly() {

        val uid = auth.currentUser?.uid
        val text = etJournal.text.toString().trim()

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (text.isEmpty()) {
            etJournal.error = "Please write something"
            return
        }

        val id = UUID.randomUUID().toString()
        val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val entry = JournalEntry(
            id = id,
            text = text,
            mood = "Not analyzed",
            supportMessage = "",
            ayahArabic = "",
            ayahTranslation = "",
            createdAt = System.currentTimeMillis(),
            formattedDate = date
        )

        db.child("journals").child(uid).child(id).setValue(entry)
            .addOnSuccessListener {
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                etJournal.text.clear()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
            }
    }

    // =========================
    // ANALYZE + SAVE JOURNAL
    // =========================
    private fun analyzeAndSaveJournal() {

        val uid = auth.currentUser?.uid
        val text = etJournal.text.toString().trim()

        if (uid == null || text.isEmpty()) return

        val analysis = analyzeMood(text)
        val ref = MoodAyahRepository.randomReferenceForMood(analysis.mood)

        QuranAyahService.fetchAyah(
            reference = ref,
            onSuccess = { ayah ->

                runOnUiThread {

                    val id = UUID.randomUUID().toString()
                    val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

                    val entry = JournalEntry(
                        id = id,
                        text = text,
                        mood = analysis.mood,
                        supportMessage = analysis.supportMessage,
                        ayahArabic = ayah.arabic,
                        ayahTranslation = ayah.translation,
                        createdAt = System.currentTimeMillis(),
                        formattedDate = date
                    )

                    db.child("journals").child(uid).child(id).setValue(entry)
                        .addOnSuccessListener {
                            etJournal.text.clear()
                            showDialog(analysis, ayah.arabic, ayah.translation, ayah.reference)
                        }
                }
            },
            onError = {
                runOnUiThread {
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // =========================
    // MOOD ANALYSIS (UPGRADED)
    // =========================
    private fun analyzeMood(text: String): JournalAnalysis {

        val lower = text.lowercase()

        val sadWords = listOf(
            "sad","cry","lonely","depressed","down","hurt","broken","hopeless",
            "empty","miserable","unhappy","low","heartbroken","grief","sorrow",
            "tearful","pain","upset","gloomy","devastated","helpless","rejected"
        )

        val stressWords = listOf(
            "stress","stressed","anxious","worried","tension","panic","fear",
            "afraid","overthinking","nervous","restless","pressure","burnout",
            "exhausted","tired","frustrated","irritated","angry","overwhelmed",
            "disturbed","tense","agitated"
        )

        val positiveWords = listOf(
            "happy","joy","good","great","excellent","amazing","grateful",
            "thankful","blessed","peaceful","calm","relaxed","motivated",
            "hopeful","excited","cheerful","content","satisfied","positive",
            "energetic","confident","loved","smiling"
        )

        val sadCount = sadWords.count { Regex("\\b$it\\b").containsMatchIn(lower) }
        val stressCount = stressWords.count { Regex("\\b$it\\b").containsMatchIn(lower) }
        val positiveCount = positiveWords.count { Regex("\\b$it\\b").containsMatchIn(lower) }

        return when {

            sadCount > stressCount && sadCount > positiveCount ->
                JournalAnalysis("Sad", "You are not alone. This pain will pass, InshaAllah.")

            stressCount > sadCount && stressCount > positiveCount ->
                JournalAnalysis("Stressed", "Take a deep breath. You are stronger than your stress.")

            positiveCount > 0 ->
                JournalAnalysis("Positive", "Alhamdulillah! Keep spreading your positivity.")

            else ->
                JournalAnalysis("Neutral", "Reflection is a powerful step toward healing.")
        }
    }

    // =========================
    // ANALYSIS DIALOG
    // =========================
    private fun showDialog(
        analysis: JournalAnalysis,
        arabic: String,
        translation: String,
        ref: String
    ) {
        AlertDialog.Builder(this)
            .setTitle("Journal Reflection")
            .setMessage(
                "Mood: ${analysis.mood}\n\n" +
                        "${analysis.supportMessage}\n\n" +
                        "$arabic\n\n" +
                        "$translation\n\n" +
                        "Ref: $ref"
            )
            .setPositiveButton("OK", null)
            .show()
    }
}