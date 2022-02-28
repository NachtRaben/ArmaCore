package dev.armadeus.bot.api.events;

import dev.armadeus.bot.api.config.GuildConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class GuildConfigReloadEvent {

    private final long guildId;
    private final GuildConfig config;
}
