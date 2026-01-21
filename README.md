# 🧩 KubeControlPlugin

<div align="center">

[![Build Status](https://github.com/bm0x/KubeControlPlugin/actions/workflows/build.yml/badge.svg)](https://github.com/bm0x/KubeControlPlugin/actions/workflows/build.yml)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20%20%2F%201.21-brightgreen.svg)]()
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Plugin Compañero para KubeControlMC · Sincronización Discord · Bridge de Estadísticas**

</div>

---

## 📋 Descripción

**KubeControlPlugin** es el plugin de servidor que complementa a [KubeControlMC](https://github.com/bm0x/KubeControlMC). Actúa como puente entre tu servidor Minecraft, Discord y la interfaz de gestión.

### Características Principales

- 🔄 **Sincronización bidireccional** de roles Discord ↔ Minecraft
- ✅ **Sistema de verificación** con panel interactivo en Discord
- 💰 **Integración económica** con Vault y LuckPerms
- 📊 **Bridge JSON** para estadísticas en tiempo real
- 🛡️ **Cola asíncrona** para evitar errores de interacción

---

## ✨ Funcionalidades

### 🔄 Sincronización de Roles

**Discord → Minecraft:**
```
Usuario tiene rol "Booster" en Discord
  ↓
Automáticamente recibe rango VIP + $5000 en el servidor
```

**Minecraft → Discord:**
```
Jugador compra rango MVP en el servidor
  ↓
Automáticamente recibe rol MVP en Discord
```

### ✅ Sistema de Verificación

- Panel con botón "Verificarse" en Discord
- Asignación automática de roles al verificar
- Ejecución de comandos de consola personalizados
- Manejo robusto de interacciones (sin errores de timeout)

### 📊 Bridge JSON

Exporta estadísticas en tiempo real para el Dashboard de KubeControlMC:

```json
{
  "tps": 19.8,
  "memory": { "used": 2048, "max": 4096 },
  "players": { "online": 5, "max": 20 },
  "uptime": 3600
}
```

---

## 📥 Instalación

### Requisitos

- **Servidor**: Paper, Folia o Spigot 1.20+
- **Java**: 17+
- **Dependencias**: 
  - [DiscordSRV](https://www.spigotmc.org/resources/discordsrv.18494/) (obligatorio)
  - [Vault](https://www.spigotmc.org/resources/vault.34315/) (opcional, para economía)
  - [LuckPerms](https://luckperms.net/) (opcional, para permisos)

### Pasos

1. Descarga el JAR desde [Releases](https://github.com/bm0x/KubeControlPlugin/releases)
2. Coloca el archivo en la carpeta `plugins/`
3. Reinicia el servidor
4. Configura `plugins/KubeControlPlugin/config.yml`

---

## 🛠️ Comandos

| Comando | Permiso | Descripción |
|---------|---------|-------------|
| `/kc reload` | `kubecontrol.admin` | Recarga la configuración |
| `/kc status` | `kubecontrol.admin` | Muestra estado de conexión JDA/Bridge |
| `/kc sendverify` | `kubecontrol.admin` | Envía panel de verificación al canal |
| `/kc verifymember <user>` | `kubecontrol.admin` | Fuerza sincronización de roles para un jugador |
| `/kc sync` | `kubecontrol.admin` | Ejecuta sincronización manual de todos los jugadores |

---

## ⚙️ Configuración

### config.yml

```yaml
# Configuración general
discord:
  bot-token: "TU_TOKEN_AQUI"
  guild-id: "123456789012345678"
  verify-channel-id: "123456789012345678"

# Roles de verificación
verification:
  enabled: true
  roles-on-verify:
    - "987654321098765432"  # Rol "Verificado"
  commands-on-verify:
    - "lp user %player% parent add miembro"

# Sincronización de roles
sync:
  interval-ticks: 1200  # 1 minuto
  
  # Discord -> Minecraft
  discord-to-game:
    - discord-role-id: "999999999999999999"  # Server Booster
      commands-on-give:
        - "lp user %player% parent add vip"
        - "eco give %player% 5000"
      commands-on-remove:
        - "lp user %player% parent remove vip"

  # Minecraft -> Discord
  game-to-discord:
    - permission: "group.vip"
      discord-role-id: "888888888888888888"
    - permission: "group.mvp"
      discord-role-id: "777777777777777777"

# Bridge JSON (para KubeControlMC)
bridge:
  enabled: true
  port: 25580
  auth-token: "tu_token_secreto"
```

---

## 🔧 Permisos

| Permiso | Descripción |
|---------|-------------|
| `kubecontrol.admin` | Acceso a todos los comandos administrativos |
| `kubecontrol.sync` | Permite usar `/kc sync` |
| `kubecontrol.verify.bypass` | Salta la verificación de Discord |

---

## 🏗️ Compilación

### Desde código fuente

```bash
git clone https://github.com/bm0x/KubeControlPlugin.git
cd KubeControlPlugin
mvn clean package
```

El JAR compilado estará en `target/KubeControlPlugin-*.jar`

### Dependencias Maven

```xml
<dependency>
    <groupId>net.dv8tion</groupId>
    <artifactId>JDA</artifactId>
    <version>5.0.0-beta.18</version>
</dependency>
```

---

## 🤝 Integración con KubeControlMC

Para aprovechar al máximo las funcionalidades:

1. Instala **KubeControlMC** en tu máquina de administración
2. Instala **KubeControlPlugin** en tu servidor
3. Configura el Bridge JSON con el mismo token en ambos
4. El Dashboard mostrará estadísticas en tiempo real

---

## ❓ FAQ

**¿Funciona sin DiscordSRV?**
> No, DiscordSRV es obligatorio para la conexión con Discord.

**¿Por qué no se sincronizan los roles?**
> Verifica que el bot tenga permiso "Manage Roles" y esté por encima de los roles que quiere asignar en la jerarquía.

**¿Cómo evito el error "Interacción Fallida"?**
> El plugin usa colas asíncronas para manejar esto automáticamente. Si persiste, aumenta el timeout en la config.

**¿Es compatible con Folia?**
> Sí, usa API asíncrona compatible con Folia.

---

## 📄 Licencia

MIT License - Ver [LICENSE](LICENSE) para más detalles.

---

<div align="center">

**KubeControlPlugin** · *El puente entre tu servidor y el mundo.*

[Reportar Bug](https://github.com/bm0x/KubeControlPlugin/issues) · [Documentación](https://github.com/bm0x/KubeControlPlugin/wiki)

</div>
