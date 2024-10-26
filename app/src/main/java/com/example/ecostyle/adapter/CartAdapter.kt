package com.example.ecostyle.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ecostyle.model.CartItem
import com.example.ecostyle.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CartAdapter(
    var cartItemList: List<CartItem>,
    private val onRemoveClick: (CartItem) -> Unit, // Callback para manejar la eliminación
    private val onQuantityChanged: (CartItem) -> Unit // Callback para actualizar cantidad
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.product_image)
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productPrice: TextView = itemView.findViewById(R.id.product_price)
        val productQuantity: TextView = itemView.findViewById(R.id.product_quantity) // TextView para la cantidad
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
        holder.productQuantity.text = "Quantity: ${cartItem.quantity}" // Mostrar la cantidad

        // Configurar la imagen del producto usando Glide
        Glide.with(holder.itemView.context)
            .load(cartItem.productImage)
            .into(holder.productImage)

        // Configurar el botón de eliminar
        holder.removeButton.setOnClickListener {
            // Usar el callback para eliminar el ítem del carrito
            onRemoveClick(cartItem)
        }
    }

    override fun getItemCount(): Int {
        return cartItemList.size
    }

    // Función para eliminar un producto del carrito
    fun removeCartItem(cartItem: CartItem) {
        val mutableList = cartItemList.toMutableList()
        mutableList.remove(cartItem)
        cartItemList = mutableList
        notifyDataSetChanged() // Actualizar la lista de productos
    }

    fun setCartItems(newList: List<CartItem>) {
        cartItemList = newList
        notifyDataSetChanged()
    }
}
