package co.aikar.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class JDACommandEvent implements CommandIssuer {

    protected MessageReceivedEvent messageEvent;
    protected SlashCommandInteractionEvent slashEvent;
    protected final JDACommandManager manager;

    public JDACommandEvent(JDACommandManager manager, SlashCommandInteractionEvent event) {
        this.manager = manager;
        this.slashEvent = event;
    }

    public JDACommandEvent(JDACommandManager manager, MessageReceivedEvent event) {
        this.manager = manager;
        this.messageEvent = event;
    }

    public GenericEvent getEvent() {
        return messageEvent != null ? messageEvent : slashEvent != null ? slashEvent : null;
    }

    public MessageReceivedEvent getMessageEvent() {
        return messageEvent;
    }

    public SlashCommandInteractionEvent getSlashEvent() {
        return slashEvent;
    }

    @SuppressWarnings("unchecked")
    @Override
    public JDACommandEvent getIssuer() {
        return this;
    }

    @Override
    public JDACommandManager getManager() {
        return this.manager;
    }

    @Override
    public boolean isPlayer() {
        return false;
    }

    @Override
    public @NotNull UUID getUniqueId() {
        // Discord id only have 64 bit width (long) while UUIDs have twice the size.
        // In order to keep it unique we use 0L for the first 64 bit.
        long authorId = messageEvent.getAuthor().getIdLong();
        return new UUID(0, authorId);
    }

    @Override
    public boolean hasPermission(String permission) {
        CommandPermissionResolver permissionResolver = this.manager.getPermissionResolver();
        return permissionResolver == null || permissionResolver.hasPermission(manager, this, permission);
    }

    @Override
    public void sendMessageInternal(String message) {
        getChannel().sendMessage(message).queue();
    }

    public void sendMessage(MessageCreateData message) {
        getChannel().asTextChannel().sendMessage(message).queue();
    }

    public void sendMessage(MessageEmbed message) {
        getChannel().sendMessageEmbeds(message).queue();
    }

    // Additionals

    public Message getMessage() {
        return getMessageEvent().getMessage();
    }

    public User getUser() {
        return messageEvent != null ? messageEvent.getAuthor() : slashEvent != null ? slashEvent.getUser() : null;
    }

    // Ambiguous
    public MessageChannelUnion getChannel() {
        return messageEvent != null ? messageEvent.getChannel() : slashEvent != null ? slashEvent.getChannel() : null;
    }

    // Guild Specific
    public Guild getGuild() {
        return messageEvent != null ? messageEvent.getGuild() : slashEvent != null ? slashEvent.getGuild() : null;
    }

    public Member getMember() {
        return messageEvent != null ? messageEvent.getMember() : slashEvent != null ? slashEvent.getMember() : null;
    }

    public boolean isFromGuild() {
        return messageEvent != null ? messageEvent.isFromGuild() : slashEvent != null && slashEvent.isFromGuild();
    }

    public TextChannel getTextChannel() {
        MessageChannel ch = getChannel();
        return ch.getType() == ChannelType.TEXT ? (TextChannel) ch : null;
    }

    public AudioChannel getVoiceChannel() {
        Member m = getMember();
        if (m != null && m.getVoiceState() != null) {
            return m.getVoiceState().getChannel();
        }
        return null;
    }

    public JDA getJda() {
        return getEvent().getJDA();
    }
}
