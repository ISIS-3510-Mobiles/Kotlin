// LikesAdapter.kt
package com.example.ecostyle.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ecostyle.R
import com.example.ecostyle.model.LikeItem

class LikesAdapter(
    private var likeItemList: List<LikeItem>,
    private val onItemClicked: (LikeItem) -> Unit,
    private val onRemoveClicked: (LikeItem) -> Unit
) : RecyclerView.Adapter<LikesAdapter.LikeViewHolder>() {

    class LikeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.product_image)
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productPrice: TextView = itemView.findViewById(R.id.product_price)
        val removeButton: ImageButton = itemView.findViewById(R.id.remove_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LikeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return LikeViewHolder(view)
    }

    override fun onBindViewHolder(holder: LikeViewHolder, position: Int) {
        val likeItem = likeItemList[position]
        holder.productName.text = likeItem.productName
        holder.productPrice.text = likeItem.productPrice

        Glide.with(holder.itemView.context)
            .load(likeItem.productImage)
            .into(holder.productImage)

        holder.itemView.setOnClickListener {
            onItemClicked(likeItem)
        }

        holder.removeButton.setOnClickListener {
            onRemoveClicked(likeItem)
        }
    }

    override fun getItemCount(): Int {
        return likeItemList.size
    }

    fun setLikeItems(newList: List<LikeItem>) {
        likeItemList = newList
        notifyDataSetChanged()
    }
}
