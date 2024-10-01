package com.example.ecostyle.model

class Product
    (
    val id: Int,
    val name: String,
    val price: String,
    val imageResource: Int,
    val description: String,
    var isFavorite: Boolean = false
)
