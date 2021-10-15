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

package com.jaoafa.javajaotan2.command;

import cloud.commandframework.Command;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.jda.JDACommandSender;
import cloud.commandframework.meta.CommandMeta;
import com.jaoafa.javajaotan2.lib.CommandPremise;
import com.jaoafa.javajaotan2.lib.JavajaotanCommand;
import com.jaoafa.javajaotan2.lib.Roles;
import com.jaoafa.javajaotan2.tasks.Task_CheckMailVerified;
import com.jaoafa.javajaotan2.tasks.Task_MemberOrganize;
import com.jaoafa.javajaotan2.tasks.Task_PermSync;
import com.jaoafa.javajaotan2.tasks.Task_SyncOtherServerPerm;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.Arrays;

public class Cmd_Test implements CommandPremise {
    @Override
    public JavajaotanCommand.Detail details() {
        return new JavajaotanCommand.Detail(
            "test", // コマンド名小文字
            Arrays.asList("test1", "test2"), // コマンドのエイリアス
            "テストコマンドです。", // コマンドの説明
            Arrays.asList(Roles.Admin, Roles.Moderator) // このコマンドを使用できるロール
        );
    }

    @Override
    public JavajaotanCommand.Cmd register(Command.Builder<JDACommandSender> builder) {
        return new JavajaotanCommand.Cmd(
            builder
                .meta(CommandMeta.DESCRIPTION, "あなたのユーザーIDを表示します。")
                .literal("userid")
                .handler(context -> execute(context, this::getUserId))
                .build(),
            builder
                .meta(CommandMeta.DESCRIPTION, "PermSyncの動作テストを行います")
                .literal("permsync")
                .handler(context -> execute(context, this::runPermSync))
                .build(),
            builder
                .meta(CommandMeta.DESCRIPTION, "MemberOrganizeの動作テストを行います")
                .literal("memberorganize")
                .handler(context -> execute(context, this::runMemberOrganize))
                .build(),
            builder
                .meta(CommandMeta.DESCRIPTION, "CheckMailVerifiedの動作テストを行います")
                .literal("checkmailverified")
                .handler(context -> execute(context, this::runCheckMailVerified))
                .build(),
            builder
                .meta(CommandMeta.DESCRIPTION, "SyncOtherServerPermの動作テストを行います")
                .literal("otherserverpermsync")
                .handler(context -> execute(context, this::runSyncOtherServerPerm))
                .build()
        );
    }

    private void getUserId(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        message.reply(MessageFormat.format("あなたのユーザーIDは `{0}` です。", member.getId())).queue();
    }

    private void runPermSync(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            message.reply("このコマンドを実行するにはADMINISTRATOR権限が必要です。").queue();
            return;
        }
        message.reply(":eyes:").queue();
        new Task_PermSync(true).execute(null);
    }

    private void runMemberOrganize(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            message.reply("このコマンドを実行するにはADMINISTRATOR権限が必要です。").queue();
            return;
        }
        message.reply(":eyes:").queue();
        new Task_MemberOrganize(true).execute(null);
    }

    private void runCheckMailVerified(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            message.reply("このコマンドを実行するにはADMINISTRATOR権限が必要です。").queue();
            return;
        }
        message.reply(":eyes:").queue();
        new Task_CheckMailVerified(true).execute(null);
    }

    private void runSyncOtherServerPerm(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            message.reply("このコマンドを実行するにはADMINISTRATOR権限が必要です。").queue();
            return;
        }
        message.reply(":eyes:").queue();
        new Task_SyncOtherServerPerm(true).execute(null);
    }
}
