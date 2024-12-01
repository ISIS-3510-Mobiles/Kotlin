package com.example.ecostyle.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ecostyle.R

class HistoryAdapter(
    private val productList: List<Map<String, Any>>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.product_image)
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productPrice: TextView = itemView.findViewById(R.id.product_price)
        val productQuantity: TextView = itemView.findViewById(R.id.product_quantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_product_card_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val product = productList[position]

        val name = product["name"] as? String ?: "Unknown"
        val price = product["price"] as? String ?: "0"
        val quantity = (product["quantity"] as? Number)?.toInt() ?: 0
        val imageUrl = product["imageResource"] as? String

        holder.productName.text = name
        holder.productPrice.text = "Unit price: $$price"
        holder.productQuantity.text = "Quantity: $quantity"


        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .into(holder.productImage)
        }
    }

    override fun getItemCount(): Int {
        return productList.size
    }
}






