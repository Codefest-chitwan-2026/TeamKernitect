package com.kernitect.saharaandroid.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernitect.saharaandroid.R

@Composable
fun SaharaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    unreadCount: Int = 0,
    onNotificationClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {

        /*
         * Sahara logo
         */
        Image(
            painter = painterResource(
                id = R.drawable.saharalogo
            ),
            contentDescription = "Sahara logo",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(50.dp)
        )

        /*
         * Screen title
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
         * Notification bell + badge
         */
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(50.dp)
        ) {

            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(50.dp)
            ) {

                Image(
                    painter = painterResource(
                        id = R.drawable.bell
                    ),
                    contentDescription = "Notifications",
                    modifier = Modifier.size(25.dp)
                )
            }

            /*
             * Unread counter badge
             */
            if (unreadCount > 0) {

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(
                            x = (-2).dp,
                            y = 3.dp
                        )
                        .sizeIn(
                            minWidth = 18.dp,
                            minHeight = 18.dp
                        ),
                    shape = CircleShape,
                    color = Color(0xFFE60000)
                ) {

                    Box(
                        modifier = Modifier.padding(
                            horizontal = 4.dp
                        ),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text =
                                if (unreadCount > 99) {
                                    "99+"
                                } else {
                                    unreadCount.toString()
                                },
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}