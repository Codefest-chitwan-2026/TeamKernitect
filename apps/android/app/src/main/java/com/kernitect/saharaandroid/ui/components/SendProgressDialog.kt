package com.kernitect.saharaandroid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

enum class SendProgressState {
    LOCATING,
    SEARCHING,
    SENDING,
    SUCCESS,
    ERROR
}

@Composable
fun SendProgressDialog(
    title: String,
    message: String,
    state: SendProgressState,
    onCancel: () -> Unit,
    onClose: () -> Unit
) {
    val canCancel =
        state == SendProgressState.LOCATING ||
                state == SendProgressState.SEARCHING

    Dialog(
        onDismissRequest = {
            if (canCancel) {
                onCancel()
            }
        }
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 32.dp,
                    vertical = 28.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                when (state) {

                    SendProgressState.SUCCESS -> {
                        Text(
                            text = "✓",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF179447)
                        )
                    }

                    SendProgressState.ERROR -> {
                        Text(
                            text = "!",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE60000)
                        )
                    }

                    else -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(52.dp),
                            color = Color(0xFFE60000),
                            strokeWidth = 5.dp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = Color(0xFF555555),
                    textAlign = TextAlign.Center
                )

                if (canCancel) {

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    TextButton(
                        onClick = onCancel
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFFE60000),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (state == SendProgressState.ERROR) {

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    TextButton(
                        onClick = onClose
                    ) {
                        Text(
                            text = "Close",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}