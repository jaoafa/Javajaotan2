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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
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
                .build()
        );
    }

    private void addWatch(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        if (!member.hasPermission(Permission.ADMINISTRATOR)){
            message.reply("このコマンドを実行するには、サーバの管理者権限を持っている必要があります。").queue();
        }
        MessageChannel log_channel = context.get("log_channel");
        MessageChannel list_channel = context.get("list_channel");
        WatchEmojis watchEmojis = Main.getWatchEmojis();
        watchEmojis.addGuild(guild, log_channel, list_channel);

        message.reply(String.format("このサーバ「%s」を絵文字監視対象に追加しました。\nログチャンネル: <#%s>\nリストチャンネル: <#%s>",
            guild.getName(),
            log_channel.getId(),
            list_channel.getId())).queue();
    }

    private void removeWatch(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        if (!member.hasPermission(Permission.ADMINISTRATOR)){
            message.reply("このコマンドを実行するには、サーバの管理者権限を持っている必要があります。").queue();
        }
        WatchEmojis watchEmojis = Main.getWatchEmojis();
        watchEmojis.removeGuild(guild);

        message.reply(String.format("このサーバ「%s」を絵文字監視対象から削除しました。", guild.getName())).queue();
    }
}
