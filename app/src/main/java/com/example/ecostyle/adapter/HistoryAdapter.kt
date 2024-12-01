package com.example.ecostyle.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ecostyle.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
class HistoryAdapter(
    private val productList: List<Map<String, Any>>,
    private val isSalesHistory: Boolean,
    private val userId: String
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
        val price = product["price"] as? String ?: "N/A"
        val initialQuantity = (product["quantity"] as? Number)?.toInt() ?: 0
        val productId = product["productId"] as? String
        val imageUrl = product["imageResource"] as? String

        holder.productName.text = name
        holder.productPrice.text = "Unit price: $${price}"

        if (isSalesHistory) {
            holder.productQuantity.text = "Initial quantity: $initialQuantity, Sold: ..."
            if (!productId.isNullOrEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val soldQuantity = fetchSoldQuantity(userId, productId, initialQuantity)
                    holder.productQuantity.post {
                        holder.productQuantity.text = "Initial quantity: $initialQuantity, Sold: ${soldQuantity.coerceAtLeast(0)}"
                    }
                }
            }
        } else {
            val purchasedQuantity = (product["quantity"] as? Number)?.toInt() ?: 0
            holder.productQuantity.text = "Purchased: $purchasedQuantity"
        }

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .into(holder.productImage)
        }
    }


    private suspend fun fetchSoldQuantity(userId: String, productId: String, initialQuantity: Int): Int {
        return try {

            val productDoc = FirebaseFirestore.getInstance()
                .collection("Products")
                .document(productId)
                .get()
                .await()

            val remainingQuantity = productDoc.getLong("quantity")?.toInt() ?: initialQuantity
            initialQuantity - remainingQuantity
        } catch (e: Exception) {
            0
        }
    }

    override fun getItemCount(): Int {
        return productList.size
    }
}
