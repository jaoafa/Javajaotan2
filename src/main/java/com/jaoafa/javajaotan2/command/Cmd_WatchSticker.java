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
import com.jaoafa.javajaotan2.Main;
import com.jaoafa.javajaotan2.lib.CommandAction;
import com.jaoafa.javajaotan2.lib.CommandArgument;
import com.jaoafa.javajaotan2.lib.CommandWithActions;
import com.jaoafa.javajaotan2.lib.WatchStickers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.List;

public class Cmd_WatchSticker extends CommandWithActions {
    public Cmd_WatchSticker() {
        this.name = "watchsticker";
        this.help = "スタンプを監視する設定をします。";
        this.actions = List.of(
            new CommandAction("add", this::addWatch, List.of("log-channel"), "サーバをスタンプ監視対象に追加します。"),
            new CommandAction("remove", this::removeWatch, "サーバをスタンプ監視対象から削除します。"),
            new CommandAction("del", this::removeWatch, "サーバをスタンプ監視対象から削除します。"),
            new CommandAction("delete", this::removeWatch, "サーバをスタンプ監視対象から削除します。")
        );
        this.arguments = CommandAction.getArguments(this.actions);
    }

    @Override
    protected void execute(CommandEvent event) {
        CommandAction.execute(this, event);
    }

    private void addWatch(CommandEvent event, List<String> argNames) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();
        CommandArgument args = new CommandArgument(event.getArgs(), argNames, CommandAction.getStartArgumentIndex(this, event));
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            message.reply("このコマンドを実行するには、サーバの管理者権限を持っている必要があります。").queue();
            return;
        }
        Member selfMember = guild.getMember(Main.getJDA().getSelfUser());
        if (selfMember == null) {
            message.reply("Botユーザーのサーバメンバー情報を取得できませんでした。").queue();
            return;
        }
        if (!selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            message.reply("このコマンドを実行するには、jaotanがサーバの監査ログ閲覧権限を持っている必要があります。").queue();
            return;
        }
        MessageChannel log_channel = parseChannelInput(guild, args.getString("log-channel"));

        GuildChannel log_guild_channel = guild.getGuildChannelById(log_channel.getIdLong());
        if (log_guild_channel == null) {
            message.reply("ログチャンネルの情報を取得できませんでした。このサーバにあるチャンネルを指定していますか？").queue();
            return;
        }

        if (!selfMember.hasPermission(log_guild_channel, Permission.MESSAGE_SEND)) {
            message.reply("このコマンドを実行するには、jaotanがログチャンネルへの書き込み権限を持っている必要があります。").queue();
            return;
        }

        WatchStickers watchStickers = Main.getWatchStickers();
        watchStickers.addGuild(guild, log_channel);

        message.reply(String.format("このサーバ「%s」をスタンプ監視対象に追加しました。\nログチャンネル: <#%s>",
            guild.getName(),
            log_channel.getId())).queue();
    }

    private void removeWatch(CommandEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            message.reply("このコマンドを実行するには、サーバの管理者権限を持っている必要があります。").queue();
        }
        WatchStickers watchStickers = Main.getWatchStickers();
        if (watchStickers.getStickerGuild(guild).isEmpty()) {
            message.reply("このサーバはスタンプ監視対象ではありません。").queue();
            return;
        }
        watchStickers.removeGuild(guild);

        message.reply(String.format("このサーバ「%s」をスタンプ監視対象から削除しました。", guild.getName())).queue();
    }

    private MessageChannel parseChannelInput(Guild guild, String str) {
        // ID
        if (str.matches("\\d+")) {
            return guild.getTextChannelById(Long.parseLong(str));
        }
        // <#ID>
        if (str.matches("<#\\d+>")) {
            return guild.getTextChannelById(Long.parseLong(str.substring(2, str.length() - 1)));
        }
        // チャンネル名
        return guild.getTextChannels().stream()
            .filter(channel -> channel.getName().equals(str))
            .findFirst()
            .orElse(null);
    }
}
