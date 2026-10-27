# GATEALL

App Android de varredura de rede local e acesso remoto a arquivos.

## Camadas de acesso

| Camada | Alvo | Requisito no alvo |
|---|---|---|
| ADB over WiFi | Android | Depuracao USB ligada 1x |
| Agente GATEALL | Android | Nada (empurrado via ADB) |
| SMB | Windows / macOS / iOS / Linux | Pasta compartilhada |

## Como funciona

1. Escaneia a rede /24 via ICMP + TCP e le a tabela ARP para identificar fabricante.
2. Identifica o SO pelas portas abertas (5555=Android, 62078=iOS, 445=Windows).
3. Em Android, instala o agente em `/data/local/tmp` via ADB — sem app, sem icone,
   sem notificacao no aparelho alvo.
4. O agente e um servidor HTTP com `/ping`, `/list` e `/file`
   (com suporte a Range, entao video da seek).
5. Download sempre cai em `Documentos/GATEALL DOWNLOAD/`.

## Estrutura

- `app/src/main/java/com/gateall/app/net/` — varredura, ADB, agente, SMB
- `app/src/main/java/com/gateall/app/fs/` — interface unificada de sistema de arquivos
- `app/src/main/java/com/gateall/app/ui/` — Compose
- `app/src/main/assets/agent/` — fonte Go do agente

## Compilar o agente manualmente
