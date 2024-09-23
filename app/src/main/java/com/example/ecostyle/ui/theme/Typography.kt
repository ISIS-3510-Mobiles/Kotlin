package com.example.ecostyle.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.ecostyle.R

// Family Definition
val UbuntuFontFamily = FontFamily(
    Font(R.font.ubuntu_regular, FontWeight.Normal),
    Font(R.font.ubuntu_bold, FontWeight.Bold)
)

// Typography type Ecostyle
val EcoStyleTypography = Typography(
    h6 = androidx.compose.ui.text.TextStyle(
        fontFamily = UbuntuFontFamily,
        fontWeight = FontWeight.Bold
    ),
    body1 = androidx.compose.ui.text.TextStyle(
        fontFamily = UbuntuFontFamily,
        fontWeight = FontWeight.Normal
    ),
    button = androidx.compose.ui.text.TextStyle(
        fontFamily = UbuntuFontFamily,
        fontWeight = FontWeight.Bold
    )
)