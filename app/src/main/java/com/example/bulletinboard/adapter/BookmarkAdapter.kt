package com.example.bulletinboard.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.bulletinboard.databinding.ItemBookmarkBinding
import com.example.bulletinboard.model.Bookmark

class BookmarkAdapter(
    private val context: Context,
    private val bookmarks: List<Bookmark>,
    private val onItemClick: (Bookmark) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder>() {

    inner class BookmarkViewHolder(val binding: ItemBookmarkBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val binding = ItemBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        holder.binding.boardIdText.text = bookmark.board_code // 名前を表示

        holder.binding.copyButton.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Board ID", bookmark.board_id))
            Toast.makeText(context, "IDをコピーしました", Toast.LENGTH_SHORT).show()
        }

        holder.binding.root.setOnClickListener {
            onItemClick(bookmark)
        }
    }

    override fun getItemCount() = bookmarks.size
}
