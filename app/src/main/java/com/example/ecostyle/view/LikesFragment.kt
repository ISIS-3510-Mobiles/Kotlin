// LikesFragment.kt

package com.example.ecostyle.view

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.example.ecostyle.utils.LocalStorageManager
import com.example.ecostyle.viewmodel.ProductViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class LikesFragment : Fragment() {

    private lateinit var likesAdapter: LikesAdapter
    private lateinit var recyclerView: RecyclerView
    private var likeItemList: List<LikeItem> = emptyList()

    private val productViewModel: ProductViewModel by activityViewModels()  // Share the ViewModel
    private var productList: List<Product> = emptyList()  // Store the product list

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        return inflater.inflate(R.layout.likes_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure RecyclerView and Adapter
        recyclerView = view.findViewById(R.id.recycler_view_liked_products)
        recyclerView.layoutManager = LinearLayoutManager(context)

        likesAdapter = LikesAdapter(likeItemList, { likeItem ->
            if (likeItem.productId != -1) {
                // Navigate to product detail passing the productId
                val productDetailFragment = ProductDetailFragment().apply {
                    arguments = Bundle().apply {
                        putInt("PRODUCT_ID", likeItem.productId)
                    }
                }

                // Navigate to ProductDetailFragment
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, productDetailFragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                Toast.makeText(context, "Product not found", Toast.LENGTH_SHORT).show()
            }
        }, { likeItem ->
            // Remove the product from favorites
            removeFromLikes(likeItem)
        })

        recyclerView.adapter = likesAdapter

        // Observe the product list
        productViewModel.getProductList().observe(viewLifecycleOwner) { products ->
            if (products != null) {
                productList = products  // Store the product list
                // Now that we have the product list, load the liked items
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
                    val likedProductIds = mutableSetOf<String>()

                    for (document in documents) {
                        val likeItem = document.toObject(LikeItem::class.java)
                        likeItem?.let {
                            likeItem.id = document.id
                            // Find the corresponding product in the product list
                            val product = productList.find { it.firebaseId == likeItem.firebaseId }
                            if (product != null) {
                                likeItem.productId = product.id
                                likedProductIds.add(product.firebaseId) // Add to local storage set
                            } else {
                                likeItem.productId = -1 // Product not found
                            }

                            likeItems.add(likeItem)
                        }
                    }

                    // Save the IDs of liked products in local storage
                    LocalStorageManager.saveLikedProducts(requireContext(), likedProductIds)

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

        val context = requireContext()

        // Check for Internet connection before removing
        if (!hasInternetConnection(context)) {
            Toast.makeText(context, "No Internet connection. Please check your connectivity.", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId != null) {
            val likesRef = db.collection("likes").document(userId).collection("items")
            likesRef.document(likeItem.id).delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "${likeItem.productName} removed from favorites", Toast.LENGTH_SHORT).show()
                    // Remove from local storage
                    LocalStorageManager.removeLikedProduct(context, likeItem.firebaseId)
                    // Reload favorites
                    loadLikedItems(productList)  // Pass productList as argument
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error removing from favorites", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun hasInternetConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val network = connectivityManager?.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}
