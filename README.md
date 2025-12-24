# KubeControlPlugin 🧩

**KubeControlPlugin** es el plugin "compañero" de Java para la herramienta de gestión **KubeControlMC**.
![Build Status](https://github.com/bm0x/KubeControlPlugin/actions/workflows/build.yml/badge.svg)

Diseñado para **Minecraft 1.20 / 1.21**, sirve como puente entre el servidor, Discord y la interfaz de gestión.

## Características Principales

1.  **🔄 Sincronización de Roles (Bidireccional)**:
    *   **Discord -> Juego**: Si alguien es "Booster" en Discord, dale "VIP" y $5000 en Minecraft.
    *   **Juego -> Discord**: Si alguien compra Rango "MVP" en el juego, dale el rol "MVP" en Discord automáticamente.
2.  **✅ Verificación Nativa Mejorada**:
    *   Panel con botón "Verificarse" en Discord.
    *   Asigna múltiples roles y ejecuta comandos consola al verificar.
    *   **Robustez**: Evita errores de "Interacción Fallida" usando colas asíncronas.
3.  **💰 Economía Integrada**:
    *   Soporte para **Vault** y **LuckPerms**.
4.  **📊 Bridge JSON**:
    *   Exporta estadísticas en tiempo real para el Dashboard de KubeControlMC (TPS, RAM, Jugadores).

---

## 🛠️ Comandos

| Comando | Permiso | Descripción |
| :--- | :--- | :--- |
| `/kc reload` | `kubecontrol.admin` | Recarga la configuración. |
| `/kc status` | `kubecontrol.admin` | Muestra estado de la conexión JDA/Bridge. |
| `/kc sendverify` | `kubecontrol.admin` | Envía el panel de verificación al canal configurado. |
| `/kc verifymember <user>` | `kubecontrol.admin` | **Nuevo**: Fuerza la sincronización de roles para un jugador específico. |

---

## ⚙️ Configuración (Sync)

La nueva sección `sync` en `config.yml` permite definir reglas complejas:

```yaml
sync:
  # Cada cuanto revisar (ticks)
  interval-ticks: 1200 # 1 minuto

  # Discord -> Minecraft
  discord-to-game:
    - discord-role-id: "999999999999999999" # Server Booster
      # Comandos a ejecutar si el usuario tiene ese rol
      commands-on-give:
        - "lp user %player% parent add vip"
        - "eco give %player% 5000"
      commands-on-remove:
        - "lp user %player% parent remove vip"

  # Minecraft -> Discord
  game-to-discord:
    - permission: "group.vip" # Si tiene este permiso/rango
      # Dar este rol en Discord
      discord-role-id: "888888888888888888"
```

## 🤖 Setup Básico

1.  **DiscordSRV**: Asegúrate de tener DiscordSRV instalado y vinculado.
2.  **Bot**: El bot debe tener permisos de `Manage Roles` y estar por encima de los roles que quiere asignar.
3.  **Compilación**: `mvn clean package`.

---

Desarrollado para **KubeControlMC**.
