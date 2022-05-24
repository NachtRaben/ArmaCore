package dev.armadeus.bot.api.util;

import dev.armadeus.bot.api.command.DiscordCommandIssuer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class EmbedUtils {

    public static EmbedBuilder newBuilder(DiscordCommandIssuer user) {
        if (user.isFromGuild())
            return newBuilder(user.getMember());
        else
            return newBuilder(user.getUser());
    }

    public static EmbedBuilder newBuilder(User user) {
        return new EmbedBuilder();
    }

    public static EmbedBuilder newBuilder(Member user) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(user.getGuild().getSelfMember().getRoles().get(0).getColor());
        return builder;
    }
}
