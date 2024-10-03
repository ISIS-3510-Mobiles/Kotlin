package com.example.ecostyle.model

data class Product(
    val id: Int? = null,
    val name: String? = null,
    val price: String? = null,
    val imageResource: String? = null,
    val description: String? = null,
    var isFavorite: Boolean? = null
)