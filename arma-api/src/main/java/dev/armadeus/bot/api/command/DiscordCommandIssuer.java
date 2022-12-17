package dev.armadeus.bot.api.command;

import co.aikar.commands.CommandIssuer;
import dev.armadeus.bot.api.config.GuildConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public interface DiscordCommandIssuer extends CommandIssuer {

    int defaultPurgeDelay = 120;

    Message getMessage();

    User getUser();

    MessageChannel getChannel();

    Guild getGuild();

    Member getMember();

    boolean isFromGuild();

    GuildConfig getGuildConfig();

    TextChannel getTextChannel();

    AudioChannel getVoiceChannel();

    JDA getJda();

    // Channel Agnostic Senders
    void sendMessage(String message);

    void sendMessage(String pattern, Object... args);

    void sendMessage(String message, long purgeAfter);

    void sendMessage(MessageEmbed embed);

    void sendMessage(MessageEmbed embed, long purgeAfter);

    void sendMessage(MessageCreateData message);

    void sendMessage(MessageCreateData message, long purgeAfter);

    void queueMessagePurge(Message message, long purgeAfter);

    // Private Only Senders
    void sendPrivateMessage(String message);

    void sendPrivateMessage(String pattern, Object... args);

    void sendPrivateMessage(MessageEmbed embed);

    void sendPrivateMessage(MessageCreateData message);
}
