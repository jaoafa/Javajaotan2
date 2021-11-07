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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Event_UserDedDB extends ListenerAdapter {
    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        User user = event.getUser();

        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        try {
            Connection conn = manager.getConnection();
            PreparedStatement stmt = conn.prepareStatement("UPDATE discordlink SET dead_at = CURRENT_TIMESTAMP WHERE disid = ?");
            stmt.setString(1, user.getId());
            boolean bool = stmt.execute();
            if (!bool) {
                System.out.println("Event_UserDedDB: dead_at update failed");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
