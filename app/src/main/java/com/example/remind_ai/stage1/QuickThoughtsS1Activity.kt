package com.example.remind_ai.stage1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.remind_ai.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import com.example.remind_ai.adapter.ThoughtsAdapter
import com.example.remind_ai.model.ThoughtModel

class QuickThoughtsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var etThought: TextInputEditText
    private lateinit var btnSaveThought: MaterialButton
    private lateinit var rvThoughts: RecyclerView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private val thoughtsList = ArrayList<ThoughtModel>()
    private lateinit var adapter: ThoughtsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_thoughts_s1)

        btnBack = findViewById(R.id.btnBack)
        etThought = findViewById(R.id.etThought)
        btnSaveThought = findViewById(R.id.btnSaveThought)
        rvThoughts = findViewById(R.id.rvThoughts)

        btnBack.setOnClickListener { finish() }
        btnSaveThought.setOnClickListener { saveThought() }

        // Recycler setup
        adapter = ThoughtsAdapter(thoughtsList)
        rvThoughts.layoutManager = LinearLayoutManager(this)
        rvThoughts.adapter = adapter

        fetchThoughts()
    }

    private fun saveThought() {
        val uid = auth.currentUser?.uid
        val thoughtText = etThought.text?.toString()?.trim().orEmpty()

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (thoughtText.isEmpty()) {
            etThought.error = "Please write something"
            return
        }

        val thoughtId = UUID.randomUUID().toString()
        val time = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        val data = hashMapOf(
            "id" to thoughtId,
            "text" to thoughtText,
            "createdAt" to System.currentTimeMillis(),
            "time" to time
        )

        btnSaveThought.isEnabled = false
        btnSaveThought.text = "Saving..."

        db.child("quick_thoughts")
            .child(uid)
            .child(thoughtId)
            .setValue(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                etThought.text?.clear()
                btnSaveThought.isEnabled = true
                btnSaveThought.text = "Save Thought"
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                btnSaveThought.isEnabled = true
                btnSaveThought.text = "Save Thought"
            }
    }

    private fun fetchThoughts() {
        val uid = auth.currentUser?.uid ?: return

        db.child("quick_thoughts").child(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    thoughtsList.clear()

                    for (child in snapshot.children) {
                        val thought = child.getValue(ThoughtModel::class.java)
                        if (thought != null) {
                            thoughtsList.add(thought)
                        }
                    }

                    thoughtsList.reverse() // latest first
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}