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

import com.jaoafa.javajaotan2.lib.JavajaotanData;
import com.jaoafa.javajaotan2.lib.MySQLDBManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class Event_UserDedDB extends ListenerAdapter {
    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        User user = event.getUser();

        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        try {
            Connection conn = manager.getConnection();
            PreparedStatement stmt = conn.prepareStatement("UPDATE discordlink SET dead_at = ? WHERE disid = ?");
            stmt.setTimestamp(1, Timestamp.from(Instant.now()));
            stmt.setString(2, user.getId());
            boolean bool = stmt.execute();
            if (!bool) {
                System.out.println("Event_UserDedDB: dead_at update failed");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
