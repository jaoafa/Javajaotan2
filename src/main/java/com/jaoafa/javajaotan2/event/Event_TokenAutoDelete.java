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

package com.jaoafa.javajaotan2.event;

import com.jaoafa.javajaotan2.Main;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

/**
 * Discord Token のパターンにマッチするメッセージを消去
 */
public class Event_TokenAutoDelete extends ListenerAdapter {
    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        Message message = event.getMessage();
        String content = message.getContentRaw();

        if (Pattern.compile("[a-zA-Z0-9]{23}\\.[a-zA-Z0-9]{6}\\.[a-zA-Z0-9]{27}").matcher(content).find())
            message.delete().queue(msg -> {
                User user = event.getAuthor();
                Logger logger = Main.getLogger();

                logger.warn("%s (%s) がTokenのパターンにマッチするメッセージを投稿しました！".formatted(user.getAsTag(), user.getId()));
                logger.warn("内容 : " + content);
            });
    }
}
