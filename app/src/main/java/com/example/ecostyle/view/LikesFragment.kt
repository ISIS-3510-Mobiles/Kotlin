// LikesFragment.kt
package com.example.ecostyle.view

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecostyle.R
import com.example.ecostyle.adapter.LikesAdapter
import com.example.ecostyle.model.LikeItem
import com.example.ecostyle.model.Product
import com.example.ecostyle.viewmodel.ProductViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class LikesFragment : Fragment() {

    private lateinit var likesAdapter: LikesAdapter
    private lateinit var recyclerView: RecyclerView
    private var likeItemList: List<LikeItem> = emptyList()

    private val productViewModel: ProductViewModel by activityViewModels()  // Compartir el ViewModel
    private var productList: List<Product> = emptyList()  // Almacenar la lista de productos

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        return inflater.inflate(R.layout.likes_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar RecyclerView y Adapter
        recyclerView = view.findViewById(R.id.recycler_view_liked_products)
        recyclerView.layoutManager = LinearLayoutManager(context)

        likesAdapter = LikesAdapter(likeItemList, { likeItem ->
            if (likeItem.productId != -1) {
                // Navegar al detalle del producto pasando el productId
                val productDetailFragment = ProductDetailFragment().apply {
                    arguments = Bundle().apply {
                        putInt("PRODUCT_ID", likeItem.productId)
                    }
                }

                // Navegar al ProductDetailFragment
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, productDetailFragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                Toast.makeText(context, "Product not found", Toast.LENGTH_SHORT).show()
            }
        }, { likeItem ->
            // Eliminar el producto de favoritos
            removeFromLikes(likeItem)
        })

        recyclerView.adapter = likesAdapter

        // Observar la lista de productos
        productViewModel.getProductList().observe(viewLifecycleOwner) { products ->
            if (products != null) {
                productList = products  // Almacenar la lista de productos
                // Ahora que tenemos la lista de productos, cargamos los favoritos
                loadLikedItems(productList)
            }
        }
    }

    private fun loadLikedItems(productList: List<Product>) {
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

                            // Buscar el producto correspondiente en la lista de productos
                            val product = productList.find { it.firebaseId == likeItem.firebaseId }

                            if (product != null) {
                                likeItem.productId = product.id
                            } else {
                                likeItem.productId = -1 // Producto no encontrado
                            }

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
                    // Recargar los favoritos
                    loadLikedItems(productList)  // Pasar productList como argumento
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
