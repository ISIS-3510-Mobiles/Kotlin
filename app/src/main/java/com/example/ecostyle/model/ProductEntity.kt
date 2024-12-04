package com.example.ecostyle.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val name: String?,
    val price: String?,
    val imageResource: String?,
    val description: String?,
    val ecoFriendly: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val quantity: Int,
    val brand: String?,
    val initialPrice: String?
)
