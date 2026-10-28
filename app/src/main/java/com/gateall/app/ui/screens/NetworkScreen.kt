package com.gateall.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    var selecionado by remember { mutableStateOf<Device?>(null) }
    var manual by remember { mutableStateOf(false) }
    var ipManual by remember { mutableStateOf("") }

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
            OutlinedButton(onClick = { manual = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("IP")
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
                DeviceCard(device = device, onClick = { selecionado = device })
            }
        }
    }

    // ---- Menu de acoes do dispositivo ----
    selecionado?.let { device ->
        AlertDialog(
            onDismissRequest = { selecionado = null },
            title = { Text(device.label) },
            text = {
                Column {
                    Text(
                        device.ip + (device.mac?.let { "  ·  $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (device.openPorts.isNotEmpty()) {
                        Text(
                            "portas abertas: " + device.openPorts.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (device.protocol == Protocol.AGENT)
                            "Porta GATEALL ja instalada."
                        else
                            "Toque em INVADIR para conectar, instalar a porta e abrir as pastas.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.invade(device)
                    selecionado = null
                }) {
                    Text(if (device.protocol == Protocol.AGENT) "ABRIR" else "INVADIR")
                }
            },
            dismissButton = {
                Column {
                    if (device.protocol != Protocol.AGENT) {
                        TextButton(onClick = {
                            vm.installAgent(device)
                            selecionado = null
                        }) { Text("So instalar a porta") }
                    } else {
                        TextButton(onClick = {
                            vm.removeAgent(device)
                            selecionado = null
                        }) { Text("Remover porta") }
                    }

                    if (device.protocol == Protocol.ADB || device.protocol == Protocol.AGENT) {
                        TextButton(onClick = {
                            vm.openDevice(device)
                            selecionado = null
                        }) { Text("Abrir pastas") }
                    }

                    TextButton(onClick = { selecionado = null }) { Text("Cancelar") }
                }
            }
        )
    }

    // ---- Adicionar por IP ----
    if (manual) {
        AlertDialog(
            onDismissRequest = { manual = false },
            title = { Text("Invadir por IP") },
            text = {
                Column {
                    Text(
                        "Digite o IP do aparelho alvo.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ipManual,
                        onValueChange = { ipManual = it },
                        singleLine = true,
                        placeholder = { Text("192.168.0.10") }
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = ipManual.count { it == '.' } == 3,
                    onClick = {
                        val d = Device(ip = ipManual.trim(), label = ipManual.trim())
                        manual = false
                        ipManual = ""
                        vm.invade(d)
                    }
                ) { Text("INVADIR") }
            },
            dismissButton = {
                TextButton(onClick = { manual = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun DeviceCard(device: Device, onClick: () -> Unit) {

    val icon = when (device.os) {
        OsKind.ANDROID -> Icons.Default.PhoneAndroid
        OsKind.IOS -> Icons.Default.PhoneIphone
        OsKind.WINDOWS -> Icons.Default.DesktopWindows
        OsKind.MAC -> Icons.Default.LaptopMac
        OsKind.LINUX -> Icons.Default.Computer
        OsKind.UNKNOWN -> Icons.Default.DevicesOther
    }

    val etiqueta = when (device.protocol) {
        Protocol.AGENT -> "PORTA INSTALADA"
        Protocol.ADB -> "ADB ABERTO"
        Protocol.SMB -> "SMB"
        Protocol.NONE -> "TOQUE PARA INVADIR"
    }

    val cor = when (device.protocol) {
        Protocol.AGENT -> MaterialTheme.colorScheme.primary
        Protocol.ADB -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Card(Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = cor)
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
                Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = cor)
            }

            Text("›", style = MaterialTheme.typography.headlineSmall, color = cor)
        }
    }
}
