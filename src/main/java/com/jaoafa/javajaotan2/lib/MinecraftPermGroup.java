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

import java.sql.*;
import java.util.Arrays;
import java.util.UUID;

public class MinecraftPermGroup {
    private boolean isFound = false;
    private String groupName = null;
    private Timestamp expiredAt = null;

    public MinecraftPermGroup(UUID uuid) throws SQLException {
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        if (manager == null) {
            throw new IllegalStateException("MySQLDBManager is null.");
        }

        Connection conn = manager.getConnection();
        String sql = "SELECT * FROM permissions WHERE uuid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet res = stmt.executeQuery()) {
                if (!res.next()) {
                    return;
                }
                isFound = true;
                groupName = res.getString("permission");
                expiredAt = res.getTimestamp("expire_at");
            }
        }
    }

    /**
     * 権限グループ情報を取得できたかどうかを返します。
     *
     * @return 権限グループ情報を取得できた場合はtrue、そうでない場合はfalse
     */
    public boolean isFound() {
        return isFound;
    }

    /**
     * 権限グループに対応する {@link Group} を返します。
     *
     * @return 権限グループに対応する {@link Group}
     */
    public Group getGroup() {
        return Arrays
            .stream(Group.values())
            .filter(p -> p.name().equalsIgnoreCase(groupName))
            .findFirst()
            .orElse(Group.UNKNOWN);
    }

    /**
     * 権限グループが一時的なものかどうかを返します。
     *
     * @return 権限グループが一時的なものの場合はtrue、そうでない場合はfalse
     */
    public boolean isTemporary() {
        return expiredAt != null;
    }

    /**
     * 権限グループの有効期限を返します。
     *
     * @return 権限グループの有効期限
     */
    public Timestamp getExpiredAt() {
        return expiredAt;
    }

    public enum Group {
        ADMIN,
        MODERATOR,
        REGULAR,
        COMMUNITYREGULAR,
        VERIFIED,
        DEFAULT,
        UNKNOWN
    }
}
