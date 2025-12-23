package com.bm0x.kubecontrol.discord;

import com.bm0x.kubecontrol.KubeControl;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;

public class DiscordHandler extends ListenerAdapter {

    private final KubeControl plugin;
    private JDA jda;

    public DiscordHandler(KubeControl plugin) {
        this.plugin = plugin;
    }

    public void init() {
        // We subscribe to DiscordSRV's ready event to get JDA safely
        DiscordSRV.api.subscribe(this);
    }

    @Subscribe(priority = ListenerPriority.NORMAL)
    public void onDiscordReady(DiscordReadyEvent event) {
        // Fix: getJDA() might not exist on event in some versions, access via static
        // instance
        this.jda = DiscordSRV.getPlugin().getJda();
        plugin.getLogger().info("KubeControl enganchado a JDA vía DiscordSRV.");

        // Register our interaction listener
        this.jda.addEventListener(this);

        // Auto-Check Verification Panel
        if (plugin.getConfig().getBoolean("discord.native-validation.enabled")) {
            sendVerificationPanel();
        }
    }

    // ==========================================
    // Native Validation Logic
    // ==========================================

    public void sendVerificationPanel() {
        if (jda == null) {
            plugin.getLogger().warning("JDA no está listo aún.");
            return;
        }

        String channelId = plugin.getConfig().getString("discord.channels.verification");
        if (channelId == null || channelId.isEmpty())
            return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            String existingMsgId = plugin.getConfig().getString("discord.native-validation.message-id");
            String btnLabel = plugin.getConfig().getString("discord.native-validation.button-label");

            // Placeholder Replacement
            String serverName = plugin.getConfig().getString("discord.native-validation.server-name", "Server");
            String rawMsgText = plugin.getConfig().getString("discord.native-validation.message-text");

            final String finalMsgText = (rawMsgText != null) ? rawMsgText.replace("{server_name}", serverName) : "";

            // Logic to send new message
            java.util.function.Consumer<Void> sendNewMessage = (v) -> {
                channel.sendMessage(finalMsgText)
                        .setActionRow(Button.success("kc_verify_btn", btnLabel))
                        .queue(msg -> {
                            plugin.getConfig().set("discord.native-validation.message-id", msg.getId());
                            plugin.saveConfig();
                            plugin.getLogger()
                                    .info("Panel de verificación enviado y guardado (ID: " + msg.getId() + ")");
                        });
            };

            if (existingMsgId != null && !existingMsgId.isEmpty()) {
                // Check if exists
                channel.retrieveMessageById(existingMsgId).queue(
                        msg -> {
                            // Message exists, maybe update it? For now just log.
                            // Optional: Edit it to ensure text is up to date
                            // msg.editMessage(msgText).setActionRow(...).queue();
                            plugin.getLogger().info("Panel de verif. ya existe (ID: " + existingMsgId + ")");
                        },
                        failure -> {
                            // Message not found (deleted?), send new
                            plugin.getLogger().warning("Mensaje de verif. antiguo no encontrado. Enviando nuevo...");
                            sendNewMessage.accept(null);
                        });
            } else {
                // No ID stored, send new
                sendNewMessage.accept(null);
            }

        } else {
            plugin.getLogger().warning("No se encontró el canal de verificación: " + channelId);
        }
    }

    @Override
    public void onButtonClick(ButtonClickEvent event) {
        if (!event.getComponentId().equals("kc_verify_btn"))
            return;

        // Get Reward Role ID
        String roleId = plugin.getConfig().getString("discord.native-validation.reward-role-id");
        Guild guild = event.getGuild();

        if (roleId != null && guild != null) {
            Role role = guild.getRoleById(roleId);
            Member member = event.getMember();

            if (role != null && member != null) {
                // Give Role
                guild.addRoleToMember(member, role).queue(
                        success -> {
                            event.reply("✅ **Verificado!**\nSe te ha asignado el rol\n" + role.getName())
                                    .setEphemeral(true).queue();

                            // Check for Linked Account and Execute Commands
                            // Run async to avoid blocking Discord thread, but commands must run on main
                            // thread
                            if (DiscordSRV.getPlugin().getAccountLinkManager() != null) {
                                java.util.UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager()
                                        .getUuid(member.getId());
                                if (uuid != null) {
                                    final String playerName = org.bukkit.Bukkit.getOfflinePlayer(uuid).getName();
                                    if (playerName != null) {
                                        java.util.List<String> commands = plugin.getConfig()
                                                .getStringList("discord.native-validation.reward-commands");
                                        if (commands != null && !commands.isEmpty()) {
                                            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                                                org.bukkit.command.ConsoleCommandSender console = org.bukkit.Bukkit
                                                        .getConsoleSender();
                                                for (String cmd : commands) {
                                                    String parsedCmd = cmd.replace("%player%", playerName);
                                                    org.bukkit.Bukkit.dispatchCommand(console, parsedCmd);
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        },
                        error -> event.reply("❌ Error al asignar rol.\nContacta a un admin.").setEphemeral(true)
                                .queue());
            } else {
                event.reply("❌ Error de configuración (Rol o Miembro no encontrado).").setEphemeral(true).queue();
            }
        } else {
            event.reply("❌ Configuración incompleta.").setEphemeral(true).queue();
        }
    }

    public JDA getJda() {
        return this.jda;
    }
}
