package com.gateall.app.vm

import android.app.Application
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gateall.app.data.Device
import com.gateall.app.data.DeviceStore
import com.gateall.app.data.DownloadStore
import com.gateall.app.data.Protocol
import com.gateall.app.data.RemoteEntry
import com.gateall.app.data.SavedTarget
import com.gateall.app.fs.FsFactory
import com.gateall.app.fs.RemoteFs
import com.gateall.app.net.AgentInstaller
import com.gateall.app.net.DeviceIdentifier
import com.gateall.app.net.NetworkScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val scanner = NetworkScanner()
    private val identifier = DeviceIdentifier(scanner)
    private val store = DeviceStore(app)
    private val installer = AgentInstaller(app)
    private val deviceLock = Any()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _saved = MutableStateFlow(store.all())
    val saved: StateFlow<List<SavedTarget>> = _saved.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _networkName = MutableStateFlow<String?>(null)
    val networkName: StateFlow<String?> = _networkName.asStateFlow()

    private val _localIp = MutableStateFlow<String?>(null)
    val localIp: StateFlow<String?> = _localIp.asStateFlow()

    private val _session = MutableStateFlow<RemoteFs?>(null)
    val session: StateFlow<RemoteFs?> = _session.asStateFlow()

    private val _path = MutableStateFlow("")
    val path: StateFlow<String> = _path.asStateFlow()

    private val _entries = MutableStateFlow<List<RemoteEntry>>(emptyList())
    val entries: StateFlow<List<RemoteEntry>> = _entries.asStateFlow()

    private val _downloads = MutableStateFlow(DownloadStore.listing())
    val downloads: StateFlow<List<File>> = _downloads.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    init {
        refreshNetworkInfo()
    }

    fun refreshNetworkInfo() {
        _localIp.value = scanner.localIpv4()
        _networkName.value = readSsid()
    }

    @Suppress("DEPRECATION")
    private fun readSsid(): String? {
        val wm = getApplication<Application>()
            .getSystemService(Application.WIFI_SERVICE) as? WifiManager
        val ssid = wm?.connectionInfo?.ssid?.trim('"')
        return ssid?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
    }

    fun scan() {
        if (_scanning.value) return

        viewModelScope.launch {
            _scanning.value = true
            _status.value = "Lendo a rede..."
            refreshNetworkInfo()

            val ip = _localIp.value
            if (ip == null) {
                _status.value = "Sem WiFi"
                _scanning.value = false
                return@launch
            }

            val prefix = scanner.prefix24(ip)
            _devices.value = emptyList()
            _status.value = "Varrendo $prefix.0/24 ..."

            val found = scanner.sweep(prefix) { hostIp ->
                val partial = Device(ip = hostIp, label = "Identificando...")
                synchronized(deviceLock) {
                    _devices.value = (_devices.value.filterNot { it.ip == hostIp } + partial)
                        .sortedBy { d -> d.ip.split(".").last().toIntOrNull() ?: 0 }
                }
            }

            _status.value = "Identificando ${found.size} aparelho(s)..."
            val arp = scanner.arpTable()

            val identified = found
                .map { hostIp -> identifier.identify(hostIp, arp[hostIp]) }
                .filter { it.ip != ip }

            _devices.value = identified
            _status.value = "${identified.size} aparelho(s) na rede"
            _scanning.value = false
        }
    }

    fun installAgent(device: Device) {
        viewModelScope.launch {
            _status.value = "Instalando porta GATEALL em ${device.ip}..."

            val ok = installer.install(device.ip)

            _status.value = if (ok) {
                store.save(
                    SavedTarget(
                        device = device.copy(
                            protocol = Protocol.AGENT,
                            agentPort = AgentInstaller.PORT
                        ),
                        protocol = Protocol.AGENT,
                        agentPort = AgentInstaller.PORT
                    )
                )
                _saved.value = store.all()
                "Porta instalada em ${device.label}"
            } else {
                "Falha. Depuração USB ligada e aparelho autorizado?"
            }

            scan()
        }
    }

    fun removeAgent(device: Device) {
        viewModelScope.launch {
            installer.uninstall(device.ip)
            store.remove(device.key)
            _saved.value = store.all()
            scan()
        }
    }

    fun saveTarget(target: SavedTarget) {
        store.save(target)
        _saved.value = store.all()
    }

    fun forget(key: String) {
        store.remove(key)
        _saved.value = store.all()
    }

    fun openDevice(
        device: Device,
        user: String? = null,
        password: String? = null,
        share: String? = null
    ) {
        viewModelScope.launch {
            _status.value = "Conectando em ${device.ip}..."

            val target = SavedTarget(
                device = device,
                protocol = device.protocol,
                user = user,
                password = password,
                share = share,
                agentPort = device.agentPort
            )

            val fs = FsFactory.open(target)
            if (fs == null) {
                _status.value = "Sem protocolo suportado"
                return@launch
            }

            _session.value = fs
            store.save(target)
            _saved.value = store.all()

            val start = if (fs.protocol == Protocol.SMB && share != null) {
                "smb://${device.ip}/$share"
            } else {
                fs.rootPath
            }

            navigate(start)
        }
    }

    fun navigate(path: String) {
        val fs = _session.value ?: return

        viewModelScope.launch {
            _path.value = path
            _status.value = "Abrindo $path"
            _entries.value = fs.list(path)
            _status.value = ""
        }
    }

    fun up() {
        val fs = _session.value ?: return
        val current = _path.value
        if (current.isEmpty() || current == fs.rootPath) return

        val parent = current.trimEnd('/').substringBeforeLast('/', "")
        navigate(if (parent.length < fs.rootPath.length) fs.rootPath else parent)
    }

    fun closeSession() {
        _session.value?.close()
        _session.value = null
        _entries.value = emptyList()
        _path.value = ""
    }

    fun download(entry: RemoteEntry) {
        val fs = _session.value ?: return

        viewModelScope.launch {
            val dest = DownloadStore.unique(File(DownloadStore.root(), entry.name))
            _progress.value = 0f
            _status.value = "Baixando ${entry.name}..."

            val ok = fs.download(entry.path, dest) { done, total ->
                _progress.value = if (total > 0) done.toFloat() / total else 0f
            }

            _status.value = if (ok) {
                "Salvo em GATEALL DOWNLOAD/${entry.name}"
            } else {
                "Falha no download"
            }

            _progress.value = 0f
            _downloads.value = DownloadStore.listing()
        }
    }

    fun deleteDownload(file: File) {
        runCatching { file.delete() }
        _downloads.value = DownloadStore.listing()
    }

    override fun onCleared() {
        _session.value?.close()
        super.onCleared()
    }
}
