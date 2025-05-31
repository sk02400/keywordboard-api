package com.example.bulletinboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bulletinboard.databinding.ItemPostBinding
import com.example.bulletinboard.model.Post

class PostAdapter(private var posts: List<Post>) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            binding.textNo.text = "${post.post_number}"  // ← 投稿番号を表示
            binding.textName.text = post.post_name
            binding.textTimestamp.text = post.created_at
            binding.textContent.text = post.content
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post)
    }

    override fun getItemCount(): Int = posts.size

    fun update(newPosts: List<Post>) {
        posts = newPosts.sortedBy { it.post_number }
        notifyDataSetChanged()
    }
}
