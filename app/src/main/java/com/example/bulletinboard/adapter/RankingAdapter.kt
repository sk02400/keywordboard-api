package com.example.bulletinboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bulletinboard.R
import com.example.bulletinboard.databinding.ItemRankingBinding
import com.example.bulletinboard.model.BoardRanking

class RankingAdapter(
    private val items: List<BoardRanking>,
    private val onItemClick: (BoardRanking) -> Unit
) : RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {

    inner class RankingViewHolder(val binding: ItemRankingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val binding = ItemRankingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RankingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding

        binding.textViewTitle.text = item.page_title

        // 投稿が2件以内であればその分だけ表示（最大2件）
        if (item.posts.isNotEmpty()) {
            binding.textViewPost1.text = item.posts[0].post_name + ":" + item.posts[0].content
            binding.textViewPost1.visibility = View.VISIBLE
        } else {
            binding.textViewPost1.visibility = View.GONE
        }

        if (item.posts.size > 1) {
            binding.textViewPost2.text = item.posts[1].post_name + ":" + item.posts[1].content
            binding.textViewPost2.visibility = View.VISIBLE
        } else {
            binding.textViewPost2.visibility = View.GONE
        }

        // is_link が true のときは favicon を表示
        if (item.is_link && item.favicon_url != null) {
            Glide.with(binding.imageViewFavicon.context)
                .load(item.favicon_url)
                .placeholder(R.drawable.ic_link)
                .into(binding.imageViewFavicon)
            binding.imageViewFavicon.visibility = View.VISIBLE
        } else {
            binding.imageViewFavicon.visibility = View.GONE
        }

        binding.root.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
