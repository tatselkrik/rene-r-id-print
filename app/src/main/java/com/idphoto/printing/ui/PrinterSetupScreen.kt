package com.idphoto.printing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idphoto.printing.print.DirectIppPrinter
import com.idphoto.printing.print.DirectPrinterSettings
import com.idphoto.printing.print.EpsonPrinterDiscovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PrinterSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf(DirectPrinterSettings.load(context)) }
    var address by remember { mutableStateOf(profile?.addressLabel.orEmpty()) }
    var isBusy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun startWork(block: suspend () -> String) {
        scope.launch {
            isBusy = true
            errorMessage = null
            statusMessage = null
            try {
                statusMessage = block()
            } catch (error: Exception) {
                errorMessage = error.message ?: "The printer setup could not be completed."
            } finally {
                isBusy = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasCream)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = "Direct Printer Setup",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Set this up once. Afterward, Print sends the sheet straight to the L15150.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = WarmWhite,
                shadowElevation = 2.dp,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        when {
                            profile == null -> "No Direct Printer Saved"
                            profile?.readyForDirectPrint == true -> "Direct Printer Ready"
                            profile?.certificateSha256.isNullOrBlank() -> "Secure Reconnect Required"
                            else -> "Compatible Direct Format Not Found"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        textAlign = TextAlign.Center,
                    )
                    profile?.let {
                        Text(
                            it.displayName,
                            modifier = Modifier.fillMaxWidth(),
                            color = Pine,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            it.addressLabel,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            if (it.readyForDirectPrint && it.useTls) {
                                "Connection: Encrypted IPPS"
                            } else if (it.readyForDirectPrint) {
                                "Connection: IPP"
                            } else {
                                "Tap Connect Using This Address once to save the secure connection."
                            },
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                            textAlign = TextAlign.Center,
                        )
                        if (it.readyForDirectPrint) {
                            Text(
                                "Direct format: high-resolution JPEG",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Text(
                            "Paper: Cassette 1 - RC Woven matte photo",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Size correction: automatic for L15150 / RC Woven matte",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Button(
                onClick = {
                    startWork {
                        val found = EpsonPrinterDiscovery.find(context)
                        address = found.probeAddress
                        val connected = withContext(Dispatchers.IO) {
                            DirectIppPrinter.probe(found.probeAddress)
                        }
                        DirectPrinterSettings.save(context, connected)
                        profile = connected
                        if (connected.useTls) {
                            "Connected securely to ${connected.displayName}."
                        } else {
                            "Connected to ${connected.displayName}."
                        }
                    }
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = "Find Printer Automatically",
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                )
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy,
                singleLine = true,
                label = { Text("Printer IP address") },
                supportingText = { Text("Example: 192.168.1.25") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            OutlinedButton(
                onClick = {
                    startWork {
                        val connected = withContext(Dispatchers.IO) {
                            DirectIppPrinter.probe(address)
                        }
                        DirectPrinterSettings.save(context, connected)
                        profile = connected
                        address = connected.addressLabel
                        if (connected.useTls) {
                            "Connected securely to ${connected.displayName}."
                        } else {
                            "Connected to ${connected.displayName}."
                        }
                    }
                },
                enabled = !isBusy && address.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Connect Using This Address")
            }

            statusMessage?.let { MessageCard(it, isError = false) }
            errorMessage?.let { MessageCard(it, isError = true) }
            if (isBusy) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        OutlinedButton(
            onClick = onBack,
            enabled = !isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun MessageCard(message: String, isError: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            textAlign = TextAlign.Center,
        )
    }
}
