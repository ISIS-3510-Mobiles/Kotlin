package com.example.ecostyle.model

data class Product(
    val id: Int = -2,
    val name: String? = null,
    val price: String? = null,
    val imageResource: String? = null,
    val description: String? = null,
    var isFavorite: Boolean = false,
    val ecofriend: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val quantity: Int = 0,
    val brand: String? = null,
    val initialPrice: String? = null,
    var firebaseId: String = "",
    val number_comments: Int = 0
)