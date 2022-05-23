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

import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.jda.JDACommandSender;
import cloud.commandframework.meta.CommandMeta;
import com.jaoafa.javajaotan2.lib.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.Arrays;

public class Cmd_UserKey implements CommandPremise {
    @Override
    public JavajaotanCommand.Detail details() {
        return new JavajaotanCommand.Detail(
            "userKey",
            "ユーザーキーに関する処理を行います。（運営のみ利用可能）",
            Arrays.asList(Roles.Admin, Roles.Moderator)
        );
    }

    @Override
    public JavajaotanCommand.Cmd register(Command.Builder<JDACommandSender> builder) {
        return new JavajaotanCommand.Cmd(
            builder
                .meta(CommandMeta.DESCRIPTION, "ユーザーキーについての情報を表示します。")
                .literal("info")
                .argument(StringArgument.of("key"))
                .handler(context -> execute(context, this::info))
                .build(),
            builder
                .meta(CommandMeta.DESCRIPTION, "ユーザーキーを利用済みにします。")
                .literal("use")
                .argument(StringArgument.of("key"))
                .handler(context -> execute(context, this::use))
                .build()
        );
    }

    private void info(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        String key = context.get("key");
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        if (manager == null) {
            message.reply("データベースへの接続が確立されていません。").queue();
            return;
        }

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

    private void use(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        String key = context.get("key");
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        if (manager == null) {
            message.reply("データベースへの接続が確立されていません。").queue();
            return;
        }
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

}
