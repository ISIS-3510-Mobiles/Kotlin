// ProductDetailFragment.kt

package com.example.ecostyle.view

import CommentUploadWorker
import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.ecostyle.R
import com.example.ecostyle.model.Product
import com.example.ecostyle.viewmodel.ProductDetailViewModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.example.ecostyle.model.CartItem
import com.example.ecostyle.utils.LocalStorageManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.ecostyle.adapter.CommentAdapter
import com.example.ecostyle.model.Comment
import com.google.firebase.firestore.FieldValue

class ProductDetailFragment : Fragment() {

    private val viewModel: ProductDetailViewModel by viewModels()

    private var productId: Int = -1

    private lateinit var productImage: ImageView
    private lateinit var productName: TextView
    private lateinit var productPrice: TextView
    private lateinit var productDescription: TextView
    private lateinit var favoriteButton: ImageButton
    private lateinit var addToCartButton: Button

    private lateinit var commentsTitle: TextView
    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var noCommentsTextView: TextView
    private lateinit var newCommentInput: EditText
    private lateinit var postCommentButton: Button
    private lateinit var commentAdapter: CommentAdapter

    private var commentsList: MutableList<Comment> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            productId = it.getInt("PRODUCT_ID", -1)
        }

        if (productId == -1) {
            Log.e("ProductDetailFragment", "Invalid productId received!")
        } else {
            Log.d("ProductDetailFragment", "Received productId: $productId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        return inflater.inflate(R.layout.fragment_product_detail, container, false)
    }

    // Agrega el método onResume() aquí
    override fun onResume() {
        super.onResume()
        uploadPendingComments()
        scheduleCommentUploadWorker(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productImage = view.findViewById(R.id.product_detail_image)
        productName = view.findViewById(R.id.product_detail_title)
        productPrice = view.findViewById(R.id.product_detail_price)
        productDescription = view.findViewById(R.id.product_detail_description)
        favoriteButton = view.findViewById(R.id.favorite_icon)
        addToCartButton = view.findViewById(R.id.btn_add_to_cart)

        commentsTitle = view.findViewById(R.id.comments_title)
        commentsRecyclerView = view.findViewById(R.id.comments_recyclerview)
        noCommentsTextView = view.findViewById(R.id.no_comments_text)
        newCommentInput = view.findViewById(R.id.new_comment_input)
        postCommentButton = view.findViewById(R.id.post_comment_button)


        // Configurar RecyclerView
        commentAdapter = CommentAdapter(commentsList)
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        commentsRecyclerView.adapter = commentAdapter

        viewModel.product.observe(viewLifecycleOwner) { product ->
            productName.text = product.name
            productPrice.text = product.price.toString()
            productDescription.text = product.description

            Glide.with(this)
                .load(product.imageResource)
                .into(productImage)


            product.isFavorite =
                LocalStorageManager.isProductLiked(requireContext(), product.firebaseId)
            updateLikeIcon(product.isFavorite)
            loadComments()
        }

        viewModel.loadProduct(productId)


        favoriteButton.setOnClickListener {
            val product = viewModel.product.value
            if (product != null) {
                if (!hasInternetConnection()) {
                    Toast.makeText(context, "No Internet connection", Toast.LENGTH_SHORT).show()

                } else {
                    toggleFavorite(product)
                }
            }
        }

        addToCartButton.setOnClickListener {
            viewModel.product.value?.let { product ->
                addToCart(product)
            }
        }

        postCommentButton.setOnClickListener {
            val commentText = newCommentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addComment(commentText)
                newCommentInput.text.clear()
            } else {
                Toast.makeText(requireContext(), "Comment cannot be empty", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    }

    private fun toggleFavorite(product: Product) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val context = requireContext()

        if (userId != null) {
            val likesRef = db.collection("likes").document(userId).collection("items")

            likesRef.whereEqualTo("firebaseId", product.firebaseId).get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        // El producto ya tiene "like", así que lo eliminamos
                        for (document in documents) {
                            likesRef.document(document.id).delete()
                                .addOnSuccessListener {
                                    product.isFavorite = false
                                    updateLikeIcon(product.isFavorite)
                                    Toast.makeText(
                                        context,
                                        "${product.name} removed from favorites",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    LocalStorageManager.removeLikedProduct(
                                        context,
                                        product.firebaseId
                                    )

                                    // Aquí puedes registrar el evento de 'like' eliminado
                                    product.name?.let { it1 -> logLikeEvent(it1, false) }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        "Error removing from favorites",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    } else {
                        // Añadir el producto a likes
                        val likeItem = hashMapOf(
                            "firebaseId" to product.firebaseId,
                            "productName" to product.name,
                            "productPrice" to product.price,
                            "productImage" to product.imageResource,
                            "timestamp" to System.currentTimeMillis()
                        )
                        likesRef.add(likeItem)
                            .addOnSuccessListener {
                                product.isFavorite = true
                                updateLikeIcon(product.isFavorite)
                                Toast.makeText(
                                    context,
                                    "${product.name} added to favorites",
                                    Toast.LENGTH_SHORT
                                ).show()
                                LocalStorageManager.addLikedProduct(context, product.firebaseId)

                                // Aquí puedes registrar el evento de 'like' añadido
                                product.name?.let { it1 -> logLikeEvent(it1, true) }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Error adding to favorites",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error accessing favorites", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    // Función para registrar el evento
    private fun logLikeEvent(productName: String, liked: Boolean) {
        val eventName = if (liked) "liked_$productName" else "unliked_$productName"
        // Aquí puedes usar tu herramienta de analytics para registrar el evento
        val analytics = FirebaseAnalytics.getInstance(requireContext())
        val bundle = Bundle()
        bundle.putString("message", "Number likes")
        analytics.logEvent(eventName, bundle)
    }

    private fun updateLikeIcon(isFavorite: Boolean) {
        val likeIconRes = if (isFavorite) {
            R.drawable.baseline_favorite_24_2 // Ícono de corazón lleno
        } else {
            R.drawable.baseline_favorite_border_24 // Ícono de corazón vacío
        }
        favoriteButton.setImageResource(likeIconRes)
    }

    // Añadir productos al carrito
    private fun addToCart(product: Product) {
        val db = Firebase.firestore
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        // Verificar conexión a Internet antes de continuar
        if (!hasInternetConnection()) {
            Toast.makeText(
                requireContext(),
                "No Internet connection. Unable to add to cart.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (userId != null) {
            val cartRef = db.collection("carts").document(userId).collection("items")

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    // Verificar si el producto ya está en el carrito
                    val documents =
                        cartRef.whereEqualTo("firebaseId", product.firebaseId).get().await()

                    if (!documents.isEmpty) {
                        for (document in documents) {
                            val cartItem = document.toObject(CartItem::class.java)
                            val currentQuantity = cartItem.quantity

                            // Actualizar la cantidad
                            cartRef.document(document.id)
                                .update("quantity", currentQuantity + 1)
                                .await()

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    requireContext(),
                                    "Updated quantity in cart",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        // Añadir el producto con su firebaseId
                        val cartItem = hashMapOf(
                            "firebaseId" to product.firebaseId,
                            "productName" to product.name,
                            "productPrice" to product.price,
                            "productImage" to product.imageResource,
                            "quantity" to 1,
                            "timestamp" to System.currentTimeMillis()
                        )

                        cartRef.add(cartItem).await()

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "${product.name} added to cart",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        } else {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || activeNetwork.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR
        )
    }

    private fun loadComments() {
        val db = FirebaseFirestore.getInstance()
        val product = viewModel.product.value
        val productId = product?.firebaseId ?: return

        db.collection("Products").document(productId)
            .collection("Comments")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ProductDetailFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val firebaseComments = mutableListOf<Comment>()
                    for (doc in snapshots.documents) {
                        val comment = doc.toObject(Comment::class.java)
                        if (comment != null) {
                            firebaseComments.add(comment)
                        }
                    }

                    // Combinar comentarios de Firebase y locales, evitando duplicados
                    val localComments = LocalStorageManager.getPendingComments(requireContext())[productId] ?: emptyList()
                    val combinedComments = (firebaseComments + localComments).distinctBy { "${it.userId}_${it.timestamp}" }

                    commentsList.clear()
                    commentsList.addAll(combinedComments)

                    commentAdapter.setCommentList(commentsList)
                    commentsTitle.text = "Comments (${commentsList.size})"

                    noCommentsTextView.visibility = if (commentsList.isEmpty()) View.VISIBLE else View.GONE
                    commentsRecyclerView.visibility = if (commentsList.isEmpty()) View.GONE else View.VISIBLE
                }
            }
    }




    private fun showAddCommentDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add Comment")

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        builder.setView(input)

        builder.setPositiveButton("Submit") { dialog, which ->
            val commentText = input.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addComment(commentText)
            } else {
                Toast.makeText(requireContext(), "Comment cannot be empty", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun addComment(commentText: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val userName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Anonymous"
        val product = viewModel.product.value
        val productId = product?.firebaseId ?: return

        val comment = Comment(
            userId = userId,
            userName = userName,
            content = commentText,
            timestamp = System.currentTimeMillis()
        )

        // Evitar duplicados en la lista local
        if (commentsList.none { it.isSameAs(comment) }) {
            commentsList.add(0, comment)
            commentAdapter.setCommentList(commentsList)
        }

        if (hasInternetConnection()) {
            val db = FirebaseFirestore.getInstance()
            val commentsRef = db.collection("Products").document(productId).collection("Comments")
            commentsRef.add(comment)
                .addOnSuccessListener {
                    incrementCommentCount(productId)
                }
                .addOnFailureListener {
                    LocalStorageManager.addPendingComment(requireContext(), productId, comment)
                    scheduleCommentUploadWorker(requireContext())
                }
        } else {
            LocalStorageManager.addPendingComment(requireContext(), productId, comment)
            scheduleCommentUploadWorker(requireContext())
            Toast.makeText(requireContext(), "No Internet. Comment saved locally.", Toast.LENGTH_SHORT).show()
        }
    }




    private fun incrementCommentCount(productId: String) {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection("Products").document(productId)
        productRef.update("number_comments", FieldValue.increment(1))
    }

    private fun uploadPendingComments() {
        if (!hasInternetConnection()) return

        val pendingComments = LocalStorageManager.getPendingComments(requireContext())
        val product = viewModel.product.value
        val productId = product?.firebaseId ?: return
        val commentsForProduct = pendingComments[productId] ?: return

        val db = FirebaseFirestore.getInstance()
        val commentsRef = db.collection("Products").document(productId).collection("Comments")

        for (comment in commentsForProduct) {
            commentsRef.add(comment)
                .addOnSuccessListener {
                    incrementCommentCount(productId)
                    LocalStorageManager.removePendingComment(requireContext(), productId, comment)
                    Log.d("ProductDetailFragment", "Uploaded pending comment: $comment")
                }
                .addOnFailureListener { e ->
                    Log.e("ProductDetailFragment", "Failed to upload comment: $comment", e)
                }
        }
    }

    private fun scheduleCommentUploadWorker(context: Context) {
        // Configurar restricciones para que solo se ejecute con conexión
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Crear una solicitud de trabajo única
        val uploadWorkRequest = OneTimeWorkRequest.Builder(CommentUploadWorker::class.java)
            .setConstraints(constraints) // Asignar restricciones
            .build()

        // Encolar la tarea en WorkManager
        WorkManager.getInstance(context).enqueue(uploadWorkRequest)
    }




}

