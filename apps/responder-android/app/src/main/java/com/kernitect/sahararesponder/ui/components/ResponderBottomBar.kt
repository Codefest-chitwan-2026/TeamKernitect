package com.kernitect.sahararesponder.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.kernitect.sahararesponder.ui.navigation.ResponderDestination

@Composable
fun ResponderBottomBar(current: ResponderDestination, onSelect: (ResponderDestination) -> Unit) {
    NavigationBar {
        ResponderDestination.entries.forEach { destination ->
            val icon = when (destination) {
                ResponderDestination.HOME -> Icons.Filled.Home
                ResponderDestination.MAP -> Icons.Filled.Map
                ResponderDestination.ACTIVE -> Icons.Filled.LocalFireDepartment
                ResponderDestination.NOTIFICATIONS -> Icons.Filled.Notifications
            }
            NavigationBarItem(
                selected = current == destination,
                onClick = { onSelect(destination) },
                icon = { Icon(icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
            )
        }
    }
}
