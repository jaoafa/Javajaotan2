/*
 * jaoLicense
 *
 * Copyright (c) 2023 jao Minecraft Server
 *
 * The following license applies to this project: jaoLicense
 *
 * Japanese: https://github.com/jaoafa/jao-Minecraft-Server/blob/master/jaoLICENSE.md
 * English: https://github.com/jaoafa/jao-Minecraft-Server/blob/master/jaoLICENSE-en.md
 */

package com.jaoafa.javajaotan2.lib;

import java.sql.*;
import java.util.UUID;

public class Socials {
    private final UUID uuid;
    private String twitterId;
    private String githubId;
    private String homeUrl;
    private Timestamp updatedAt;

    private Socials(UUID uuid) {
        this.uuid = uuid;
    }

    public static Socials get(UUID uuid) throws SQLException {
        Socials socials = new Socials(uuid);
        socials.pullSocials(uuid);
        return socials;
    }

    private void initSocials(UUID uuid) throws SQLException {
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();

        Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO socials (uuid, created_at) VALUES (?, CURRENT_TIMESTAMP)");
        stmt.setString(1, uuid.toString());
        stmt.execute();
    }

    public void pullSocials(UUID uuid) throws SQLException {
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();

        Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM socials WHERE uuid = ?");
        stmt.setString(1, uuid.toString());
        ResultSet result = stmt.executeQuery();

        if (!result.next()) {
            initSocials(uuid);
            this.updatedAt = new Timestamp(System.currentTimeMillis());
            return;
        }

        this.twitterId = result.getString("twitterId");
        this.githubId = result.getString("githubId");
        this.homeUrl = result.getString("homeUrl");
        this.updatedAt = result.getTimestamp("update_at");
    }

    private void pushSocials() throws SQLException {
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();

        Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement("UPDATE socials SET twitterId = ?, githubId = ?, homeUrl = ? WHERE uuid = ?");
        stmt.setString(1, this.twitterId);
        stmt.setString(2, this.githubId);
        stmt.setString(3, this.homeUrl);
        stmt.setString(4, this.uuid.toString());
        stmt.executeUpdate();
        this.updatedAt = new Timestamp(System.currentTimeMillis()); // 実際にDBに更新された日時じゃないから考え物だけど
    }

    public void setTwitterId(String twitterId) throws SQLException {
        if (twitterId != null && !twitterId.matches("^[0-9]+$")) {
            throw new IllegalArgumentException("Twitter ID is not number.");
        }
        this.twitterId = twitterId;
        pushSocials();
    }

    public void setGitHubId(String githubId) throws SQLException {
        this.githubId = githubId;
        pushSocials();
    }

    public void setHomeUrl(String homeUrl) throws SQLException {
        this.homeUrl = homeUrl;
        pushSocials();
    }

    public String getTwitterId() {
        return this.twitterId;
    }

    public String getGitHubId() {
        return this.githubId;
    }

    public String getHomeUrl() {
        return this.homeUrl;
    }

    public Timestamp getUpdatedAt() {
        return this.updatedAt;
    }
}
