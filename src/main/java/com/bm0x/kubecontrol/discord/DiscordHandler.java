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

        // Acknowledge interaction immediately to prevent "Interaction Failed"
        event.deferReply(true).queue();

        // Get Reward Role IDs (List support)
        java.util.List<String> roleIds = new java.util.ArrayList<>(
                plugin.getConfig().getStringList("discord.native-validation.reward-role-ids"));

        // Fallback to singular if empty
        if (roleIds.isEmpty()) {
            String singleObj = plugin.getConfig().getString("discord.native-validation.reward-role-id");
            if (singleObj != null)
                roleIds.add(singleObj);
        }

        Guild guild = event.getGuild();
        Member member = event.getMember();
        Member selfMember = guild != null ? guild.getSelfMember() : null;

        // === Validaciones básicas ===
        if (guild == null || member == null || selfMember == null) {
            event.getHook().sendMessage("❌ **Error:** No se pudo obtener información del servidor.").queue();
            return;
        }

        if (roleIds.isEmpty()) {
            event.getHook().sendMessage(
                    "⚠️ **Error de Configuración:**\nNo hay roles configurados en `reward-role-ids`.\nContacta a un administrador.")
                    .queue();
            return;
        }

        // === Clasificar roles por estado ===
        java.util.List<Role> rolesToAdd = new java.util.ArrayList<>();
        java.util.List<String> alreadyHas = new java.util.ArrayList<>();
        java.util.List<String> notFound = new java.util.ArrayList<>();
        java.util.List<String> hierarchyError = new java.util.ArrayList<>();
        boolean hasManageRoles = selfMember
                .hasPermission(github.scarsz.discordsrv.dependencies.jda.api.Permission.MANAGE_ROLES);

        for (String rid : roleIds) {
            Role role = guild.getRoleById(rid);

            if (role == null) {
                notFound.add(rid);
                continue;
            }

            // ¿Ya tiene el rol?
            if (member.getRoles().contains(role)) {
                alreadyHas.add(role.getName());
                continue;
            }

            // ¿Bot puede asignar este rol? (jerarquía)
            if (!selfMember.canInteract(role)) {
                hierarchyError.add(role.getName());
                continue;
            }

            rolesToAdd.add(role);
        }

        // === Construir respuesta ===
        StringBuilder response = new StringBuilder();

        // Verificar permiso MANAGE_ROLES
        if (!hasManageRoles) {
            response.append("❌ **Error:** El bot no tiene el permiso `MANAGE_ROLES`.\nContacta a un administrador.\n");
        } else if (!rolesToAdd.isEmpty()) {
            StringBuilder names = new StringBuilder();
            for (Role r : rolesToAdd) {
                if (names.length() > 0)
                    names.append(", ");
                names.append(r.getName());
                guild.addRoleToMember(member, r).queue(
                        success -> {
                        },
                        error -> plugin.getLogger()
                                .warning("Error asignando rol " + r.getName() + ": " + error.getMessage()));
            }
            response.append("✅ **¡Verificado!** Roles asignados: ").append(names).append("\n");
        }

        if (!alreadyHas.isEmpty()) {
            response.append("ℹ️ Ya tenías: ").append(String.join(", ", alreadyHas)).append("\n");
        }

        if (!notFound.isEmpty()) {
            response.append("⚠️ Roles no encontrados (IDs inválidas): `").append(String.join("`, `", notFound))
                    .append("`\n");
        }

        if (!hierarchyError.isEmpty()) {
            response.append("❌ No puedo asignar (jerarquía superior al bot): ")
                    .append(String.join(", ", hierarchyError)).append("\n");
        }

        if (response.length() == 0) {
            response.append("❌ No se pudo asignar ningún rol. Contacta a un administrador.");
        }

        event.getHook().sendMessage(response.toString()).queue();

        // === Ejecutar comandos si hay cuenta vinculada ===
        if (!rolesToAdd.isEmpty() && DiscordSRV.getPlugin().getAccountLinkManager() != null) {
            java.util.UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(member.getId());
            if (uuid != null) {
                final String playerName = org.bukkit.Bukkit.getOfflinePlayer(uuid).getName();
                if (playerName != null) {
                    java.util.List<String> commands = plugin.getConfig()
                            .getStringList("discord.native-validation.reward-commands");
                    if (commands != null && !commands.isEmpty()) {
                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                            org.bukkit.command.ConsoleCommandSender console = org.bukkit.Bukkit.getConsoleSender();
                            for (String cmd : commands) {
                                String parsedCmd = cmd.replace("%player%", playerName);
                                org.bukkit.Bukkit.dispatchCommand(console, parsedCmd);
                            }
                        });
                    }
                }
            }
        }
    }

    public JDA getJda() {
        return this.jda;
    }
}
