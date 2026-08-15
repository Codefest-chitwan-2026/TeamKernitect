package com.kernitect.sahararesponder.ui.screens.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kernitect.sahararesponder.model.*
import com.kernitect.sahararesponder.ui.components.CriticalRed

@Composable
fun ResponderRegistrationScreen(deviceId: String, initial: ResponderRegistration?, loading: Boolean, error: String?, onSubmit: (ResponderRegistration) -> Unit) {
    var leaderName by remember { mutableStateOf(initial?.operatorName.orEmpty()) }; var teamName by remember { mutableStateOf(initial?.organization.orEmpty()) }
    var callsign by remember { mutableStateOf(initial?.callsign.orEmpty()) }; var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }
    var email by remember { mutableStateOf(initial?.email.orEmpty()) }; var district by remember { mutableStateOf(initial?.district ?: "Chitwan") }
    var password by remember { mutableStateOf("") }; var confirmPassword by remember { mutableStateOf("") }; var validation by remember { mutableStateOf<String?>(null) }
    OnboardingFrame("CREATE RESCUE TEAM", "Responder access requires SAHARA administrator verification.") {
        Text("RESCUE TEAM", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Field(teamName, { teamName = it }, "Team Name"); Field(callsign, { callsign = it }, "Callsign"); Field(district, { district = it }, "District")
        Text("TEAM LEADER", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Field(leaderName, { leaderName = it }, "Leader Name"); Field(phone, { phone = it }, "Phone"); Field(email, { email = it }, "Email")
        Field(password, { password = it }, "Password", true); Field(confirmPassword, { confirmPassword = it }, "Confirm Password", true)
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF3F4F6), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text("DEVICE ID", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold); Text(deviceId, style = MaterialTheme.typography.bodySmall) } }
        (validation ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(enabled = !loading, onClick = { validation = validateRegistration(RegistrationInput(teamName, callsign, district, leaderName, phone, email, password, confirmPassword)); if (validation == null) onSubmit(ResponderRegistration(deviceId, operatorName = leaderName.trim(), organization = teamName.trim(), phone = phone.trim(), email = email.trim(), district = district.trim(), teamName = teamName.trim(), callsign = callsign.trim(), password = password)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(if (loading) "SUBMITTING..." else "REGISTER TEAM") }
    }
}

@Composable
fun ResponderLoginScreen(deviceId: String, loading: Boolean, error: String?, onLogin: (String, String) -> Unit, onRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var validation by remember { mutableStateOf<String?>(null) }
    OnboardingFrame("SAHARA RESPONDER", "Team leader login") {
        Field(email, { email = it }, "Email"); Field(password, { password = it }, "Password", true)
        (validation ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { validation = if (email.isBlank() || password.isBlank()) "Email and password are required." else null; if (validation == null) onLogin(email.trim(), password) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "SIGNING IN..." else "LOGIN") }
        TextButton(onClick = onRegister, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Don't have a rescue team? REGISTER TEAM") }
        Text("Device: $deviceId", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun OfflinePinSetupScreen(teamName: String, onSave: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    OnboardingFrame("SET OFFLINE RESPONDER PIN", teamName) {
        Text("This PIN unlocks this previously verified responder device when internet is unavailable. It is not your account password.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        PinField(pin, { if (it.length <= 6 && it.all(Char::isDigit)) pin = it }, "Create a 6-digit PIN")
        PinField(confirm, { if (it.length <= 6 && it.all(Char::isDigit)) confirm = it }, "Confirm PIN")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { error = when { pin.length != 6 -> "Enter exactly 6 digits."; pin != confirm -> "PINs do not match."; else -> null }; if (error == null) onSave(pin) }, modifier = Modifier.fillMaxWidth()) { Text("SAVE OFFLINE PIN") }
    }
}

@Composable
fun OfflineUnlockScreen(teamName: String, callsign: String, error: String?, onUnlock: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    OnboardingFrame("OFFLINE DEVICE UNLOCK", "Previously verified team") {
        StatusCard("$teamName • $callsign", Color(0xFF16794A))
        PinField(pin, { if (it.length <= 6 && it.all(Char::isDigit)) pin = it }, "Offline Responder PIN")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { onUnlock(pin) }, enabled = pin.length == 6, modifier = Modifier.fillMaxWidth()) { Text("UNLOCK OFFLINE") }
        Text("Unlocks only this previously verified device. It does not authenticate with the backend.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun PendingApprovalScreen(registration: ResponderRegistration, loading: Boolean, error: String?, onCheckStatus: () -> Unit) { OnboardingFrame("Registration Submitted", "Your rescue team is awaiting administrator verification.") { StatusCard("PENDING", Color(0xFFE17800)); ProfileRow("Responder ID", registration.responderId ?: "Assigning..."); ProfileRow("Team", registration.teamName ?: registration.organization); ProfileRow("District", registration.district); error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; Button(onClick = onCheckStatus, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "CHECKING..." else "CHECK STATUS") } } }

@Composable fun RejectedRegistrationScreen(registration: ResponderRegistration, loading: Boolean, error: String?, onRetry: () -> Unit, onCheckStatus: () -> Unit) { OnboardingFrame("Registration not approved", "This responder cannot enter operational mode.") { StatusCard("REJECTED", CriticalRed); Text(registration.rejectionReason ?: "Contact the SAHARA command administrator for assistance."); error?.let { Text(it, color = MaterialTheme.colorScheme.error) }; Button(onClick = onRetry, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("UPDATE & RESUBMIT") }; OutlinedButton(onClick = onCheckStatus, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("CHECK STATUS") } } }

@Composable private fun Field(value: String, change: (String) -> Unit, label: String, secret: Boolean = false) = OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None)
@Composable private fun PinField(value: String, change: (String) -> Unit, label: String) = OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, visualTransformation = PasswordVisualTransformation())
@Composable private fun OnboardingFrame(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Text("SAHARA RESPONDER", color = CriticalRed, fontWeight = FontWeight.Black); Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant); content() } }
@Composable private fun StatusCard(label: String, color: Color) { Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = .12f)) { Text(label, Modifier.fillMaxWidth().padding(16.dp), color = color, fontWeight = FontWeight.Black) } }
@Composable private fun ProfileRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.SemiBold) } }
