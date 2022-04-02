package dev.armadeus.bot.api.guild;

import com.electronwill.nightconfig.core.Config;
import dev.armadeus.bot.api.config.GuildConfig;
import net.dv8tion.jda.api.entities.Guild;

public interface GuildManager {

    Config getDefaultConfig();

    GuildConfig getConfigFor(Guild guild);

    GuildConfig getConfigFor(long guildId);

    void shutdown();
}
