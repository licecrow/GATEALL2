package com.gateall.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gateall.app.data.DownloadStore
import com.gateall.app.ui.humanSize
import com.gateall.app.vm.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DownloadsScreen(vm: MainViewModel) {

    val files by vm.downloads.collectAsState()
    val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Text(
            "GATEALL DOWNLOAD",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Baixados",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            DownloadStore.root().absolutePath,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )

        Spacer(Modifier.height(12.dp))

        if (files.isEmpty()) {
            Text(
                "Nada baixado ainda.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(files, key = { it.absolutePath }) { f ->
                ListItem(
                    headlineContent = {
                        Text(f.name, fontWeight = FontWeight.Medium)
                    },
                    supportingContent = {
                        Text(
                            "${humanSize(f.length())}  ·  ${fmt.format(Date(f.lastModified()))}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Folder,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { vm.deleteDownload(f) }) {
                            Icon(Icons.Default.Delete, "apagar")
                        }
                    }
                )
            }
        }
    }
}
