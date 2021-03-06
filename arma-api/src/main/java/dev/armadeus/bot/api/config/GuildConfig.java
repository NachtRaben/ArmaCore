package dev.armadeus.bot.api.config;

import com.electronwill.nightconfig.core.Config;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public interface GuildConfig {

    Config getRawConfig();

    // Command Cooldown
    int getCommandCooldown();

    GuildConfig setCommandCooldown(int cooldown);

    int getPurgeDelay();

    GuildConfig setPurgeDelay(int delay);

    boolean isDevGuild();

    GuildConfig setDevGuild(boolean enabled);

    // Command Deletion
    boolean deleteCommandMessages();

    GuildConfig deleteCommandMessages(boolean delete);

    // Blacklisted Commands
    Set<String> getDisabledCommands();

    GuildConfig setDisabledCommands(Set<String> permissions);

    GuildConfig addDisabledCommand(String... permissions);

    GuildConfig removeDisabledCommand(String... permissions);

    // Blacklisted Commands Per Role
    Set<String> getDisabledCommandsForRole(long roleId);

    GuildConfig setDisabledCommandsForRole(long roleId, Set<String> permissions);

    GuildConfig addDisabledCommandsForRole(long roleId, String... permissions);

    GuildConfig removeDisabledCommandsForRole(long roleId, String... permissions);

    Map<String, Config> getMetadata();

    Config getMetadata(String key);

    Config getMetadataOrInitialize(String key, Consumer<Config> config);

    GuildConfig setMetadata(String key, Config config);

    Guild getGuild();
}
