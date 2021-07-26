/*
 * jaoLicense
 *
 * Copyright (c) 2021 jao Minecraft Server
 *
 * The following license applies to this project: jaoLicense
 *
 * Japanese: https://github.com/jaoafa/jao-Minecraft-Server/blob/master/jaoLICENSE.md
 * English: https://github.com/jaoafa/jao-Minecraft-Server/blob/master/jaoLICENSE-en.md
 */

package com.jaoafa.javajaotan2.event;

import com.jaoafa.javajaotan2.lib.Channels;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;

public class Event_WarnUsers extends ListenerAdapter {
    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
        if (event.getGuild().getIdLong() != 597378876556967936L) { // jMS Gamers Club
            return;
        }
        File file = new File("warning_users.json");
        if (!file.exists()) {
            return;
        }
        User user = event.getUser();
        try {
            JSONObject object = new JSONObject(Files.readString(file.toPath()));
            if (!object.has(user.getId())) {
                return;
            }
            TextChannel channel = event.getGuild().getTextChannelById(Channels.jaotan.getChannelId());
            if (channel == null) {
                return;
            }
            EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.RED)
                .setTitle(user.getAsTag() + " は警告対象者です")
                .setAuthor(user.getAsTag(), "https://discord.com/users/" + user.getId(), user.getAvatarUrl())
                .setDescription("理由: " + object.getString(user.getId()))
                .setTimestamp(Instant.now())
                .setFooter("Javajaotan2 WarnUsers");
            channel.sendMessageEmbeds(builder.build()).queue();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
