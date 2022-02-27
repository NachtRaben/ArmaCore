package dev.armadeus.bot.api.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;

@AllArgsConstructor
@Data
public class ArmaJDAInitEvent {

    private DefaultShardManagerBuilder builder;
}
