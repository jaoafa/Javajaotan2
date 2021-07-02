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

package com.jaoafa.javajaotan2.lib;

import cloud.commandframework.Command;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.jda.JDACommandSender;
import com.jaoafa.javajaotan2.Main;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

public interface CommandPremise {
    /**
     * コマンドに関する情報を指定・返却します。
     *
     * @return コマンドの使い方
     */
    JavajaotanCommand.Detail details();

    /**
     * コマンドを登録します。
     */
    JavajaotanCommand.Cmd register(Command.Builder<JDACommandSender> builder);

    /**
     * Javajaotan用のコマンド実行事前処理を実施します<br />
     * <code>.handler(context -> execute(context, this::getUserId))</code> のように指定してください
     *
     * @param context CommandContext<JDACommandSender>
     * @param handler 事前処理後に実行する関数 (<code>this::関数名</code>)
     */
    default void execute(CommandContext<JDACommandSender> context, CmdFunction handler) {
        MessageChannel channel = context.getSender().getChannel();
        if (context.getSender().getEvent().isEmpty()) {
            channel.sendMessage("メッセージデータを取得できなかったため、処理に失敗しました。").queue();
            return;
        }
        if (!context.getSender().getEvent().get().isFromGuild()) {
            Main.getLogger().warn("execute: Guildからのメッセージではない");
            return;
        }
        Guild guild = context.getSender().getEvent().get().getGuild();
        Member member = guild.getMember(context.getSender().getUser());
        if (member == null) {
            member = guild.retrieveMember(context.getSender().getUser()).complete();
            if (member == null) {
                Main.getLogger().warn("execute: member == null");
                return;
            }
        }
        Message message = context.getSender().getEvent().get().getMessage();
        handler.execute(guild, channel, member, message, context);
    }
}
