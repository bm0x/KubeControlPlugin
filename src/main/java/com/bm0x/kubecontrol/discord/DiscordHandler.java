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
            String msgText = plugin.getConfig().getString("discord.native-validation.message-text");
            String btnLabel = plugin.getConfig().getString("discord.native-validation.button-label");

            // Allow cleaning chat? Maybe later.

            channel.sendMessage(msgText)
                    .setActionRow(Button.success("kc_verify_btn", btnLabel)) // ID: kc_verify_btn
                    .queue();

            plugin.getLogger().info("Panel de verificación enviado al canal " + channel.getName());
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
                        success -> event.reply("✅ **Verificado!** Se te ha asignado el rol " + role.getName())
                                .setEphemeral(true).queue(),
                        error -> event.reply("❌ Error al asignar rol. Contacta a un admin.").setEphemeral(true)
                                .queue());
            } else {
                event.reply("❌ Error de configuración (Rol o Miembro no encontrado).").setEphemeral(true).queue();
            }
        } else {
            event.reply("❌ Configuración incompleta.").setEphemeral(true).queue();
        }
    }
}
