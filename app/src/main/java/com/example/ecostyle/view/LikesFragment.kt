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
import com.example.ecostyle.adapter.ProductAdapter
import com.example.ecostyle.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class LikesFragment : Fragment() {

    private lateinit var productAdapter: ProductAdapter
    private lateinit var recyclerView: RecyclerView
    private var likedProductList: List<Product> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        val view = inflater.inflate(R.layout.likes_fragment, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_liked_products)
        recyclerView.layoutManager = LinearLayoutManager(context)

        productAdapter = ProductAdapter(likedProductList, { product ->
            // Manejar clics en productos si es necesario
        }, { product ->
            // Cuando un producto es desmarcado como favorito, se elimina de la lista
            likedProductList = likedProductList.filter { it != product }
            productAdapter.setProductList(likedProductList)
        })
        recyclerView.adapter = productAdapter

        loadLikedProducts()

        return view
    }

    private fun loadLikedProducts() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    // Asumiendo que los likes se almacenan en "users/{userId}/likes"
                    val likedProductsRef = db.collection("users").document(userId).collection("likes")
                    val snapshot = likedProductsRef.get().await()

                    val newLikedProductList = mutableListOf<Product>()

                    for (document in snapshot.documents) {
                        val productId = document.id
                        // Obtener detalles del producto desde "Products"
                        val productDoc = db.collection("Products").document(productId).get().await()
                        if (productDoc.exists()) {
                            val product = productDoc.toObject(Product::class.java)
                            product?.let {
                                it.firebaseId = productDoc.id
                                it.isFavorite = true
                                newLikedProductList.add(it)
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        likedProductList = newLikedProductList
                        productAdapter.setProductList(likedProductList)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error al cargar los productos favoritos.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
        }
    }

    // Verificar disponibilidad de red
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}
