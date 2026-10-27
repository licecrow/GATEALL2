package com.gateall.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gateall.app.data.RemoteEntry
import com.gateall.app.fs.PreviewCache
import com.gateall.app.ui.humanSize
import com.gateall.app.vm.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(vm: MainViewModel, entry: RemoteEntry, onBack: () -> Unit) {

    val context = LocalContext.current
    val fs by vm.session.collectAsState()

    var local by remember { mutableStateOf<File?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(entry.path) {
        val s = fs
        if (s == null) {
            loading = false
            return@LaunchedEffect
        }
        local = PreviewCache.ensure(context, s, entry.path)
        loading = false
    }

    Column(Modifier.fillMaxSize()) {

        TopAppBar(
            title = { Text(entry.name, style = MaterialTheme.typography.titleSmall) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "voltar")
                }
            },
            actions = {
                IconButton(onClick = { vm.download(entry) }) {
                    Icon(Icons.Default.Download, "baixar")
                }
            }
        )

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val file = local

            when {
                loading -> CircularProgressIndicator()

                file == null -> Text("Nao foi possivel carregar")

                entry.isImage -> AsyncImage(
                    model = ImageRequest.Builder(context).data(file).build(),
                    contentDescription = entry.name,
                    modifier = Modifier.fillMaxSize()
                )

                entry.isVideo -> {
                    val player = remember { ExoPlayer.Builder(context).build() }

                    DisposableEffect(file) {
                        player.setMediaItem(MediaItem.fromUri(file.toURI().toString()))
                        player.prepare()
                        onDispose { player.release() }
                    }

                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = true
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(humanSize(entry.size), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        entry.mime.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Button(onClick = { vm.download(entry) }) {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(8.dp))
                    Text("SALVAR NO CELULAR")
                }
            }
        }
    }
}
