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
import cloud.commandframework.jda.parsers.ChannelArgument;
import cloud.commandframework.meta.CommandMeta;
import com.jaoafa.javajaotan2.Main;
import com.jaoafa.javajaotan2.lib.CommandPremise;
import com.jaoafa.javajaotan2.lib.JavajaotanCommand;
import com.jaoafa.javajaotan2.lib.WatchEmojis;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class Cmd_WatchEmoji implements CommandPremise {
    @Override
    public JavajaotanCommand.Detail details() {
        return new JavajaotanCommand.Detail(
            "watchemoji",
            "絵文字を監視する設定をします。"
        );
    }

    @Override
    public JavajaotanCommand.Cmd register(Command.Builder<JDACommandSender> builder) {
        return new JavajaotanCommand.Cmd(
            builder
                .meta(CommandMeta.DESCRIPTION, "サーバを絵文字監視対象に追加します。")
                .literal("add")
                .argument(ChannelArgument
                    .<JDACommandSender>newBuilder("log_channel")
                    .withParsers(Arrays
                        .stream(ChannelArgument.ParserMode.values())
                        .collect(Collectors.toSet())))
                .argument(ChannelArgument
                    .<JDACommandSender>newBuilder("list_channel")
                    .withParsers(Arrays
                        .stream(ChannelArgument.ParserMode.values())
                        .collect(Collectors.toSet())))
                .handler(context -> execute(context, this::addWatch))
                .build(),
            builder
                .meta(CommandMeta.DESCRIPTION, "サーバを絵文字監視対象から削除します。")
                .literal("remove", "del", "delete")
                .handler(context -> execute(context, this::removeWatch))
                .build(),
            builder
                .meta(CommandMeta.DESCRIPTION, "絵文字一覧を再生成します。")
                .literal("regenerate")
                .handler(context -> execute(context, this::regenerate))
                .build()
        );
    }

    private void addWatch(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
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
        MessageChannel log_channel = context.get("log_channel");
        MessageChannel list_channel = context.get("list_channel");

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

        if (!selfMember.hasPermission(log_guild_channel, Permission.MESSAGE_WRITE)) {
            message.reply("このコマンドを実行するには、jaotanがログチャンネルへの書き込み権限を持っている必要があります。").queue();
            return;
        }

        if (!selfMember.hasPermission(list_guild_channel, Permission.MESSAGE_WRITE)) {
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

    private void removeWatch(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
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

    private void regenerate(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        WatchEmojis watchEmojis = Main.getWatchEmojis();
        Optional<WatchEmojis.EmojiGuild> emojiGuildOpt = watchEmojis.getEmojiGuild(guild);
        if (emojiGuildOpt.isEmpty()) {
            message.reply("このサーバは絵文字監視対象ではありません。").queue();
            return;
        }
        emojiGuildOpt.get().generateEmojiList(Main.getJDA());
        message.reply("絵文字一覧を再生成しています…").queue();
    }
}
