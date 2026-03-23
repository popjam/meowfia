package com.meowfia.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MeowfiaTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = MeowfiaColors.TextPrimary
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MeowfiaColors.TextPrimary
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        color = MeowfiaColors.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = MeowfiaColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = MeowfiaColors.TextPrimary
    ),
    labelLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MeowfiaColors.TextOnPrimary
    ),
    labelSmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = MeowfiaColors.TextSecondary
    )
)
