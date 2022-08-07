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
import com.jaoafa.javajaotan2.lib.WatchEmojis;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.List;
import java.util.Optional;

public class Cmd_WatchEmoji extends CommandWithActions {
    public Cmd_WatchEmoji() {
        this.name = "watchemoji";
        this.help = "絵文字を監視する設定をします。";
        this.actions = List.of(
            new CommandAction("add", this::addWatch, List.of("log-channel", "list-channel")),
            new CommandAction("remove", this::removeWatch),
            new CommandAction("del", this::removeWatch),
            new CommandAction("delete", this::removeWatch),
            new CommandAction("regenerate", this::regenerate)
        );
    }

    @Override
    protected void execute(CommandEvent event) {
        CommandAction.execute(this, event);
    }

    private void addWatch(CommandEvent event, List<String> argNames) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();
        CommandArgument args = new CommandArgument(event.getArgs(), argNames);
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
        MessageChannel list_channel = parseChannelInput(guild, args.getString("list-channel"));

        GuildChannel log_guild_channel = guild.getGuildChannelById(log_channel.getIdLong());
        if (log_guild_channel == null) {
            message.reply("ログチャンネルの情報を取得できませんでした。このサーバにあるチャンネルを指定していますか？").queue();
            return;
        }
        GuildChannel list_guild_channel = guild.getGuildChannelById(list_channel.getIdLong());
        if (list_guild_channel == null) {
            message.reply("リストチャンネルの情報を取得できませんでした。このサーバにあるチャンネルを指定していますか？").queue();
            return;
        }

        if (!selfMember.hasPermission(log_guild_channel, Permission.MESSAGE_SEND)) {
            message.reply("このコマンドを実行するには、jaotanがログチャンネルへの書き込み権限を持っている必要があります。").queue();
            return;
        }

        if (!selfMember.hasPermission(list_guild_channel, Permission.MESSAGE_SEND)) {
            message.reply("このコマンドを実行するには、jaotanがリストチャンネルへの書き込み権限を持っている必要があります。").queue();
            return;
        }

        WatchEmojis watchEmojis = Main.getWatchEmojis();
        watchEmojis.addGuild(guild, log_channel, list_channel);

        message.reply(String.format("このサーバ「%s」を絵文字監視対象に追加しました。\nログチャンネル: <#%s>\nリストチャンネル: <#%s>",
            guild.getName(),
            log_channel.getId(),
            list_channel.getId())).queue();
    }

    private void removeWatch(CommandEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            message.reply("このコマンドを実行するには、サーバの管理者権限を持っている必要があります。").queue();
        }
        WatchEmojis watchEmojis = Main.getWatchEmojis();
        if (watchEmojis.getEmojiGuild(guild).isEmpty()) {
            message.reply("このサーバは絵文字監視対象ではありません。").queue();
            return;
        }
        watchEmojis.removeGuild(guild);

        message.reply(String.format("このサーバ「%s」を絵文字監視対象から削除しました。", guild.getName())).queue();
    }

    private void regenerate(CommandEvent event) {
        Guild guild = event.getGuild();
        Message message = event.getMessage();
        WatchEmojis watchEmojis = Main.getWatchEmojis();
        Optional<WatchEmojis.EmojiGuild> emojiGuildOpt = watchEmojis.getEmojiGuild(guild);
        if (emojiGuildOpt.isEmpty()) {
            message.reply("このサーバは絵文字監視対象ではありません。").queue();
            return;
        }
        emojiGuildOpt.get().generateEmojiList(Main.getJDA());
        message.reply("絵文字一覧を再生成しています…").queue();
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
