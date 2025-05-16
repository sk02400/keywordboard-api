package com.example.bulletinboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.bulletinboard.model.Post

class PostAdapter(context: Context, private val posts: MutableList<Post>) :
    ArrayAdapter<Post>(context, 0, posts) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val post = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_post, parent, false)

        val nameText = view.findViewById<TextView>(R.id.textName)
        val timeText = view.findViewById<TextView>(R.id.textTimestamp)
        val contentText = view.findViewById<TextView>(R.id.textContent)

        nameText.text = post?.userName
        timeText.text = post?.createdAt
        contentText.text = post?.content

        return view
    }

    fun update(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
}
