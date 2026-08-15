package com.kernitect.sahararesponder.ui.screens.setup

import android.util.Patterns
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
import androidx.compose.ui.unit.dp
import com.kernitect.sahararesponder.model.ResponderRegistration
import com.kernitect.sahararesponder.ui.components.CriticalRed

@Composable
fun ResponderRegistrationScreen(
    deviceId: String,
    initial: ResponderRegistration?,
    loading: Boolean,
    error: String?,
    onSubmit: (ResponderRegistration) -> Unit,
) {
    var operatorName by remember { mutableStateOf(initial?.operatorName.orEmpty()) }
    var organization by remember { mutableStateOf(initial?.organization.orEmpty()) }
    var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }
    var email by remember { mutableStateOf(initial?.email.orEmpty()) }
    var district by remember { mutableStateOf(initial?.district ?: "Chitwan") }
    var validation by remember { mutableStateOf<String?>(null) }
    OnboardingFrame("Responder Registration", "Responder access must be approved by the SAHARA command administrator.") {
        OutlinedTextField(operatorName, { operatorName = it }, Modifier.fillMaxWidth(), label = { Text("Operator Name") }, singleLine = true)
        OutlinedTextField(organization, { organization = it }, Modifier.fillMaxWidth(), label = { Text("Organization") }, singleLine = true)
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Phone") }, singleLine = true)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email (optional)") }, singleLine = true)
        OutlinedTextField(district, { district = it }, Modifier.fillMaxWidth(), label = { Text("Requested District") }, singleLine = true)
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF3F4F6), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) { Text("DEVICE ID", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold); Text(deviceId, style = MaterialTheme.typography.bodySmall) }
        }
        (validation ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            enabled = !loading,
            onClick = {
                validation = when {
                    operatorName.isBlank() -> "Operator name is required."
                    organization.isBlank() -> "Organization is required."
                    phone.isBlank() -> "Phone is required."
                    district.isBlank() -> "District is required."
                    email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email address."
                    else -> null
                }
                if (validation == null) onSubmit(ResponderRegistration(deviceId, operatorName = operatorName.trim(), organization = organization.trim(), phone = phone.trim(), email = email.trim(), district = district.trim()))
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text(if (loading) "SUBMITTING…" else "SUBMIT REGISTRATION") }
    }
}

@Composable
fun PendingApprovalScreen(registration: ResponderRegistration, loading: Boolean, error: String?, onCheckStatus: () -> Unit) {
    OnboardingFrame("Registration submitted", "Waiting for administrator approval") {
        StatusCard("PENDING", Color(0xFFE17800))
        ProfileRow("Responder ID", registration.responderId ?: "Assigning…")
        ProfileRow("Organization", registration.organization)
        ProfileRow("District", registration.district)
        Text("A SAHARA command administrator must verify this device and assign an official rescue team.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = onCheckStatus, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "CHECKING…" else "CHECK STATUS") }
    }
}

@Composable
fun RejectedRegistrationScreen(registration: ResponderRegistration, loading: Boolean, error: String?, onRetry: () -> Unit, onCheckStatus: () -> Unit) {
    OnboardingFrame("Registration not approved", "This responder cannot enter operational mode.") {
        StatusCard("REJECTED", CriticalRed)
        Text(registration.rejectionReason ?: "Contact the SAHARA command administrator for assistance.")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = onRetry, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("UPDATE & RESUBMIT") }
        OutlinedButton(onClick = onCheckStatus, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("CHECK STATUS") }
    }
}

@Composable private fun OnboardingFrame(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("SAHARA RESPONDER", color = CriticalRed, fontWeight = FontWeight.Black)
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable private fun StatusCard(label: String, color: Color) { Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = .12f)) { Text(label, Modifier.fillMaxWidth().padding(16.dp), color = color, fontWeight = FontWeight.Black) } }
@Composable private fun ProfileRow(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.SemiBold) } }
