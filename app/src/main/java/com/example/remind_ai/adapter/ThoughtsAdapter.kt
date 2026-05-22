package com.example.remind_ai.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.remind_ai.R
import com.example.remind_ai.model.ThoughtModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ThoughtsAdapter(
    private val list: List<ThoughtModel>
) : RecyclerView.Adapter<ThoughtsAdapter.ThoughtViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    class ThoughtViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvText: TextView = itemView.findViewById(R.id.tvThoughtText)
        val tvTime: TextView = itemView.findViewById(R.id.tvThoughtTime)

        // DELETE BUTTON
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThoughtViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thought, parent, false)

        return ThoughtViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThoughtViewHolder, position: Int) {

        val item = list[position]

        holder.tvText.text = item.text
        holder.tvTime.text = item.time

        // DELETE FUNCTION
        holder.btnDelete.setOnClickListener {

            val uid = auth.currentUser?.uid ?: return@setOnClickListener

            db.child("quick_thoughts")
                .child(uid)
                .child(item.id)
                .removeValue()
                .addOnSuccessListener {

                    Toast.makeText(
                        holder.itemView.context,
                        "Thought deleted",
                        Toast.LENGTH_SHORT
                    ).show()
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

    override fun getItemCount(): Int {
        return list.size
    }
}