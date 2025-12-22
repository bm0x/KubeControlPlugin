package com.bm0x.kubecontrol.bridge;

import com.bm0x.kubecontrol.KubeControl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import github.scarsz.discordsrv.DiscordSRV;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StateExporter {

    private final KubeControl plugin;
    private final Gson gson;
    private Economy economy = null;

    public StateExporter(KubeControl plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public void start() {
        int interval = plugin.getConfig().getInt("bridge.export-interval", 5);

        new BukkitRunnable() {
            @Override
            public void run() {
                export();
            }
        }.runTaskTimerAsynchronously(plugin, 20L, interval * 20L);
    }

    private void export() {
        ServerState state = new ServerState();

        // Server Stats
        state.tps = Bukkit.getTPS()[0]; // 1m TPS approximately (Paper API needed for better TPS?)
        state.rams_used = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        state.rams_max = Runtime.getRuntime().maxMemory() / 1024 / 1024;

        // Players
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerInfo info = new PlayerInfo();
            info.uuid = p.getUniqueId().toString();
            info.name = p.getName();
            info.ping = p.getPing();
            info.world = p.getWorld().getName();

            // Economy (Vault)
            if (economy != null) {
                info.balance = economy.getBalance(p);
            }

            // Discord (DiscordSRV)
            if (Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) {
                String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(p.getUniqueId());
                if (discordId != null) {
                    info.discordId = discordId;
                    // JDA is heavy to query avatar every 5s.
                    // TUI can resolve Avatar URL from ID:
                    // https://cdn.discordapp.com/avatars/{id}/{hash}.png
                    // But we don't have hash easily without JDA user object.
                    // We can try to get cached user from DiscordSRV JDA.
                    try {
                        github.scarsz.discordsrv.dependencies.jda.api.entities.User user = DiscordSRV.getPlugin()
                                .getJda().getUserById(discordId);
                        if (user != null) {
                            info.discordTag = user.getAsTag();
                            info.avatarUrl = user.getAvatarUrl();
                        }
                    } catch (Exception e) {
                        // Ignore JDA errors during async export
                    }
                }
            }

            state.players.add(info);
        }

        // Write File
        File file = new File(plugin.getDataFolder().getParentFile().getParentFile(),
                plugin.getConfig().getString("bridge.output-file", "server-state.json"));
        // Default writes to root server folder (plugins/../..)

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(state, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Error escribiendo server-state.json: " + e.getMessage());
        }
    }

    // Data Classes
    @SuppressWarnings("unused")
    private static class ServerState {
        double tps;
        long rams_used;
        long rams_max;
        List<PlayerInfo> players = new ArrayList<>();
    }

    @SuppressWarnings("unused")
    private static class PlayerInfo {
        String uuid;
        String name;
        int ping;
        String world;
        double balance = 0.0;
        String discordId;
        String discordTag;
        String avatarUrl;
    }
}
