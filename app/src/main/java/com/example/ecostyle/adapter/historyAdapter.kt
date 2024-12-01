package com.example.ecostyle.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ecostyle.R

class HistoryAdapter(private val products: List<Map<String, Any>>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_product_card_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val product = products[position]
        holder.bind(product)
    }

    override fun getItemCount(): Int = products.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.product_name)
        private val price: TextView = itemView.findViewById(R.id.product_price)
        private val image: ImageView = itemView.findViewById(R.id.product_image)

        fun bind(product: Map<String, Any>) {
            title.text = product["nombre"] as String
            price.text = "Price: ${(product["precio"] as Number).toInt()}"
            val imageUrl = product["imagen"] as String

            // Cargar imagen usando Glide
            Glide.with(itemView.context).load(imageUrl).into(image)
        }
    }
}
