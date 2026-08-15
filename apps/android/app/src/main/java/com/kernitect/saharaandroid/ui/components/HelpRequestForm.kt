package com.kernitect.saharaandroid.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HelpRequestForm(
    modifier: Modifier = Modifier,
    onSend: (
        disasterType: String,
        peopleCount: String,
        explanation: String
    ) -> Unit = { _, _, _ -> }
) {
    var disasterType by remember {
        mutableStateOf("")
    }

    var peopleCount by remember {
        mutableStateOf("")
    }

    var explanation by remember {
        mutableStateOf("")
    }

    val disasterOptions = listOf(
        "Earthquake",
        "Flood",
        "Fire",
        "Landslide",
        "Medical",
        "Other"
    )

    val peopleOptions = listOf(
        "1",
        "2 - 5",
        "6 - 10",
        "11 - 20",
        "20+"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFF777777)
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 14.dp
            )
        ) {

            Text(
                text = "Ask for help.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            FormDropdown(
                modifier = Modifier.padding(top = 14.dp),
                placeholder = "Type of disaster",
                selectedValue = disasterType,
                options = disasterOptions,
                onSelected = {
                    disasterType = it
                }
            )

            FormDropdown(
                modifier = Modifier.padding(top = 12.dp),
                placeholder = "Number of people",
                selectedValue = peopleCount,
                options = peopleOptions,
                onSelected = {
                    peopleCount = it
                }
            )

            Text(
                text = "Explain (Optional)",
                modifier = Modifier.padding(
                    top = 14.dp,
                    bottom = 7.dp
                ),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            TextField(
                value = explanation,
                onValueChange = {
                    explanation = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
                placeholder = {
                    Text(
                        text = "Type here...",
                        fontSize = 11.sp,
                        color = Color(0xFF888888)
                    )
                },
                shape = RoundedCornerShape(7.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFE4E4E4),
                    unfocusedContainerColor = Color(0xFFE4E4E4),
                    disabledContainerColor = Color(0xFFE4E4E4),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {

                    Button(
                        onClick = {
                            onSend(
                                disasterType,
                                peopleCount,
                                explanation
                            )
                        },
                        enabled =
                            disasterType.isNotBlank() &&
                                    peopleCount.isNotBlank(),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFE0E0),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFFFFEAEA),
                            disabledContentColor = Color(0xFF999999)
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 26.dp,
                            vertical = 5.dp
                        )
                    ) {

                        Text(
                            text = "Send",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormDropdown(
    placeholder: String,
    selectedValue: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clickable {
                    expanded = true
                },
            shape = RoundedCornerShape(7.dp),
            color = Color(0xFFE4E4E4)
        ) {

            Row(
                modifier = Modifier.padding(
                    horizontal = 13.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text =
                        if (selectedValue.isBlank()) {
                            placeholder
                        } else {
                            selectedValue
                        },
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    color =
                        if (selectedValue.isBlank()) {
                            Color(0xFF777777)
                        } else {
                            Color.Black
                        }
                )

                Text(
                    text = "▼",
                    fontSize = 9.sp,
                    color = Color.Black
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {

            options.forEach { option ->

                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}