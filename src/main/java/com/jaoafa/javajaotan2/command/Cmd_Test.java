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
import com.jaoafa.javajaotan2.lib.CommandAction;
import com.jaoafa.javajaotan2.lib.CommandArgument;
import com.jaoafa.javajaotan2.lib.CommandWithActions;
import com.jaoafa.javajaotan2.tasks.Task_CheckMailVerified;
import com.jaoafa.javajaotan2.tasks.Task_MemberOrganize;
import com.jaoafa.javajaotan2.tasks.Task_PermSync;
import com.jaoafa.javajaotan2.tasks.Task_SyncOtherServerPerm;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import java.text.MessageFormat;
import java.util.List;

public class Cmd_Test extends CommandWithActions {
    public Cmd_Test() {
        this.name = "test";
        this.help = "テストコマンドです。";
        this.guildOnly = true;
        this.hidden = true;
        this.actions = List.of(
            new CommandAction("userid", this::getUserId),
            new CommandAction("permsync", this::runPermSync),
            new CommandAction("memberorganize", this::runMemberOrganize),
            new CommandAction("checkmailverified", this::runCheckMailVerified),
            new CommandAction("otherserverpermsync", this::runSyncOtherServerPerm)
        );
    }

    @Override
    protected void execute(CommandEvent event) {
        CommandAction.execute(this, event);
    }

    private void getUserId(CommandEvent event) {
        Member member = event.getMember();
        Message message = event.getMessage();
        message.reply(MessageFormat.format("あなたのユーザーIDは `{0}` です。", member.getId())).queue();
    }

    private void runPermSync(CommandEvent event) {
        Member member = event.getMember();
        Message message = event.getMessage();
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            message.reply("このコマンドを実行するにはADMINISTRATOR権限が必要です。").queue();
            return;
        }
        CommandArgument args = new CommandArgument(event.getArgs());
        boolean dryRun = args.getOptionalBoolean(0, true);
        message.reply(":eyes:").queue();
        new Task_PermSync(dryRun).execute(null);
    }

    private void runMemberOrganize(CommandEvent event) {
        Member member = event.getMember();
        Message message = event.getMessage();
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            message.reply("このコマンドを実行するにはADMINISTRATOR権限が必要です。").queue();
            return;
        }
        CommandArgument args = new CommandArgument(event.getArgs());
        boolean dryRun = args.getOptionalBoolean(0, true);
        message.reply(":eyes:").queue();
        new Task_MemberOrganize(dryRun).execute(null);
    }

    private void runCheckMailVerified(CommandEvent event) {
        Member member = event.getMember();
        Message message = event.getMessage();
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            message.reply("このコマンドを実行するにはADMINISTRATOR権限が必要です。").queue();
            return;
        }
        CommandArgument args = new CommandArgument(event.getArgs());
        boolean dryRun = args.getOptionalBoolean(0, true);
        message.reply(":eyes:").queue();
        new Task_CheckMailVerified(dryRun).execute(null);
    }

    private void runSyncOtherServerPerm(CommandEvent event) {
        Member member = event.getMember();
        Message message = event.getMessage();
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            message.reply("このコマンドを実行するにはADMINISTRATOR権限が必要です。").queue();
            return;
        }
        CommandArgument args = new CommandArgument(event.getArgs());
        boolean dryRun = args.getOptionalBoolean(0, true);
        message.reply(":eyes:").queue();
        new Task_SyncOtherServerPerm(dryRun).execute(null);
    }
}
