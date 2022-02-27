package dev.armadeus.bot.rest;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.json.JsonFormat;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.armadeus.bot.api.ArmaCore;
import dev.armadeus.bot.api.config.ArmaConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import spark.Spark;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class RestManager {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static void init(ArmaCore core) {
        log.info("Initializing Rest API");
        ArmaConfig.RestConfig config = core.armaConfig().getRestConfig();
        String address = config.getBind().split(":")[0];
        int port = Integer.parseInt(config.getBind().split(":")[1]);
        Preconditions.checkState(config.isEnabled(), "RestManager was called without being initialized");
        Spark.ipAddress(address);
        Spark.port(port);
        Spark.get("/user/:userid", (req, res) -> {
            long userId = Long.parseLong(req.params("userid"));
            Map<Long, GuildData> guilds = core.shardManager().getGuilds().stream().filter(g -> {
                Member m = g.retrieveMemberById(userId).complete();
                return m != null && (m.isOwner() || m.hasPermission(Permission.MANAGE_SERVER));
            }).collect(Collectors.toMap(ISnowflake::getIdLong, g -> {
                Member owner = g.retrieveOwner().complete();
                return new GuildData(g.getName(), g.getIconUrl(), g.getOwnerIdLong(), owner.getEffectiveName());
            }));
            return gson.toJson(guilds);
        });
        Spark.path("/config/:guildid", () -> {
            Spark.get("/", (req, res) -> {
                Config conf = core.guildManager().getConfigFor(Long.parseLong(req.params("guildid"))).getRawConfig();
                return JsonFormat.minimalInstance().createWriter().writeToString(conf);
            });
            Spark.get("/:path", (req, res) -> {
                String path = req.params(":path");
                Config conf = core.guildManager().getConfigFor(Long.parseLong(req.params("guildid"))).getRawConfig();
                Object value = conf.getOrElse(path, Config.inMemory());
                return (value instanceof Config) ? JsonFormat.fancyInstance().createWriter().writeToString((Config) value) : value;
            });
            Spark.post("/set/:path", (req, res) -> {
                return "No U";
            });
        });
    }

    @AllArgsConstructor
    static class GuildData {

        String name;
        String iconUrl;
        long ownerId;
        String ownerName;
    }
}
