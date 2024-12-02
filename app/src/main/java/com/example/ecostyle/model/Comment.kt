// Comment.kt
package com.example.ecostyle.model
data class Comment(
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val timestamp: Long = 0L
) {
    fun isSameAs(other: Comment): Boolean {
        return this.userId == other.userId && this.timestamp == other.timestamp
    }
}
