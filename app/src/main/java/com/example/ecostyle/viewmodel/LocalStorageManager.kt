package com.example.ecostyle.utils

import android.content.Context
import com.example.ecostyle.model.Comment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


object LocalStorageManager {

    private const val PREFS_NAME = "EcostylePrefs"
    private const val KEY_LIKED_PRODUCTS = "LikedProducts"
    private const val KEY_PENDING_COMMENTS = "PendingComments"
    private val commentListeners = mutableListOf<(String, List<Comment>) -> Unit>()

    fun saveLikedProducts(context: Context, likedProductIds: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_LIKED_PRODUCTS, likedProductIds).apply()
    }

    fun getLikedProducts(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_LIKED_PRODUCTS, emptySet()) ?: emptySet()
    }

    fun addLikedProduct(context: Context, productId: String) {
        val likedProducts = getLikedProducts(context).toMutableSet()
        likedProducts.add(productId)
        saveLikedProducts(context, likedProducts)
    }

    fun removeLikedProduct(context: Context, productId: String) {
        val likedProducts = getLikedProducts(context).toMutableSet()
        likedProducts.remove(productId)
        saveLikedProducts(context, likedProducts)
    }

    fun isProductLiked(context: Context, productId: String): Boolean {
        val likedProducts = getLikedProducts(context)
        return likedProducts.contains(productId)
    }

    fun addPendingComment(context: Context, productId: String, comment: Comment) {
        val pendingComments = getPendingComments(context).toMutableMap()
        val commentsForProduct = pendingComments[productId]?.toMutableList() ?: mutableListOf()
        if (commentsForProduct.none { it.isSameAs(comment) }) {
            commentsForProduct.add(comment)
            pendingComments[productId] = commentsForProduct
            savePendingComments(context, pendingComments)
            notifyCommentListeners(productId, commentsForProduct)
        }
    }

    fun getPendingComments(context: Context): Map<String, List<Comment>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PENDING_COMMENTS, null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, List<Comment>>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyMap()
        }
    }

    private fun savePendingComments(context: Context, pendingComments: Map<String, List<Comment>>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(pendingComments)
        prefs.edit().putString(KEY_PENDING_COMMENTS, json).apply()
    }

    fun addCommentListener(listener: (String, List<Comment>) -> Unit) {
        commentListeners.add(listener)
    }

    fun removeCommentListener(listener: (String, List<Comment>) -> Unit) {
        commentListeners.remove(listener)
    }

    private fun notifyCommentListeners(productId: String, comments: List<Comment>) {
        commentListeners.forEach { listener ->
            listener(productId, comments)
        }
    }


    fun removePendingComment(context: Context, productId: String, comment: Comment) {
        val pendingComments = getPendingComments(context).toMutableMap()
        val commentsForProduct = pendingComments[productId]?.toMutableList()
        commentsForProduct?.remove(comment)
        if (commentsForProduct.isNullOrEmpty()) {
            pendingComments.remove(productId)
        } else {
            pendingComments[productId] = commentsForProduct
        }
        savePendingComments(context, pendingComments)
    }


}
