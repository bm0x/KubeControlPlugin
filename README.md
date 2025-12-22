# KubeControlPlugin 🧩

**KubeControlPlugin** es el plugin "compañero" de Java para la herramienta de gestión **KubeControlMC (TUI)**.
Diseñado para **Minecraft 1.20 / 1.21**, sirve como puente entre el servidor, Discord y la interfaz de terminal.

## Características Principales

1.  **Integración con Discord Nativa**:
    *   Usa **Botones** de Discord para verificar usuarios (sin comandos complejos).
    *   Asigna roles de Discord automáticamente.
2.  **Sincronización de Economía**:
    *   Integra con **Vault** para leer balances de jugadores.
    *   Permite asignar roles de Discord basados en dinero ("Magnate", "VIP").
3.  **Bridge JSON**:
    *   Exporta el estado del servidor (TPS, RAM, Jugadores) a `server-state.json`.
    *   Permite a la TUI (Python) mostrar datos en tiempo real sin RCON.
4.  **Hook con DiscordSRV**:
    *   No reinventa la rueda: usa la conexión de DiscordSRV existente.

---

## 🛠️ Instalación y Compilación

Este proyecto usa **Maven**.

### Requisitos
- JDK 17 o superior.
- Maven (`mvn`).

### Compilar
```bash
mvn clean package
```
El archivo generado estará en `target/KubeControlPlugin-1.0-SNAPSHOT.jar`.
Cópialo a tu carpeta `/plugins/`.

### Dependencias
Asegúrate de tener instalados en tu servidor:
- **DiscordSRV** (Obligatorio)
- **Vault** (Opcional, para economía)
- **EssentialsX** (u otro proveedor de economía)

---

## 🤖 Guía de Setup: Discord Bot

Para que la verificación funcione, necesitas configurar un Bot.

### 1. Crear el Bot
1. Ve a [Discord Developer Portal](https://discord.com/developers/applications).
2. Crea una **"New Application"**.
3. En la pestaña **"Bot"**, crea el bot y copia su **Token**.
4. **Privileged Gateway Intents** (IMPORTANTE):
   - Activa **Presence Intent**.
   - Activa **Server Members Intent** (Necesario para dar roles).
   - Activa **Message Content Intent**.

### 2. Configuración en DiscordSRV
KubeControl usa la conexión de DiscordSRV. Edita `/plugins/DiscordSRV/config.yml`:

```yaml
BotToken: "PEGA_TU_TOKEN_AQUI"
Channels:
  global: "ID_CANAL_CHAT"
```

### 3. Configuración de KubeControl
Edita `/plugins/KubeControlPlugin/config.yml`:

```yaml
discord:
  enabled: true
  # Token se maneja en DiscordSRV
  
  channels:
    verification: "ID_CANAL_VERIFICACION" # Donde aparecerá el botón
  
  native-validation:
    enabled: true
    button-label: "✅ Verificarse"
    reward-role-id: "ID_ROL_A_DAR" # Rol que gana el usuario
```

> **Nota**: Asegúrate de que el rol del Bot en Discord esté **por encima** del rol que intenta asignar.

---

## Comandos

| Comando | Permiso | Descripción |
| :--- | :--- | :--- |
| `/kc reload` | `kubecontrol.admin` | Recarga la configuración. |
| `/kc status` | `kubecontrol.admin` | Muestra estado del Bridge y JDA. |
| `/kc sendverify` | `kubecontrol.admin` | Envía el panel con botón al canal configurado. |

---

## Estructura de Proyecto

- `src/main/java/`: Código fuente Java.
- `src/main/resources/`: `plugin.yml` y `config.yml`.
- `pom.xml`: Configuración de dependencias Maven.

Desarrollado para **KubeControlMC**.
