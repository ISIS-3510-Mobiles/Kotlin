// CommentAdapter.kt

package com.example.ecostyle.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ecostyle.R
import com.example.ecostyle.model.Comment
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(private var commentList: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameText: TextView = itemView.findViewById(R.id.comment_user_name)
        val contentText: TextView = itemView.findViewById(R.id.comment_content)
        val timestampText: TextView = itemView.findViewById(R.id.comment_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentList[position]
        holder.userNameText.text = comment.userName
        holder.contentText.text = comment.content

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = Date(comment.timestamp)
        holder.timestampText.text = sdf.format(date)
    }


    override fun getItemCount(): Int = commentList.size

    fun setCommentList(newList: List<Comment>) {
        commentList = newList
        notifyDataSetChanged()
    }
}
