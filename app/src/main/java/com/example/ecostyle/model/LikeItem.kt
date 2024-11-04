// LikeItem.kt
package com.example.ecostyle.model

data class LikeItem(
    var id: String = "",
    var firebaseId: String = "",
    var productId: Int = -1,
    var productName: String = "",
    var productPrice: String = "",
    var productImage: String = "",
    var timestamp: Long = 0L
)
