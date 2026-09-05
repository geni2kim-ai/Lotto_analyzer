package com.example.lottoinsight.feature.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LottoBall(
    number: Int,
    size: Dp = 36.dp
) {
    val ballColor = getLottoBallColor(number)
    val textColor = getLottoBallTextColor(number)

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(ballColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            color = textColor,
            fontSize = (size.value * 0.4).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LottoBallRow(
    numbers: List<Int>,
    ballSize: Dp = 36.dp
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        numbers.forEachIndexed { index, number ->
            LottoBall(number = number, size = ballSize)
            if (index < numbers.size - 1) {
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

fun getLottoBallTextColor(number: Int): Color {
    return if (number in 1..10) Color.Black else Color.White
}

fun getLottoBallColor(number: Int): Color {
    return when (number) {
        in 1..10 -> Color(0xFFFBC02D) // Yellow
        in 11..20 -> Color(0xFF1976D2) // Blue
        in 21..30 -> Color(0xFFD32F2F) // Red
        in 31..40 -> Color(0xFF616161) // Gray
        in 41..45 -> Color(0xFF388E3C) // Green
        else -> Color.Black
    }
}
