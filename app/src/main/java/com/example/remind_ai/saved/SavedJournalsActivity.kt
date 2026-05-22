package com.example.remind_ai.saved

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.remind_ai.R
import com.example.remind_ai.adapter.JournalAdapter
import com.example.remind_ai.model.JournalEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SavedJournalsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: JournalAdapter
    private val list = mutableListOf<JournalEntry>()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_journals)

        recyclerView = findViewById(R.id.recyclerJournals)

        adapter = JournalAdapter(list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadJournals()
    }

    private fun loadJournals() {
        val uid = auth.currentUser?.uid ?: return

        db.child("journals").child(uid)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    list.clear()

                    for (child in snapshot.children) {
                        val item = child.getValue(JournalEntry::class.java)
                        if (item != null) {
                            list.add(item)
                        }
                    }

                    list.sortByDescending { it.createdAt }
                    adapter.updateData(list)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}