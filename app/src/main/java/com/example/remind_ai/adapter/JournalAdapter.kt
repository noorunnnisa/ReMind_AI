package com.example.remind_ai.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.remind_ai.R
import com.example.remind_ai.model.JournalEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class JournalAdapter(
    private var list: List<JournalEntry>
) : RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    class JournalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvMood: TextView = itemView.findViewById(R.id.tvMood)
        val tvText: TextView = itemView.findViewById(R.id.tvJournal)
        val tvAyah: TextView = itemView.findViewById(R.id.tvAnalysis)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_journal, parent, false)

        return JournalViewHolder(view)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {

        val item = list[position]

        holder.tvDate.text = item.formattedDate
        holder.tvMood.text = "Mood: ${item.mood}"
        holder.tvText.text = item.text

        holder.tvAyah.text =
            if (item.ayahArabic.isNotEmpty()) item.ayahArabic else "No Ayah"

        // =========================
        // DELETE FUNCTION
        // =========================
        holder.btnDelete.setOnClickListener {

            val uid = auth.currentUser?.uid ?: return@setOnClickListener

            db.child("journals")
                .child(uid)
                .child(item.id)
                .removeValue()
                .addOnSuccessListener {

                    Toast.makeText(
                        holder.itemView.context,
                        "Journal deleted",
                        Toast.LENGTH_SHORT
                    ).show()

                    // remove from list locally
                    val mutableList = list.toMutableList()
                    mutableList.removeAt(position)

                    list = mutableList
                    notifyItemRemoved(position)
                }
                .addOnFailureListener {

                    Toast.makeText(
                        holder.itemView.context,
                        "Delete failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<JournalEntry>) {
        list = newList
        notifyDataSetChanged()
    }
}