package com.example.ecostyle.model

import android.os.Parcel
import android.os.Parcelable

data class CartItem(
    var id: String = "",  // ID del documento en 'carts'
    val firebaseId: String = "",  // ID del producto en 'Products'
    val productName: String = "",
    val productPrice: String = "",
    val productImage: String = "",
    var quantity: Int = 1
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(firebaseId)
        parcel.writeString(productName)
        parcel.writeString(productPrice)
        parcel.writeString(productImage)
        parcel.writeInt(quantity)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<CartItem> {
        override fun createFromParcel(parcel: Parcel): CartItem = CartItem(parcel)
        override fun newArray(size: Int): Array<CartItem?> = arrayOfNulls(size)
    }
}

