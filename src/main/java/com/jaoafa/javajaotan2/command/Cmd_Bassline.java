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
import com.jaoafa.javajaotan2.Main;
import com.jaoafa.javajaotan2.lib.CommandPremise;
import com.jaoafa.javajaotan2.lib.JavajaotanCommand;
import com.jaoafa.javajaotan2.lib.JavajaotanLibrary;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cmd_Bassline implements CommandPremise {
    @Override
    public JavajaotanCommand.Detail details() {
        return new JavajaotanCommand.Detail(
            "bassline",
            "ベースラインやってる？"
        );
    }

    @Override
    public JavajaotanCommand.Cmd register(Command.Builder<JDACommandSender> builder) {
        return new JavajaotanCommand.Cmd(
            builder
                .meta(CommandMeta.DESCRIPTION, "ベースラインやってる？")
                .argument(StringArgument.optional("target", "<@221498004505362433>"))
                .handler(context -> execute(context, this::bassline))
                .build()
        );
    }

    private void bassline(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        String target_raw = context.get("target");
        String target = parseTarget(target_raw);

        channel.sendMessage(("""
            ベースラインパーティーの途中ですが、ここで臨時ニュースをお伝えします。今日昼頃、わりとキモく女性にナンパをしたうえ、路上で爆睡をしたとして、
            道の上で寝たり、女の子に声をかけたりしたらいけないんだよ罪の容疑で、
            自称優良物件、%s容疑者が逮捕されました。""").formatted(target)).queue();
    }

    public String parseTarget(String target) {
        JDA jda = Main.getJDA();

        // メンション
        Pattern pattern = Pattern.compile("<@!?(\\d+)>");
        Matcher matcher = pattern.matcher(target);
        if (matcher.find()) {
            User user = jda.getUserById(matcher.group(1));
            if (user != null) {
                return user.getAsMention();
            }
        }

        // ユーザーID
        if (JavajaotanLibrary.isLong(target)) {
            User user = jda.getUserById(target);
            if (user != null) {
                return user.getAsMention();
            }
        }

        // その他文字列
        return target;
    }
}
