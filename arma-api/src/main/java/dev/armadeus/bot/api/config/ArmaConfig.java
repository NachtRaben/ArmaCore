package dev.armadeus.bot.api.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public interface ArmaConfig {

    Logger logger = LoggerFactory.getLogger(ArmaConfig.class);

    UUID getUuid();

    boolean isDevMode();

    String getToken();

    List<Integer> getShards();

    Integer getShardsTotal();

    Set<Long> getOwnerIds();

    Set<Long> getDeveloperIds();

    boolean isDatabaseEnabled();

    Map<String, CommentedConfig> getMetadata();

    CommentedConfig getMetadata(String key);

    CommentedConfig getMetadataOrInitialize(String key, Consumer<CommentedConfig> config);

    ArmaConfig setMetadata(String key, CommentedConfig config);

    Connection createDatabaseConnection();

    RestConfig getRestConfig();

    @Getter
    class RestConfig {

        boolean enabled;
        String bind;
    }
}
