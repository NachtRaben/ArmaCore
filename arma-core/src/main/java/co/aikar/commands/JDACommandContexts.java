package co.aikar.commands;

import co.aikar.commands.annotation.Author;
import co.aikar.commands.annotation.CrossGuild;
import co.aikar.commands.annotation.SelfUser;
import dev.armadeus.bot.api.command.DiscordCommandIssuer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// TODO: Message Keys !!!
public class JDACommandContexts extends CommandContexts<JDACommandExecutionContext> {

    private final JDACommandManager manager;
    private final ShardManager shardManager;

    public JDACommandContexts(JDACommandManager manager) {
        super(manager);
        this.manager = manager;
        this.shardManager = this.manager.getShardManager();
        this.registerIssuerOnlyContext(JDACommandEvent.class, CommandExecutionContext::getIssuer);
        this.registerIssuerOnlyContext(DiscordCommandIssuer.class, c -> (DiscordCommandIssuer) c.getIssuer());
        this.registerIssuerOnlyContext(MessageReceivedEvent.class, c -> c.getIssuer().getMessageEvent());
        this.registerIssuerOnlyContext(SlashCommandInteractionEvent.class, c -> c.getIssuer().getSlashEvent());
        this.registerIssuerOnlyContext(Message.class, c -> c.getIssuer().getMessage());
        this.registerIssuerOnlyContext(ChannelType.class, c -> c.getIssuer().getChannel().getType());
        this.registerIssuerOnlyContext(JDA.class, c -> c.getIssuer().getJda());
        this.registerIssuerOnlyContext(ShardManager.class, c -> shardManager);
        this.registerIssuerOnlyContext(Guild.class, c -> {
            MessageChannel channel = c.getIssuer().getChannel();
            if (channel.getType() == ChannelType.PRIVATE && !c.isOptional()) {
                throw new InvalidCommandArgument("This command can only be executed in a Guild.", false);
            } else {
                return c.getIssuer().getGuild();
            }
        });
        this.registerIssuerAwareContext(TextChannel.class, c -> {
            if (c.hasAnnotation(Author.class)) {
                return (TextChannel) c.getIssuer().getChannel();
            }
            boolean isCrossGuild = c.hasAnnotation(CrossGuild.class);
            String argument = c.popFirstArg(); // we pop because we are only issuer aware if we are annotated
            TextChannel channel = null;
            if (argument.startsWith("<#")) {
                String id = argument.substring(2, argument.length() - 1);
                channel = isCrossGuild ? shardManager.getTextChannelById(id) : c.getIssuer().getGuild().getTextChannelById(id);
            } else if (argument.matches("[0-9]+")) {
                long id = Long.parseLong(argument);
                channel = isCrossGuild ? shardManager.getTextChannelById(id) : c.getIssuer().getGuild().getTextChannelById(id);
            } else {
                List<TextChannel> channelList = isCrossGuild ? shardManager.getShards().stream().flatMap(jda -> jda.getTextChannelsByName(argument, true).stream()).collect(Collectors.toList()) :
                        c.getIssuer().getGuild().getTextChannelsByName(argument, true);
                if (channelList.size() > 1) {
                    throw new InvalidCommandArgument("Too many channels were found with the given name. Try with the `#channelname` syntax.", false);
                } else if (channelList.size() == 1) {
                    channel = channelList.get(0);
                }
            }
            if (channel == null) {
                throw new InvalidCommandArgument("Couldn't find a channel with that name or ID.");
            }
            return channel;
        });
        this.registerIssuerAwareContext(VoiceChannel.class, c -> {
            if (c.hasAnnotation(Author.class)) {
                return (VoiceChannel) c.getIssuer().getVoiceChannel();
            }
            boolean isCrossGuild = c.hasAnnotation(CrossGuild.class);
            String argument = c.popFirstArg(); // we pop because we are only issuer aware if we are annotated
            VoiceChannel channel = null;
            if (argument.startsWith("<#")) {
                String id = argument.substring(2, argument.length() - 1);
                channel = isCrossGuild ? shardManager.getVoiceChannelById(id) : c.getIssuer().getGuild().getVoiceChannelById(id);
            } else if (argument.matches("[0-9]+")) {
                long id = Long.parseLong(argument);
                channel = isCrossGuild ? shardManager.getVoiceChannelById(id) : c.getIssuer().getGuild().getVoiceChannelById(id);
            } else {
                List<VoiceChannel> channelList = isCrossGuild ? shardManager.getShards().stream().flatMap(jda -> jda.getVoiceChannelsByName(argument, true).stream()).collect(Collectors.toList()) :
                        c.getIssuer().getGuild().getVoiceChannelsByName(argument, true);
                if (channelList.size() > 1) {
                    throw new InvalidCommandArgument("Too many channels were found with the given name. Try with the `#channelname` syntax.", false);
                } else if (channelList.size() == 1) {
                    channel = channelList.get(0);
                }
            }
            if (channel == null) {
                throw new InvalidCommandArgument("Couldn't find a channel with that name or ID.");
            }
            return channel;
        });
        this.registerIssuerAwareContext(User.class, c -> {
            if (c.hasAnnotation(SelfUser.class)) {
                return c.getIssuer().getJda().getSelfUser();
            }
            String arg = c.getFirstArg();
            if (c.isOptional() && (arg == null || arg.isEmpty())) {
                return null;
            }
            arg = c.popFirstArg(); // we pop because we are only issuer aware if we are annotated
            User user = null;
            if (arg.startsWith("<@!")) { // for some reason a ! is added when @'ing and clicking their name.
                user = shardManager.getUserById(arg.substring(3, arg.length() - 1));
            } else if (arg.startsWith("<@")) { // users can /also/ be mentioned like this...
                user = shardManager.getUserById(arg.substring(2, arg.length() - 1));
            } else {
                User u = shardManager.getUserByTag(arg);
                if (u != null)
                    return u;

                String finalArg = arg;
                List<User> users = shardManager.getShards().stream().flatMap(jda -> jda.getUsersByName(finalArg, true).stream()).collect(Collectors.toList());
                if (users.size() > 1) {
                    throw new InvalidCommandArgument("Too many users were found with the given name. Try with the `@username#0000` syntax.", false);
                }
                if (!users.isEmpty()) {
                    user = users.get(0);
                }
            }
            if (user == null) {
                throw new InvalidCommandArgument("Could not find a user with that name or ID.");
            }
            return user;
        });
        this.registerContext(Role.class, c -> {
            boolean isCrossGuild = c.hasAnnotation(CrossGuild.class);
            String arg = c.popFirstArg();
            Role role = null;
            if (arg.startsWith("<@&")) {
                String id = arg.substring(3, arg.length() - 1);
                role = isCrossGuild ? shardManager.getRoleById(id) : c.getIssuer().getGuild().getRoleById(id);
            } else {
                List<Role> roles = isCrossGuild ? shardManager.getRolesByName(arg, true)
                        : c.getIssuer().getGuild().getRolesByName(arg, true);
                if (roles.size() > 1) {
                    throw new InvalidCommandArgument("Too many roles were found with the given name. Try with the `@role` syntax.", false);
                }
                if (!roles.isEmpty()) {
                    role = roles.get(0);
                }
            }
            if (role == null) {
                throw new InvalidCommandArgument("Could not find a role with that name or ID.");
            }
            return role;
        });
        this.registerIssuerAwareContext(File.class, c -> {
            CompletableFuture<File> future = null;
            if (c.getIssuer().getEvent() instanceof SlashCommandInteractionEvent e) {
                future = e.getOptionsByType(OptionType.ATTACHMENT).stream().findFirst().orElseThrow(() -> new InvalidCommandArgument("No attachment")).getAsAttachment().downloadToFile();
            } else if (c.getIssuer().getEvent() instanceof MessageReceivedEvent e) {
                future = e.getMessage().getAttachments().stream().findFirst().orElseThrow(() -> new InvalidCommandArgument("No attachment")).downloadToFile();
            }
            try {
                return Objects.requireNonNull(future).get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new InvalidCommandArgument("Unable to download attachment");
            }
        });
    }
}
