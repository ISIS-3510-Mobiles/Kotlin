package com.example.ecostyle.model;

import android.widget.Button
import android.widget.TextView

data class Profile(
    val email: String,
    val provider: String,
    val emailTextView: TextView,
    val providerTextView: TextView,
    val logOutButton: Button
)
