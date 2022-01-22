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

package com.jaoafa.javajaotan2.event;

import com.jaoafa.javajaotan2.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Event_VCSpeechLogLink extends ListenerAdapter {
    final Pattern messageUrlPattern = Pattern.compile("^https://.*?discord\\.com/channels/([0-9]+)/([0-9]+)/([0-9]+)\\??(.*)$", Pattern.CASE_INSENSITIVE);

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        TextChannel channel = event.getTextChannel();
        Message message = event.getMessage();
        String content = message.getContentRaw();
        Matcher match = messageUrlPattern.matcher(content);
        if (!match.find()) {
            return;
        }
        String channelId = match.group(2);
        String messageId = match.group(3);

        if (!channelId.equals("927666435336056862")) {
            // vc-speech-logではない
            return;
        }
        TextChannel logChannel = Main.getJDA().getTextChannelById(927666435336056862L);
        if (logChannel == null) {
            return;
        }
        Message logMessage = logChannel.retrieveMessageById(messageId).complete();
        if (logMessage == null) {
            return;
        }
        channel.sendMessageEmbeds(
            new EmbedBuilder()
                .setDescription(logMessage.getContentRaw())
                .setTimestamp(logMessage.getTimeCreated())
                .build()
        ).queue();
    }
}
