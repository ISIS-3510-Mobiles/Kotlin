package com.example.ecostyle.utils

import android.content.Context

object LocalStorageManager {

    private const val PREFS_NAME = "EcostylePrefs"
    private const val KEY_LIKED_PRODUCTS = "LikedProducts"

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
}
