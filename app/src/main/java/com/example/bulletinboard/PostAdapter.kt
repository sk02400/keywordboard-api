package com.example.bulletinboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bulletinboard.databinding.ItemPostBinding
import com.example.bulletinboard.model.Post
import android.widget.TextView

class PostAdapter(private var posts: List<Post>) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
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
        holder.bind(posts[position])
        holder.itemView.findViewById<TextView>(R.id.textNo).text = "${position + 1}."
    }

    override fun getItemCount(): Int = posts.size

    fun update(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}
