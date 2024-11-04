// LikesFragment.kt
package com.example.ecostyle.view

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecostyle.R
import com.example.ecostyle.adapter.LikesAdapter
import com.example.ecostyle.model.LikeItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class LikesFragment : Fragment() {

    private lateinit var likesAdapter: LikesAdapter
    private lateinit var recyclerView: RecyclerView
    private var likeItemList: List<LikeItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        val view = inflater.inflate(R.layout.likes_fragment, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_liked_products)
        recyclerView.layoutManager = LinearLayoutManager(context)

        likesAdapter = LikesAdapter(likeItemList, { likeItem ->
            // Manejar clic en el producto si es necesario
            // Por ejemplo, abrir detalles del producto
        }, { likeItem ->
            // Eliminar el producto de favoritos
            removeFromLikes(likeItem)
        })
        recyclerView.adapter = likesAdapter

        loadLikedItems()

        return view
    }

    private fun loadLikedItems() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val documents = db.collection("likes").document(userId).collection("items").get().await()

                    val likeItems = mutableListOf<LikeItem>()

                    for (document in documents) {
                        val likeItem = document.toObject(LikeItem::class.java)
                        likeItem?.let {
                            likeItem.id = document.id
                            likeItems.add(likeItem)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        likesAdapter.setLikeItems(likeItems)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error loading favorites.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeFromLikes(likeItem: LikeItem) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val likesRef = db.collection("likes").document(userId).collection("items")
            likesRef.document(likeItem.id).delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "${likeItem.productName} removed from favorites", Toast.LENGTH_SHORT).show()
                    // Reload the liked items
                    loadLikedItems()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error removing from favorites", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Verificar disponibilidad de red (opcional)
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}
