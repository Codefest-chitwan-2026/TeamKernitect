package com.kernitect.saharaandroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SosButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(85.dp)
            .background(
                color = Color.White,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .size(70.dp)
                .background(
                    color = Color(0xFFFF0000),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "SOS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}