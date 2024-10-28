package com.example.ecostyle.model

data class CartItem(
    var id: String = "",  // ID del documento en 'carts'
    val firebaseId: String = "",  // ID del producto en 'Products'
    val productName: String = "",
    val productPrice: String = "",
    val productImage: String = "",
    var quantity: Int = 1
)
