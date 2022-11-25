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

package com.jaoafa.javajaotan2.command;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jaoafa.javajaotan2.lib.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.List;

public class Cmd_UserKey extends CommandWithActions {
    private final MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();

    public Cmd_UserKey() {
        this.name = "userkey";
        this.help = "ユーザーキーに関する処理を行います。（運営のみ利用可能）";
        this.actions = List.of(
            new CommandAction("info", this::info, List.of("key"), "指定されたユーザーキーに紐づくアカウント情報を表示します。"),
            new CommandAction("use", this::use, List.of("key"), "指定されたユーザーキーを使用します。")
        );
        this.arguments = CommandAction.getArguments(this.actions);
    }

    @Override
    protected void execute(CommandEvent event) {
        CommandAction.execute(this, event);
    }

    private void info(CommandEvent event, List<String> argNames) {
        if (checkStatusFailed(event)) return;

        Message message = event.getMessage();
        CommandArgument args = new CommandArgument(event.getArgs(), argNames, CommandAction.getStartArgumentIndex(this, event));
        String key = args.getString("key");

        UserKeyResult result = getUserKey(manager, key);
        if (!result.status()) {
            message.reply(result.message()).queue();
            return;
        }
        UserKey userKey = result.userkey();
        String player = userKey.player();
        String uuid = userKey.uuid();
        boolean used = userKey.used();
        Timestamp createdAt = userKey.created_time();

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("ユーザーキー情報")
            .setDescription("ユーザーキー: " + key)
            .addField("プレイヤー", "[%s](https://users.jaoafa.com/%s)".formatted(player, uuid), true)
            .addField("使用済みか", used ? "使用済み" : "未使用", true)
            .addField("作成日時", createdAt.toString(), true)
            .setTimestamp(createdAt.toInstant());
        message.replyEmbeds(embed.build()).queue();
    }

    private void use(CommandEvent event, List<String> argNames) {
        if (checkStatusFailed(event)) return;

        Message message = event.getMessage();
        CommandArgument args = new CommandArgument(event.getArgs(), argNames, CommandAction.getStartArgumentIndex(this, event));
        String key = args.getString("key");

        try {
            UserKeyResult result = getUserKey(manager, key);
            if (!result.status()) {
                message.reply(result.message()).queue();
                return;
            }
            UserKey userKey = result.userkey();
            String player = userKey.player();
            String uuid = userKey.uuid();
            boolean used = userKey.used();
            Timestamp createdAt = userKey.created_time();

            if (used) {
                message.reply("指定されたユーザーキーは既に使用済みです。").queue();
                return;
            }

            Connection conn = manager.getConnection();
            PreparedStatement usedStmt = conn.prepareStatement("UPDATE userkey SET used = true WHERE userkey = ?");
            usedStmt.setString(1, key);
            usedStmt.executeUpdate();
            usedStmt.close();

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("ユーザーキー使用済み処理完了")
                .setDescription("ユーザーキー: " + key)
                .addField("プレイヤー", "[%s](https://users.jaoafa.com/%s)".formatted(player, uuid), true)
                .addField("作成日時", createdAt.toString(), true)
                .setTimestamp(createdAt.toInstant());
            message.replyEmbeds(embed.build()).queue();
        } catch (SQLException e) {
            e.printStackTrace();
            message.reply("SQLException: " + e.getMessage()).queue();

            if (JavajaotanData.getRollbar() != null) {
                JavajaotanData.getRollbar().error(e);
            }
        }
    }

    record UserKey(String userKey, String player, String uuid, boolean used, Timestamp created_time) {
    }

    record UserKeyResult(boolean status, String message, UserKey userkey) {
    }

    @NotNull
    UserKeyResult getUserKey(MySQLDBManager manager, String userKey) {
        try {
            Connection conn = manager.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM userkey WHERE userkey = ?");
            stmt.setString(1, userKey);
            ResultSet row = stmt.executeQuery();
            if (!row.next()) {
                row.close();
                stmt.close();
                return new UserKeyResult(
                    false,
                    "指定されたユーザーキーは存在しません。",
                    null
                );
            }
            String player = row.getString("player");
            String uuid = row.getString("uuid");
            boolean used = row.getBoolean("used");
            Timestamp createdAt = row.getTimestamp("created_time");

            row.close();
            stmt.close();
            return new UserKeyResult(
                true,
                null,
                new UserKey(userKey, player, uuid, used, createdAt)
            );
        } catch (SQLException e) {
            return new UserKeyResult(
                false,
                "SQLException: " + e.getMessage(),
                null
            );
        }
    }

    private boolean checkStatusFailed(CommandEvent event) {
        Message message = event.getMessage();
        Member member = event.getMember();
        if (!JavajaotanLibrary.isGrantedRole(member, Roles.Admin.getRole()) && !JavajaotanLibrary.isGrantedRole(member, Roles.Moderator.getRole())) {
            message.reply("このコマンドは運営のみ使用できます。").queue();
            return true;
        }
        if (manager == null) {
            message.reply("データベースへの接続が確立されていません。").queue();
            return true;
        }

        return false;
    }
}
