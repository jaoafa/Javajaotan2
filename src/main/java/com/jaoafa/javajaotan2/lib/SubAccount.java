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

package com.jaoafa.javajaotan2.lib;

import com.jaoafa.javajaotan2.Main;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class SubAccount {
    private Guild jMSGuild = null;
    private boolean exists = true;
    private User user = null;
    private long discordId;
    private SubAccount main = null;

    public SubAccount(long discordId) {
        try {
            this.discordId = discordId;
            this.jMSGuild = Main.getJDA().getGuildById(597378876556967936L);

            JavajaotanData.getMainMySQLDBManager().getConnection();

            try {
                this.user = Main.getJDA().retrieveUserById(discordId).complete();
            } catch (ErrorResponseException e) {
                exists = false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            exists = false;
        }
    }

    public SubAccount(Member member) {
        try {
            this.discordId = member.getIdLong();
            this.jMSGuild = Main.getJDA().getGuildById(597378876556967936L);

            JavajaotanData.getMainMySQLDBManager().getConnection();

            this.user = member.getUser();
        } catch (SQLException e) {
            e.printStackTrace();
            exists = false;
        }
    }

    public boolean setMainAccount(@NotNull SubAccount mainAccount) {
        Role SubAccountRole = jMSGuild.getRoleById(753047225751568474L);
        if (SubAccountRole == null) {
            return false;
        }
        try {
            MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
            Connection conn = manager.getConnection();

            PreparedStatement stmt = conn.prepareStatement("INSERT INTO subaccount (name, discriminator, disid, main_name, main_discriminator, main_disid, created_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);");
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getDiscriminator());
            stmt.setLong(3, user.getIdLong());
            stmt.setString(4, mainAccount.getUser().getName());
            stmt.setString(5, mainAccount.getUser().getDiscriminator());
            stmt.setLong(6, mainAccount.getUser().getIdLong());
            stmt.executeUpdate();
        } catch (SQLException e) {
            return false;
        }
        jMSGuild.addRoleToMember(user.getId(), SubAccountRole).queue();
        return true;
    }

    public boolean removeMainAccount() {
        Role SubAccountRole = jMSGuild.getRoleById(753047225751568474L);
        if (SubAccountRole == null) {
            return false;
        }
        try {
            MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
            Connection conn = manager.getConnection();

            PreparedStatement stmt = conn.prepareStatement("DELETE FROM subaccount WHERE disid = ?");
            stmt.setLong(1, user.getIdLong());
            stmt.executeUpdate();
        } catch (SQLException e) {
            return false;
        }
        jMSGuild.removeRoleFromMember(user.getId(), SubAccountRole).queue();
        return true;
    }

    public boolean isExists() {
        return exists;
    }

    public boolean isSubAccount() {
        return getMainAccount() != null;
    }

    public long getDiscordId() {
        return discordId;
    }

    public User getUser() {
        return user;
    }

    public SubAccount getMainAccount() {
        if (main != null) {
            return main;
        }
        try {
            MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
            Connection conn = manager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM subaccount WHERE disid = ?")) {
                stmt.setLong(1, discordId);
                try (ResultSet res = stmt.executeQuery()) {
                    if (res.next()) {
                        main = new SubAccount(res.getLong("main_disid"));
                    }
                }
            }
            return main;
        } catch (SQLException e) {
            return null;
        }
    }

    public Set<SubAccount> getSubAccounts() {
        try {
            MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
            Connection conn = manager.getConnection();
            PreparedStatement stmt_sub = conn.prepareStatement("SELECT * FROM subaccount WHERE main_disid = ?");
            stmt_sub.setLong(1, discordId);
            ResultSet res_sub = stmt_sub.executeQuery();
            final Set<SubAccount> subAccounts = new HashSet<>();
            while (res_sub.next()) {
                subAccounts.add(new SubAccount(res_sub.getLong("disid")));
            }
            res_sub.close();
            stmt_sub.close();
            return subAccounts;
        } catch (SQLException e) {
            return null;
        }
    }
}
