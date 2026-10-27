package com.gateall.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gateall.app.data.Device
import com.gateall.app.data.OsKind
import com.gateall.app.data.Protocol
import com.gateall.app.vm.MainViewModel

@Composable
fun NetworkScreen(vm: MainViewModel, onOpen: (Device) -> Unit) {

    val devices by vm.devices.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val status by vm.status.collectAsState()
    val ssid by vm.networkName.collectAsState()
    val localIp by vm.localIp.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Text(
            "REDE ATUAL",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            ssid ?: "WiFi sem nome visivel",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Seu IP: ${localIp ?: "-"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.scan() }, enabled = !scanning) {
                Icon(Icons.Default.Radar, null)
                Spacer(Modifier.width(6.dp))
                Text(if (scanning) "Varrendo..." else "ESCANEAR")
            }
            if (scanning) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }

        if (status.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(devices, key = { it.ip }) { device ->
                DeviceCard(
                    device = device,
                    onOpen = { onOpen(device) },
                    onInstall = { vm.installAgent(device) },
                    onRemove = { vm.removeAgent(device) }
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: Device,
    onOpen: () -> Unit,
    onInstall: () -> Unit,
    onRemove: () -> Unit
) {
    val icon = when (device.os) {
        OsKind.ANDROID -> Icons.Default.PhoneAndroid
        OsKind.IOS -> Icons.Default.PhoneIphone
        OsKind.WINDOWS -> Icons.Default.DesktopWindows
        OsKind.MAC -> Icons.Default.LaptopMac
        OsKind.LINUX -> Icons.Default.Computer
        OsKind.UNKNOWN -> Icons.Default.DevicesOther
    }

    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(device.label, fontWeight = FontWeight.SemiBold)
                Text(
                    buildString {
                        append(device.ip)
                        device.mac?.let { append("  ·  $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (device.openPorts.isNotEmpty()) {
                    Text(
                        "portas: " + device.openPorts.joinToString(","),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }

            when (device.protocol) {
                Protocol.AGENT -> {
                    AssistChip(onClick = onOpen, label = { Text("ABRIR") })
                    IconButton(onClick = onRemove) { Icon(Icons.Default.Close, "remover") }
                }

                Protocol.ADB -> {
                    AssistChip(
                        onClick = onInstall,
                        label = { Text("INSTALAR PORTA") },
                        leadingIcon = { Icon(Icons.Default.Download, null, Modifier.size(16.dp)) }
                    )
                    IconButton(onClick = onOpen) { Icon(Icons.Default.Folder, "abrir") }
                }

                Protocol.SMB -> AssistChip(onClick = onOpen, label = { Text("ABRIR SMB") })

                Protocol.NONE -> Text(
                    "-",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}
