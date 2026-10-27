package com.gateall.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gateall.app.data.RemoteEntry
import com.gateall.app.ui.screens.BrowserScreen
import com.gateall.app.ui.screens.DownloadsScreen
import com.gateall.app.ui.screens.MediaScreen
import com.gateall.app.ui.screens.NetworkScreen
import com.gateall.app.ui.screens.SavedScreen
import com.gateall.app.ui.theme.GateAllTheme
import com.gateall.app.vm.MainViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GateAllTheme { Root() }
        }
    }
}

private sealed interface Route {
    data object Tabs : Route
    data class Preview(val entry: RemoteEntry) : Route
}

@Composable
private fun Root() {

    val vm: MainViewModel = viewModel()
    var tab by remember { mutableIntStateOf(0) }
    var route by remember { mutableStateOf<Route>(Route.Tabs) }
    val session by vm.session.collectAsState()

    LaunchedEffect(session) {
        if (session != null) route = Route.Tabs
    }

    when (val r = route) {
        is Route.Preview -> MediaScreen(vm, r.entry) { route = Route.Tabs }

        Route.Tabs -> {
            if (session != null) {
                BrowserScreen(vm) { entry -> route = Route.Preview(entry) }
            } else {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab == 0,
                                onClick = { tab = 0 },
                                icon = { Icon(Icons.Default.Radar, null) },
                                label = { Text("Rede") }
                            )
                            NavigationBarItem(
                                selected = tab == 1,
                                onClick = { tab = 1 },
                                icon = { Icon(Icons.Default.Lock, null) },
                                label = { Text("Salvos") }
                            )
                            NavigationBarItem(
                                selected = tab == 2,
                                onClick = { tab = 2 },
                                icon = { Icon(Icons.Default.Download, null) },
                                label = { Text("Baixados") }
                            )
                        }
                    }
                ) { pad ->
                    Surface(Modifier.padding(pad)) {
                        when (tab) {
                            0 -> NetworkScreen(vm) { device -> vm.openDevice(device) }
                            1 -> SavedScreen(vm)
                            else -> DownloadsScreen(vm)
                        }
                    }
                }
            }
        }
    }
}
