package com.gateall.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gateall.app.data.RemoteEntry
import com.gateall.app.ui.humanSize
import com.gateall.app.vm.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(vm: MainViewModel, onPreview: (RemoteEntry) -> Unit) {

    val fs by vm.session.collectAsState()
    val path by vm.path.collectAsState()
    val entries by vm.entries.collectAsState()
    val status by vm.status.collectAsState()
    val progress by vm.progress.collectAsState()

    Column(Modifier.fillMaxSize()) {

        TopAppBar(
            title = {
                Column {
                    Text(
                        fs?.device?.label ?: "Dispositivo",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        path,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = { vm.up() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "voltar")
                }
            },
            actions = {
                IconButton(onClick = { vm.closeSession() }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, "sair")
                }
            }
        )

        if (progress > 0f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (status.isNotBlank()) {
            Text(
                status,
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        LazyColumn(Modifier.weight(1f)) {
            items(entries, key = { it.path }) { entry ->
                EntryRow(
                    entry = entry,
                    onClick = {
                        when {
                            entry.isDir -> vm.navigate(entry.path)
                            entry.isMedia -> onPreview(entry)
                            else -> vm.download(entry)
                        }
                    },
                    onDownload = { vm.download(entry) }
                )
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: RemoteEntry,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = {
            Text(entry.name, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Text(
                buildString {
                    append(if (entry.isDir) "pasta" else humanSize(entry.size))
                    entry.mime?.let { append("  ·  $it") }
                },
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingContent = {
            Icon(
                when {
                    entry.isDir -> Icons.Default.Folder
                    entry.isImage -> Icons.Default.Image
                    entry.isVideo -> Icons.Default.Movie
                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                },
                null,
                tint = if (entry.isDir) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                }
            )
        },
        trailingContent = {
            if (!entry.isDir) {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, "baixar")
                }
            }
        }
    )
}
