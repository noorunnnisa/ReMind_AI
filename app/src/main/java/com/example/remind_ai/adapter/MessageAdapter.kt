package com.example.remind_ai.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.remind_ai.databinding.ItemChatMessageBinding
import com.example.remind_ai.model.ChatMessage

class MessageAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.text
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
}