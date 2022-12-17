package co.aikar.commands;

import dev.armadeus.bot.api.ArmaCore;
import dev.armadeus.bot.api.command.DiscordCommandIssuer;
import dev.armadeus.bot.api.config.GuildConfig;
import dev.armadeus.bot.api.util.DiscordReference;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Log4j2
@Getter
public class CommandSenderImpl extends JDACommandEvent implements DiscordCommandIssuer {

    @Getter
    private static final Map<DiscordReference<Message>, CompletableFuture<?>> pendingDeletions = new ConcurrentHashMap<>();

    // Instance settings
    private final ArmaCore core;

    public CommandSenderImpl(ArmaCore core, JDACommandManager manager, SlashCommandInteractionEvent event) {
        super(manager, event);
        this.core = core;
        if (event.isFromGuild() && !event.getInteraction().isAcknowledged())
            event.deferReply(getGuildConfig().deleteCommandMessages()).queue();
        else if (!event.getInteraction().isAcknowledged())
            event.deferReply(false).queue();
    }

    public CommandSenderImpl(ArmaCore core, JDACommandManager manager, MessageReceivedEvent event) {
        super(manager, event);
        this.core = core;
        if (event.isFromGuild()) {
            if (getGuildConfig().deleteCommandMessages()) {
                queueMessagePurge(event.getMessage(), 0);
            }
        }
    }

    @Override
    public void queueMessagePurge(Message message, long purgeAfter) {
        // Message Event Handling
        if (message.getChannel().getType() == ChannelType.TEXT && purgeAfter == 0) {
            long guildMessageTimeout = getGuildConfig().getPurgeDelay();
            purgeAfter = guildMessageTimeout != 0 ? guildMessageTimeout : defaultPurgeDelay;
        } else if (message.getChannel().getType() == ChannelType.PRIVATE) {
            purgeAfter = -1;
        }
        if (purgeAfter == -1)
            return;

        boolean purge = false;
        if (message.isFromGuild() && message.getGuild().getSelfMember().hasPermission(message.getChannel().asTextChannel(), Permission.MESSAGE_MANAGE))
            purge = true;
        else if (message.getAuthor().equals(message.getJDA().getSelfUser()))
            purge = true;

        if (purge) {
            synchronized (pendingDeletions) {
                var reference = new DiscordReference<>(message, id -> getChannel().getHistory().getMessageById(id));
                pendingDeletions.put(reference, message.delete().submitAfter(purgeAfter, TimeUnit.SECONDS).thenAccept(aVoid -> pendingDeletions.remove(reference)));
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
        MessageCreateData builder = MessageCreateData.fromContent(message);
        sendMessage(builder, purgeAfter);
    }

    public void sendMessage(MessageEmbed embed, long purgeAfter) {
        checkArgument(embed != null && embed.isSendable(), "Empty Message");
        MessageCreateData builder = MessageCreateData.fromEmbeds(embed);
        sendMessage(builder, purgeAfter);
    }

    public void sendMessage(MessageCreateData message, long purgeAfter) {
        sendAndPurge(message, getChannel(), purgeAfter);
    }

    // Private Messages
    public void sendPrivateMessage(String message) {
        checkArgument(message != null && !message.isBlank(), "Empty Message");
        MessageCreateData builder = MessageCreateData.fromContent(message);
        sendPrivateMessage(builder);
    }

    public void sendPrivateMessage(String format, Object... args) {
        String message = String.format(format, args);
        checkArgument(message != null && !message.isBlank(), "Empty Message");
        MessageCreateData builder = MessageCreateData.fromContent(message);
        sendPrivateMessage(builder);
    }

    public void sendPrivateMessage(MessageEmbed embed) {
        checkArgument(embed != null && embed.isSendable(), "Empty Message");
        MessageCreateData builder = MessageCreateData.fromEmbeds(embed);
        sendPrivateMessage(builder);
    }

    public void sendPrivateMessage(MessageCreateData message) {
        getUser().openPrivateChannel().queue(channel -> {
            sendAndPurge(message, channel, -1);
        });
    }

    private void sendAndPurge(MessageCreateData message, MessageChannel channel, long purgeAfter) {
        // Slash Event Handling
        if (slashEvent != null && !slashEvent.getHook().isExpired()) {
            if (!slashEvent.isAcknowledged())
                slashEvent.getHook().sendMessage(message).queue();
            else {
                MessageEditData editor = MessageEditData.fromCreateData(message);
                slashEvent.getHook().editOriginal(editor).queue();
            }
            return;
        }
        if (channel.getType() == ChannelType.TEXT && !channel.canTalk()) {
            sendPrivateMessage(message);
        }
        channel.sendMessage(message).submit()
                .thenAccept(m -> {
                    queueMessagePurge(m, purgeAfter);
                })
                .exceptionally(throwable -> {
                    if (channel.getType() == ChannelType.TEXT) {
                        sendPrivateMessage(message);
                    } else {
                        log.warn(String.format("Failed to send private message to %s with text %s", getUser().getName(), message), throwable);
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

    public void sendMessage(MessageCreateData message) {
        sendMessage(message, 0);
    }

    public void sendMessage(MessageEmbed embed) {
        sendMessage(embed, 0);
    }
}
