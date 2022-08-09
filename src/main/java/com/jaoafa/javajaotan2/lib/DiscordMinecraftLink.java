/*
 * jaoLicense
 *
 * Copyright (c) 2022 jao Minecraft Server
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
import net.dv8tion.jda.api.entities.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DiscordMinecraftLink {
    private final boolean isFound;
    private boolean isLinked;

    private final String minecraftName;
    private final UUID minecraftUUID;
    private final String discordId;
    private final String discordName;
    private final String discordDiscriminator;
    private final Timestamp expiredAt;
    private final String disconnectPermGroup;
    private final Timestamp disconnectAt;
    private final Timestamp connectedAt;
    private final Timestamp lastLogin;

    public static DiscordMinecraftLink get(long discordId) throws SQLException {
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        if (manager == null) {
            throw new IllegalStateException("MySQLDBManager is null.");
        }
        Connection conn = manager.getConnection();
        String sql = "SELECT * FROM discordlink WHERE disid = ?";
        DiscordMinecraftLink link = null;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Long.toString(discordId));
            try (ResultSet res = stmt.executeQuery()) {
                while (res.next()) {
                    link = new DiscordMinecraftLink(res);

                    if (link.isLinked()) return link;
                }
            }
        }
        return link;
    }

    public static DiscordMinecraftLink get(UUID uuid) throws SQLException {
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        if (manager == null) {
            throw new IllegalStateException("MySQLDBManager is null.");
        }
        Connection conn = manager.getConnection();
        String sql = "SELECT * FROM discordlink WHERE uuid = ?";
        DiscordMinecraftLink link = null;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet res = stmt.executeQuery()) {
                while (res.next()) {
                    link = new DiscordMinecraftLink(res);

                    if (link.isLinked()) return link;
                }
            }
        }
        return link;
    }

    private DiscordMinecraftLink(ResultSet res) throws SQLException {
        this.isFound = true;

        this.isLinked = !res.getBoolean("disabled");
        this.minecraftName = res.getString("player");
        this.minecraftUUID = UUID.fromString(res.getString("uuid"));
        this.discordId = res.getString("disid");
        this.discordName = res.getString("name");
        this.discordDiscriminator = res.getString("discriminator");
        this.expiredAt = res.getTimestamp("expired_date");
        this.disconnectPermGroup = res.getString("dead_perm");
        this.disconnectAt = res.getTimestamp("dead_at");
        this.connectedAt = res.getTimestamp("date");

        this.lastLogin = getLastLogin(this.minecraftUUID);
    }

    /**
     * この連携を解除します。
     *
     * @return
     *
     * @throws IllegalStateException 対象の連携が見つからない、または既に解除済みの場合
     */
    public void disconnect() throws SQLException {
        if (!this.isLinked || !this.isFound) {
            throw new IllegalStateException("This link is not linked or not found.");
        }
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        if (manager == null) {
            throw new IllegalStateException("MySQLDBManager is null.");
        }
        Connection conn = manager.getConnection();

        Member member = getDiscordMember();

        MinecraftPermGroup mp = new MinecraftPermGroup(this.minecraftUUID);
        MinecraftPermGroup.Group group = mp.isFound() ? mp.getGroup() : null;
        if (JavajaotanLibrary.isGrantedRole(member, Roles.CommunityRegular.getRole())) {
            group = MinecraftPermGroup.Group.COMMUNITYREGULAR;
        }

        String sql = "UPDATE discordlink SET disabled = ?, dead_perm = ?, dead_at = CURRENT_TIMESTAMP WHERE uuid = ? AND disabled = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, true);
            stmt.setString(2, group != null ? group.name() : null);
            stmt.setString(3, minecraftUUID.toString());
            stmt.setBoolean(4, false);
            stmt.execute();
        }

        this.isLinked = false;
    }

    /**
     * （無効化されているかはともかく）一度でも連携されたことがあるかを返します。
     *
     * @return 連携されたことがあるかどうか
     */
    public boolean isFound() {
        return isFound;
    }

    /**
     * 現時点で連携されているかどうかを返します。<br>
     * disabled フラグが True の場合、この値は False になります。
     *
     * @return 連携されているかどうか
     */
    public boolean isLinked() {
        return isLinked;
    }

    /**
     * <b>連携時の</b> Minecraft ID を返します。
     *
     * @return <b>連携時の</b> Minecraft ID
     */
    public String getMinecraftName() {
        return minecraftName;
    }

    /**
     * Minecraft UUID を返します。
     *
     * @return Minecraft UUID
     */
    public UUID getMinecraftUUID() {
        return minecraftUUID;
    }

    /**
     * Discord ID を返します。
     *
     * @return Discord ID
     */
    public String getDiscordId() {
        return discordId;
    }

    /**
     * <b>連携時の</b> Discord Name を返します。
     *
     * @return Discord Name
     */
    public String getDiscordName() {
        return discordName;
    }

    /**
     * <b>連携時の</b> Discord Discriminator を返します。
     *
     * @return Discord Discriminator
     */
    public String getDiscordDiscriminator() {
        return discordDiscriminator;
    }

    /**
     * Discord {@link User} を返します。
     *
     * @return Discord {@link User}
     */
    public User getDiscordUser() {
        return Main.getJDA().getUserById(discordId);
    }

    /**
     * Discord {@link Member} を返します。
     *
     * @return Discord {@link Member}
     */
    public Member getDiscordMember() {
        Guild guild = Main.getJDA().getGuildById(Main.getConfig().getGuildId());
        if (guild == null) {
            throw new IllegalStateException("Guild is null.");
        }
        return guild.getMemberById(discordId);
    }

    /**
     * 「期限付きリンク継続期限日時」を返します。<br>
     * この値が NULL ではない場合、この値か最終ログインから3か月後のうち、最も遅い日に自動的に連携が解除されます。
     *
     * @return 期限付きリンク継続期限日時
     */
    public Timestamp getExpiredAt() {
        return expiredAt;
    }

    /**
     * 連携が解除されたときの権限グループを返します。
     *
     * @return 連携が解除されたときの権限グループ
     */
    public String getDisconnectPermGroup() {
        return disconnectPermGroup;
    }

    /**
     * 連携が解除された日時を返します。
     *
     * @return 連携が解除された日時
     */
    public Timestamp getDisconnectAt() {
        return disconnectAt;
    }

    /**
     * 連携日時を返します。
     *
     * @return 連携日時
     */
    public Timestamp getConnectedAt() {
        return connectedAt;
    }

    /**
     * 最終ログイン日時を返します
     *
     * @return 最終ログイン日時
     */
    public Timestamp getLastLogin() {
        return lastLogin;
    }

    /**
     * 指定した UUID の最終ログイン日時を返します。
     *
     * @return 最終ログイン日時
     */
    public static Timestamp getLastLogin(UUID uuid) throws SQLException {
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        if (manager == null) {
            throw new IllegalStateException("MySQLDBManager is null.");
        }
        Connection conn = manager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM login WHERE uuid = ? AND login_success = ? ORDER BY id DESC LIMIT 1")) {

            stmt.setString(1, uuid.toString());
            stmt.setBoolean(2, true);

            try (ResultSet result = stmt.executeQuery()) {
                return result.next() ? result.getTimestamp("date") : null;
            }
        }
    }

    /**
     * Minecraft アカウントを基準にすべての連携データを取得します。<br>
     * 同じ UUID を持った連携データが複数ある場合は、最後に登録されたものが返されます。
     *
     * @return 連携データ
     *
     * @throws SQLException          データベース操作時にエラーが発生した場合
     * @throws IllegalStateException データベース接続情報が定義されていない場合
     */
    public static List<DiscordMinecraftLink> getAllForMinecraft() throws SQLException {
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        if (manager == null) {
            throw new IllegalStateException("MySQLDBManager is null.");
        }
        Connection conn = manager.getConnection();
        List<DiscordMinecraftLink> connections = new ArrayList<>();
        String sql = "SELECT * FROM discordlink ORDER BY id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet res = stmt.executeQuery()) {
                while (res.next()) {
                    connections.add(new DiscordMinecraftLink(res));
                }
            }
        }
        // 被りを排除する: Minecraft UUID が重複している場合は、最後に登録されたもの以外を削除する
        connections = connections
            .stream()
            .filter(c -> c.getMinecraftUUID() != null)
            .collect(Collectors.groupingBy(DiscordMinecraftLink::getMinecraftUUID))
            .values()
            .stream()
            .map(l -> l.get(l.size() - 1))
            .toList();
        return connections;
    }

    /**
     * Discord アカウントを基準にすべての連携データを取得します。<br>
     * 同じ Discord ID を持った連携データが複数ある場合は、最後に登録されたものが返されます。
     *
     * @return 連携データ
     *
     * @throws SQLException          データベース操作時にエラーが発生した場合
     * @throws IllegalStateException データベース接続情報が定義されていない場合
     */
    public static List<DiscordMinecraftLink> getAllForDiscord() throws SQLException {
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        if (manager == null) {
            throw new IllegalStateException("MySQLDBManager is null.");
        }
        Connection conn = manager.getConnection();
        List<DiscordMinecraftLink> connections = new ArrayList<>();
        String sql = "SELECT * FROM discordlink ORDER BY id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet res = stmt.executeQuery()) {
                while (res.next()) {
                    connections.add(new DiscordMinecraftLink(res));
                }
            }
        }
        // 被りを排除する: Discord ID が重複している場合は、最後に登録されたもの以外を削除する
        connections = connections
            .stream()
            .filter(c -> c.getDiscordId() != null)
            .collect(Collectors.groupingBy(DiscordMinecraftLink::getDiscordId))
            .values()
            .stream()
            .map(l -> l.get(l.size() - 1))
            .toList();
        return connections;
    }
}
