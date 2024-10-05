package com.example.ecostyle.model

data class Product(
    val id: Int = -2,
    val name: String? = null,
    val price: String? = null,
    val imageResource: String? = null,
    val description: String? = null,
    var isFavorite: Boolean? = null,
    val quantity: Int = 0
)