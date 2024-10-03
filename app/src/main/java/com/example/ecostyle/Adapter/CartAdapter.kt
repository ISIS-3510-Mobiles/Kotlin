package com.example.ecostyle.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ecostyle.model.CartItem
import com.example.ecostyle.R

class CartAdapter(
    private var cartItemList: List<CartItem>,
    private val onRemoveClick: (CartItem) -> Unit // Callback para manejar la eliminación
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.product_image)
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productPrice: TextView = itemView.findViewById(R.id.product_price)
        val removeButton: Button = itemView.findViewById(R.id.remove_button) // Botón para eliminar
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_product_card_checkout, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val cartItem = cartItemList[position]
        holder.productName.text = cartItem.productName
        holder.productPrice.text = "$${cartItem.productPrice}"
        // Configurar imagen, si usas imágenes locales o remotas
        // holder.productImage.setImageResource(cartItem.productImageResource)

        // Configurar el botón de eliminar
        holder.removeButton.setOnClickListener {
            onRemoveClick(cartItem) // Llama al callback para eliminar el producto
        }
    }

    override fun getItemCount(): Int {
        return cartItemList.size
    }

    fun setCartItems(newList: List<CartItem>) {
        cartItemList = newList
        notifyDataSetChanged()
    }
}
