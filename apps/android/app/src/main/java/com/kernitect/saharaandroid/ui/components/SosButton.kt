package com.kernitect.saharaandroid.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        Button(
            onClick = onClick,
            modifier = Modifier.size(76.dp),
            shape = CircleShape,

            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF0000),
                contentColor = Color.Black
            ),

            border = BorderStroke(
                width = 4.dp,
                color = Color.White
            )
        ) {

            Text(
                text = "SOS",
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
        }
    }
}