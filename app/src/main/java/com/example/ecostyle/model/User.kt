package com.example.ecostyle.model

data class User( var id:String?,
                 var imgUrl: String?,
                 var name: String,
                 var adress: String,
                 var number: String? ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}
