package com.example.bulletinboard.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bulletinboard.databinding.ItemMessageBinding
import com.example.bulletinboard.model.Message
import com.example.bulletinboard.R

class ChatAdapter(
    private val messages: List<Message>,
    private val userId: String
) : RecyclerView.Adapter<ChatAdapter.VH>() {

    inner class VH(val bind: ItemMessageBinding) : RecyclerView.ViewHolder(bind.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = messages[position]
        val binding = holder.bind

        binding.messageText.text = m.content
        binding.senderNameText.text = m.sender_name
        binding.timeText.text = m.created_at

        // メッセージの寄せ方向・背景を設定
        if (m.sender_id == userId) {
            binding.messageContainer.gravity = Gravity.END
            binding.messageText.setBackgroundResource(R.drawable.bg_message_self)
        } else {
            binding.messageContainer.gravity = Gravity.START
            binding.messageText.setBackgroundResource(R.drawable.bg_message_other)
        }
    }

    override fun getItemCount(): Int = messages.size
}
