package com.example.ecostyle.Adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ecostyle.model.Product
import com.example.ecostyle.R

class ProductAdapter(private var productList: List<Product>, private val onItemClicked: (Product) -> Unit) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.product_image)
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productPrice: TextView = itemView.findViewById(R.id.product_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_product_card, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.productName.text = product.name
        holder.productPrice.text = product.price
        holder.productImage.setImageResource(product.imageResource)

        holder.itemView.setOnClickListener {
            onItemClicked(product)
        }
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    fun setProductList(products: List<Product>) {
        productList = products
        notifyDataSetChanged()
    }
}
