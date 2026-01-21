package com.bm0x.kubecontrol.discord;

import com.bm0x.kubecontrol.KubeControl;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class RoleSyncTask extends BukkitRunnable {

    private final KubeControl plugin;

    public RoleSyncTask(KubeControl plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int interval = plugin.getConfig().getInt("sync.interval", 60);
        if (plugin.getConfig().getBoolean("sync.enabled")) {
            this.runTaskTimer(plugin, 100L, interval * 20L); // Delay 5s, Period configurable
            plugin.getLogger().info("Tarea de Sincronización de Roles iniciada (Intervalo: " + interval + "s)");
        }
    }

    @Override
    public void run() {
        if (!Bukkit.getPluginManager().isPluginEnabled("DiscordSRV"))
            return;

        // Iterate over online players to sync them
        for (Player p : Bukkit.getOnlinePlayers()) {
            syncPlayer(p);
        }
    }

    public void syncPlayer(Player p) {
        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(p.getUniqueId());
        if (discordId == null)
            return; // Not linked

        // We need JDA to check roles
        if (plugin.getDiscordHandler() == null || plugin.getDiscordHandler().getJda() == null)
            return;

        // Assuming main guild (DiscordSRV main guild)
        // Ideally checking all guilds or specific one from config
        String mainGuildId = DiscordSRV.getPlugin().getMainGuild() != null
                ? DiscordSRV.getPlugin().getMainGuild().getId()
                : null;
        if (mainGuildId == null)
            return;

        Guild guild = plugin.getDiscordHandler().getJda().getGuildById(mainGuildId);
        if (guild == null)
            return;

        // Get Member (Requires caching or rest action, careful with rate limits)
        // JDA entities are usually cached if presence intent is on
        Member member = guild.getMemberById(discordId);

        if (member == null) {
            // Member left discord?
            return;
        }

        // ===================================
        // 1. Discord -> Game (Server Booster etc)
        // ===================================
        ConfigurationSection d2g = plugin.getConfig().getConfigurationSection("sync.discord-to-game");
        if (d2g != null) {
            for (String key : d2g.getKeys(false)) {
                String roleId = d2g.getString(key + ".discord-role-id");
                List<String> commands = d2g.getStringList(key + ".commands");

                if (roleId != null && !commands.isEmpty()) {
                    Role role = guild.getRoleById(roleId);
                    if (role != null && member.getRoles().contains(role)) {
                        // User HAS the role. Execute commands.
                        // Optimization: Execute only if not already done?
                        // Problem: Commands like "addtemp" can be run repeatedly?
                        // "addtemp" checks validity usually.
                        // Or we can check if player already has permission?
                        // For now, naive execution.

                        runCommands(p, commands);
                    }
                }
            }
        }

        // ===================================
        // 2. Game -> Discord (Staff, Magnate)
        // ===================================
        ConfigurationSection g2d = plugin.getConfig().getConfigurationSection("sync.game-to-discord");
        if (g2d != null) {
            for (String key : g2d.getKeys(false)) {
                String permission = g2d.getString(key + ".minecraft-permission");
                double minBal = g2d.getDouble(key + ".min-balance", -1.0);
                String roleId = g2d.getString(key + ".give-discord-role-id");

                boolean criterionMet = false;

                // Permission Check
                if (permission != null && !permission.isEmpty()) {
                    if (p.hasPermission(permission))
                        criterionMet = true;
                }

                // Balance Check (Vault)
                if (minBal >= 0 && plugin.getStateExporter() != null) {
                    // Accessing Vault via direct static if easier or keep passing instances
                    // Let's assume we invoke standard Vault method
                    if (hasBalance(p, minBal))
                        criterionMet = true;
                }

                if (criterionMet && roleId != null) {
                    Role targetRole = guild.getRoleById(roleId);

                    // Validar que el rol existe
                    if (targetRole == null) {
                        plugin.getLogger()
                                .warning("[Sync] Rol no encontrado (ID: " + roleId + ") para la regla '" + key + "'");
                        continue;
                    }

                    // Validar jerarquía
                    if (!guild.getSelfMember().canInteract(targetRole)) {
                        plugin.getLogger().warning("[Sync] No puedo asignar rol '" + targetRole.getName() +
                                "' - Jerarquía superior al bot. Mueve el rol del bot más arriba en Discord.");
                        continue;
                    }

                    // Agregar rol si no lo tiene
                    if (!member.getRoles().contains(targetRole)) {
                        guild.addRoleToMember(member, targetRole).queue(
                                success -> plugin.getLogger()
                                        .info("[Sync] Rol '" + targetRole.getName() + "' asignado a " + p.getName()),
                                error -> plugin.getLogger().warning("[Sync] Error asignando rol '"
                                        + targetRole.getName() + "': " + error.getMessage()));
                    }
                }
            }
        }
    }

    private void runCommands(Player p, List<String> commands) {
        // Run on main thread synchronously
        Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.command.ConsoleCommandSender console = Bukkit.getConsoleSender();
            for (String cmd : commands) {
                Bukkit.dispatchCommand(console, cmd.replace("%player%", p.getName()));
            }
        });
    }

    private boolean hasBalance(Player p, double min) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault"))
            return false;
        try {
            net.milkbowl.vault.economy.Economy eco = Bukkit.getServicesManager()
                    .getRegistration(net.milkbowl.vault.economy.Economy.class).getProvider();
            return eco.getBalance(p) >= min;
        } catch (Exception e) {
            return false;
        }
    }
}
