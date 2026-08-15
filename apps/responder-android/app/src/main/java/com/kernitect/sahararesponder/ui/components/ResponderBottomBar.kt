package com.kernitect.sahararesponder.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.kernitect.sahararesponder.ui.navigation.ResponderDestination

@Composable
fun ResponderBottomBar(current: ResponderDestination, onSelect: (ResponderDestination) -> Unit, onSecurity: () -> Unit) {
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
        NavigationBarItem(
            selected = false,
            onClick = onSecurity,
            icon = { Icon(Icons.Filled.Lock, contentDescription = "Security") },
            label = { Text("Security") },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityBottomSheet(
    offlinePinConfigured: Boolean,
    onDismiss: () -> Unit,
    onSetOfflinePin: () -> Unit,
    onLockApp: () -> Unit,
    onRemoveAccount: () -> Unit,
) {
    var confirmLock by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ACCOUNT SECURITY", style = MaterialTheme.typography.titleLarge)
            if (!offlinePinConfigured) OutlinedButton(onClick = onSetOfflinePin, modifier = Modifier.fillMaxWidth()) { Text("SET OFFLINE PIN") }
            Button(onClick = { if (offlinePinConfigured) confirmLock = true else onSetOfflinePin() }, modifier = Modifier.fillMaxWidth()) { Text("LOCK RESPONDER APP") }
            TextButton(onClick = { confirmRemove = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("REMOVE ACCOUNT FROM THIS DEVICE") }
            Spacer(Modifier.height(12.dp))
        }
    }
    if (confirmLock) AlertDialog(onDismissRequest = { confirmLock = false }, title = { Text("Lock responder app?") }, text = { Text("Your verified team remains trusted on this device. You can unlock it again with your Offline Responder PIN.") }, dismissButton = { TextButton(onClick = { confirmLock = false }) { Text("CANCEL") } }, confirmButton = { Button(onClick = { confirmLock = false; onDismiss(); onLockApp() }) { Text("LOCK APP") } })
    if (confirmRemove) AlertDialog(onDismissRequest = { confirmRemove = false }, title = { Text("Remove verified responder account from this device?") }, text = { Text("Offline unlock will no longer be available. Internet will be required to sign in again. Rescue history will remain on this device.") }, dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("CANCEL") } }, confirmButton = { Button(onClick = { confirmRemove = false; onDismiss(); onRemoveAccount() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("REMOVE ACCOUNT") } })
}
