package com.bm0x.kubecontrol.commands;

import com.bm0x.kubecontrol.KubeControl;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

@SuppressWarnings("deprecation")
public class CommandManager implements CommandExecutor {

    private final KubeControl plugin;

    public CommandManager(KubeControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload":
                if (!sender.hasPermission("kubecontrol.admin")) {
                    sender.sendMessage(ChatColor.RED + "No tienes permiso.");
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.prefix")
                                + plugin.getConfig().getString("messages.reload")));
                break;

            case "status":
                if (!sender.hasPermission("kubecontrol.admin")) {
                    sender.sendMessage(ChatColor.RED + "No tienes permiso.");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "KubeControl Plugin v" + plugin.getDescription().getVersion());
                sender.sendMessage(ChatColor.GRAY + "Discord Module: "
                        + (plugin.getConfig().getBoolean("discord.enabled") ? "ON" : "OFF"));
                break;

            case "sendverify":
                if (!sender.hasPermission("kubecontrol.admin")) {
                    sender.sendMessage(ChatColor.RED + "No tienes permiso.");
                    return true;
                }
                if (plugin.getDiscordHandler() != null) {
                    plugin.getDiscordHandler().sendVerificationPanel();
                    sender.sendMessage(
                            ChatColor.GREEN + "Panel de verificación enviado a Discord (si la config es correcta).");
                } else {
                    sender.sendMessage(ChatColor.RED + "El módulo de Discord no está activo (Falta DiscordSRV?).");
                }
                break;

            case "verifymember":
                // Placeholder for Role Manager (Phase 7)
                sender.sendMessage(ChatColor.YELLOW + "Función en desarrollo (Fase 7)");
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "--- KubeControl Help ---");
        sender.sendMessage(ChatColor.GRAY + "/kc reload - Recargar configuración");
        sender.sendMessage(ChatColor.GRAY + "/kc status - Ver estado de módulos");
    }
}
