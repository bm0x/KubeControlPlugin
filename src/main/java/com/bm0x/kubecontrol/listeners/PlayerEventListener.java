package com.bm0x.kubecontrol.listeners;

import com.bm0x.kubecontrol.KubeControl;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener para eventos de jugadores.
 * Proporciona sincronización en tiempo real de roles y actualización del estado
 * del servidor.
 */
public class PlayerEventListener implements Listener {

    private final KubeControl plugin;

    public PlayerEventListener(KubeControl plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Sincronizar roles de Discord al conectarse
        // Delay de 2 segundos para que DiscordSRV cargue la vinculación de cuenta
        if (plugin.getRoleSyncTask() != null) {
            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                if (event.getPlayer().isOnline()) {
                    plugin.getRoleSyncTask().syncPlayer(event.getPlayer());
                }
            }, 40L); // 2 segundos
        }

        // Actualizar JSON del estado del servidor inmediatamente
        if (plugin.getStateExporter() != null) {
            plugin.getStateExporter().exportNow();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Actualizar JSON inmediatamente cuando un jugador se desconecta
        if (plugin.getStateExporter() != null) {
            // Pequeño delay para que el jugador ya no esté en la lista
            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                plugin.getStateExporter().exportNow();
            }, 5L); // 0.25 segundos
        }
    }
}
