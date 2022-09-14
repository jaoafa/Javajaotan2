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
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Event_VCSpeechLogLink extends ListenerAdapter {
    final Pattern messageUrlPattern = Pattern.compile("^https://.*?discord(?:app)?\\.com/channels/(\\d+)/(\\d+)/(\\d+)\\??(.*)$", Pattern.CASE_INSENSITIVE);

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (!event.isFromType(ChannelType.TEXT)) {
            return;
        }
        MessageChannelUnion channel = event.getChannel();
        Message message = event.getMessage();
        String content = message.getContentRaw();
        Matcher match = messageUrlPattern.matcher(content);
        if (!match.find()) {
            return;
        }
        String channelId = match.group(2);
        String messageId = match.group(3);

        GuildMessageChannel logChannel = null;
        if (channelId.equals("927666435336056862")) {
            // #vc-speech-log
            logChannel = Main.getJDA().getTextChannelById(927666435336056862L);
            if (logChannel == null) {
                return;
            }
        } else if (channelId.equals("927685488821829653")) {
            // #vc -> スレッド#発言ログ
            logChannel = Main.getJDA().getThreadChannelById(927685488821829653L);
        }
        if (logChannel == null) {
            return;
        }
        Message logMessage = logChannel.retrieveMessageById(messageId).complete();
        if (logMessage == null) {
            return;
        }
        channel.sendMessageEmbeds(new EmbedBuilder().setDescription(logMessage.getContentRaw()).setTimestamp(logMessage.getTimeCreated()).build()).queue();
    }
}
