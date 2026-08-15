package com.kernitect.saharaandroid.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.kernitect.saharaandroid.R


@Composable
fun SaharaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNotificationClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {

        /*
         * Temporary text logo.
         * We'll replace this with your actual Sahara logo asset.
         */
        Image(
            painter = painterResource(id = R.drawable.saharalogo),
            contentDescription = "Sahara logo",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(50.dp)
        )

        /*
         * Screen title.
         */
        Text(
            text = title,
            modifier = Modifier.align(
                Alignment.Center
            ),
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        /*
         * Notification button.
         */
        IconButton(
            onClick = onNotificationClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(50.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.bell),
                contentDescription = "Notifications",
                modifier = Modifier.size(25.dp)
            )
        }
    }
}