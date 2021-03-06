package dev.armadeus.core.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.armadeus.bot.api.config.ArmaConfig;
import dev.armadeus.bot.api.util.ConfigUtil;
import dev.armadeus.core.ArmaCoreImpl;
import lombok.Getter;

import java.net.URL;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

@Getter
public class ArmaConfigImpl implements ArmaConfig {

    private transient CommentedFileConfig config;

    public ArmaConfigImpl(CommentedFileConfig config) {
        this.config = config;
    }

    @Override
    public UUID getUuid() {
        String id = config.getOrElse(asList("arma-core", "uuid"), "-1");
        UUID uid;
        try {
            uid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            uid = UUID.randomUUID();
            config.set(asList("arma-core", "uuid"), String.valueOf(uid));
            config.save();
        }
        return uid;
    }

    @Override
    public boolean isDevMode() {
        return ArmaCoreImpl.options().has("dev-mode");
    }

    @Override
    public String getToken() {
        return ArmaCoreImpl.options().has("token") ? (String) ArmaCoreImpl.options().valueOf("token") : config.get(asList("arma-core", "token"));
    }

    @Override
    public List<Integer> getShards() {
        return config.get(asList("arma-core", "shards"));
    }

    @Override
    public Integer getShardsTotal() {
        return config.get(asList("arma-core", "shardsTotal"));
    }

    @Override
    public Set<Long> getOwnerIds() {
        return Set.copyOf(config.get(asList("arma-core", "ownerIds")));
    }

    @Override
    public Set<Long> getDeveloperIds() {
        Set<Long> joined = new HashSet<>();
        joined.addAll(config.get(asList("arma-core", "developerIds")));
        joined.addAll(config.get(asList("arma-core", "ownerIds")));
        return joined;
    }

    public boolean isDatabaseEnabled() {
        return config.get(asList("database", "enabled"));
    }

    public Database getDatabaseInfo() {
        return new ObjectConverter().toObject(config.get("database"), Database::new);
    }

    public static ArmaConfig read(Path path) {
        URL defaultConfigLocation = ArmaConfigImpl.class.getClassLoader().getResource("configs/arma-core.toml");
        checkNotNull(defaultConfigLocation, "Default Arma-Core configuration does not exist");

        CommentedConfig defaults = TomlFormat.instance().createParser().parse(defaultConfigLocation);

        CommentedFileConfig config = CommentedFileConfig.builder(path)
                .preserveInsertionOrder()
                .defaultData(defaultConfigLocation)
                .autosave()
                .sync()
                .build();
        config.load();

        ConfigUtil.merge(defaults, config);
        return new ArmaConfigImpl(config);
    }

    @Override
    public Map<String, CommentedConfig> getMetadata() {
        return config.getOrElse("metadata", CommentedConfig.inMemory()).entrySet().stream().collect(Collectors.toMap(UnmodifiableConfig.Entry::getKey, UnmodifiableConfig.Entry::getValue));
    }

    @Override
    public CommentedConfig getMetadata(String key) {
        return config.get(asList("metadata", key));
    }

    public CommentedConfig getMetadataOrInitialize(String key, Consumer<CommentedConfig> initializer) {
        CommentedConfig metaConf = getMetadata(key);
        if (initializer != null && metaConf == null) {
            metaConf = config.createSubConfig();
            initializer.accept(metaConf);
            if (metaConf.isEmpty()) {
                throw new IllegalArgumentException("Configurations cannot be empty!");
            }
            setMetadata(key, metaConf);
        }
        return metaConf;
    }

    @Override
    public ArmaConfig setMetadata(String key, CommentedConfig config) {
        this.config.set(asList("metadata", key), config);
        return this;
    }

    @Override
    public Connection createDatabaseConnection() {
        if (!isDatabaseEnabled()) {
            logger.warn("Attempted to establish unconfigured database connection");
            return null;
        }
        try {
            return DriverManager.getConnection(getDatabaseInfo().getUri(), getDatabaseInfo().getUsername(), getDatabaseInfo().getPassword());
        } catch (SQLException e) {
            logger.warn("Failed to create database connection", e);
            return null;
        }
    }

    @Override
    public RestConfig getRestConfig() {
        return new ObjectConverter().toObject(config.get("rest"), RestConfig::new);
    }

    @Getter
    public static class Database {

        boolean enabled;
        String username = "postgres";
        String password = "password";
        String uri = "jdbc:postgresql://localhost:5432/postgres";
    }
}