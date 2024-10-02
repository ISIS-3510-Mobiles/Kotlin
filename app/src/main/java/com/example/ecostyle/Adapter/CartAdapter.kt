package com.example.ecostyle.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ecostyle.model.CartItem
import com.example.ecostyle.R

class CartAdapter(private var cartItemList: List<CartItem>) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.product_image)
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productPrice: TextView = itemView.findViewById(R.id.product_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_product_card_checkout, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val cartItem = cartItemList[position]
        holder.productName.text = cartItem.productName
        holder.productPrice.text = cartItem.productPrice
        // holder.productImage.setImageResource(cartItem.productImage) // Si usas imágenes locales
    }

    override fun getItemCount(): Int {
        return cartItemList.size
    }

    fun setCartItems(newList: List<CartItem>) {
        cartItemList = newList
        notifyDataSetChanged()
    }
}
