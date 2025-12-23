package com.bm0x.kubecontrol;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import java.util.logging.Level;

@SuppressWarnings("deprecation")
public class KubeControl extends JavaPlugin {

    private static KubeControl instance;
    private com.bm0x.kubecontrol.discord.DiscordHandler discordHandler;
    private com.bm0x.kubecontrol.bridge.StateExporter stateExporter;

    @Override
    public void onEnable() {
        instance = this;
        // Save Default Config
        saveDefaultConfig();

        getLogger().info("----------------------------------------");
        getLogger().info(" KubeControlPlugin v" + getDescription().getVersion());
        getLogger().info(" Iniciando módulos de integración...");
        getLogger().info("----------------------------------------");

        // Check Dependencies
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().log(Level.SEVERE, "Vault no encontrado! Módulo de Economía desactivado.");
        }

        if (Bukkit.getPluginManager().getPlugin("DiscordSRV") == null) {
            getLogger().log(Level.SEVERE, "DiscordSRV no encontrado! Módulo de Discord desactivado.");
        } else {
            // Init Discord Handler
            discordHandler = new com.bm0x.kubecontrol.discord.DiscordHandler(this);
            discordHandler.init();
        }

        // Init Bridge (State Exporter)
        stateExporter = new com.bm0x.kubecontrol.bridge.StateExporter(this);
        stateExporter.start();

        // Start Role Syncer
        new com.bm0x.kubecontrol.discord.RoleSyncTask(this).start();

        // Register Commands
        getCommand("kc").setExecutor(new com.bm0x.kubecontrol.commands.CommandManager(this));

        getLogger().info("KubeControlPlugin habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        getLogger().info("KubeControlPlugin deshabilitando...");
        // Close resources
        instance = null;
    }

    public static KubeControl getInstance() {
        return instance;
    }

    public com.bm0x.kubecontrol.discord.DiscordHandler getDiscordHandler() {
        return discordHandler;
    }

    public com.bm0x.kubecontrol.bridge.StateExporter getStateExporter() {
        return stateExporter;
    }
}
