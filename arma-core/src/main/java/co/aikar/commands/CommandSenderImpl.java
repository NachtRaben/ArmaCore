package co.aikar.commands;

import dev.armadeus.bot.api.ArmaCore;
import dev.armadeus.bot.api.command.DiscordCommandIssuer;
import dev.armadeus.bot.api.config.GuildConfig;
import dev.armadeus.bot.api.util.DiscordReference;
import lombok.Getter;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
public class CommandSenderImpl extends JDACommandEvent implements DiscordCommandIssuer {

    private static final Logger logger = LogManager.getLogger();

    @Getter
    private static final Map<DiscordReference<Message>, CompletableFuture<?>> pendingDeletions = new ConcurrentHashMap<>();

    // Instance settings
    private final ArmaCore core;
    private boolean slashAcked;

    public CommandSenderImpl(ArmaCore core, JDACommandManager manager, SlashCommandInteractionEvent event) {
        super(manager, event);
        this.core = core;
    }

    public CommandSenderImpl(ArmaCore core, JDACommandManager manager, MessageReceivedEvent event) {
        super(manager, event);
        this.core = core;
        if (event.isFromGuild()) {
            if (getGuildConfig().deleteCommandMessages()) {
                purge(event.getMessage(), 0);
            }
        }
    }

    private void purge(Message message, long purgeAfter) {
        if (purgeAfter == -1)
            return;

        boolean purge = false;
        if (message.isFromGuild() && message.getGuild().getSelfMember().hasPermission(message.getTextChannel(), Permission.MESSAGE_MANAGE))
            purge = true;
        else if (message.getAuthor().equals(message.getJDA().getSelfUser()))
            purge = true;

        if (purge) {
            synchronized (pendingDeletions) {
                pendingDeletions.put(new DiscordReference<>(message, id -> getChannel().getHistory().getMessageById(id)), message.delete().submitAfter(purgeAfter, TimeUnit.SECONDS).thenAccept(aVoid ->
                        pendingDeletions.remove(message)
                ));
            }
        }
    }

    public GuildConfig getGuildConfig() {
        return core.guildManager().getConfigFor(getGuild());
    }

    public void sendMessage(String pattern, Object... args) {
        sendMessage(String.format(pattern, args), 0);
    }

    public void sendMessage(String message, long purgeAfter) {
        checkArgument(message != null && !message.isBlank(), "Empty Message");
        MessageBuilder builder = new MessageBuilder(message);
        sendMessage(builder.build(), purgeAfter);
    }

    public void sendMessage(MessageEmbed embed, long purgeAfter) {
        checkArgument(embed != null && embed.isSendable(), "Empty Message");
        MessageBuilder builder = new MessageBuilder(embed);
        sendMessage(builder.build(), purgeAfter);
    }

    public void sendMessage(Message message, long purgeAfter) {
        sendAndPurge(message, getChannel(), purgeAfter);
    }

    // Private Messages
    public void sendPrivateMessage(String message) {
        checkArgument(message != null && !message.isBlank(), "Empty Message");
        MessageBuilder builder = new MessageBuilder(message);
        sendPrivateMessage(builder.build());
    }

    public void sendPrivateMessage(String format, Object... args) {
        String message = String.format(format, args);
        checkArgument(message != null && !message.isBlank(), "Empty Message");
        MessageBuilder builder = new MessageBuilder(message);
        sendPrivateMessage(builder.build());
    }

    public void sendPrivateMessage(MessageEmbed embed) {
        checkArgument(embed != null && embed.isSendable(), "Empty Message");
        MessageBuilder builder = new MessageBuilder(embed);
        sendPrivateMessage(builder.build());
    }

    public void sendPrivateMessage(Message message) {
        getUser().openPrivateChannel().queue(channel -> {
            sendAndPurge(message, channel, -1);
        });
    }

    private void sendAndPurge(Message message, MessageChannel channel, long purgeAfter) {
        if (isSlashEvent() && !getSlash().getHook().isExpired()) {
            getSlash().getHook().sendMessage(message).queue();
            slashAcked = true;
            return;
        }
        if (channel.getType() == ChannelType.TEXT && purgeAfter == 0) {
            long guildMessageTimeout = getGuildConfig().getPurgeDelay();
            purgeAfter = guildMessageTimeout != 0 ? guildMessageTimeout : defaultPurgeDelay;
        } else if (channel.getType() == ChannelType.PRIVATE) {
            purgeAfter = -1;
        }
        if (channel.getType() == ChannelType.TEXT && !((TextChannel) channel).canTalk()) {
            sendPrivateMessage(message);
        }
        long finalPurgeAfter = purgeAfter;
        channel.sendMessage(message).submit()
                .thenAccept(m -> {
                    purge(m, finalPurgeAfter);
                })
                .exceptionally(throwable -> {
                    if (channel.getType() == ChannelType.TEXT) {
                        sendPrivateMessage(message);
                    } else {
                        logger.warn(String.format("Failed to send private message to %s with text %s", getUser().getName(), message), throwable);
                    }
                    return null;
                });
    }

    // Universal Senders
    @Override
    public void sendMessageInternal(String message) {
        sendMessage(message);
    }

    public void sendMessage(String message) {
        sendMessage(message, 0);
    }

    public void sendMessage(Message message) {
        sendMessage(message, 0);
    }

    public void sendMessage(MessageEmbed embed) {
        sendMessage(embed, 0);
    }
}
