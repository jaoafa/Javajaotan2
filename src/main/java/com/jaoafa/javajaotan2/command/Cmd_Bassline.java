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

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jaoafa.javajaotan2.Main;
import com.jaoafa.javajaotan2.lib.CommandArgument;
import com.jaoafa.javajaotan2.lib.JavajaotanLibrary;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cmd_Bassline extends Command {
    public Cmd_Bassline() {
        this.name = "bassline";
        this.help = "ベースラインやってる？ [Suspect] を指定すると Hiratake#2012 にリプライします。";
        this.arguments = "[Suspect]";
    }

    @Override
    protected void execute(CommandEvent event) {
        CommandArgument args = new CommandArgument(event.getArgs());
        String target = parseTarget(args.getOptionalString(0, "<@221498004505362433>"));

        MessageChannel channel = event.getChannel();
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
