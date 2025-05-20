package com.example.bulletinboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bulletinboard.databinding.ItemChatRoomBinding
import com.example.bulletinboard.model.ChatRoom

class ChatRoomAdapter(
    private val rooms: List<ChatRoom>,
    private val onClick: (ChatRoom) -> Unit
) : RecyclerView.Adapter<ChatRoomAdapter.VH>() {

    inner class VH(val bind: ItemChatRoomBinding) : RecyclerView.ViewHolder(bind.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemChatRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val r = rooms[pos]
        holder.bind.partnerNameText.text = r.partner_name.ifBlank { r.partner_id }
        holder.bind.lastMessageText.text = r.last_message ?: ""
        holder.bind.root.setOnClickListener { onClick(r) }
    }

    override fun getItemCount() = rooms.size
}
