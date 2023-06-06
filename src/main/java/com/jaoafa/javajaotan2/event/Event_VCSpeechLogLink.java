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
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
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
        switch (channelId) {
            case "927666435336056862" -> // #vc-speech-log-google
                logChannel = Main.getJDA().getTextChannelById(927666435336056862L);
            case "1008655448007782410" -> // #vc-speech-log-vosk
                logChannel = Main.getJDA().getTextChannelById(1008655448007782410L);
            case "1114949703700848671" -> // #vc-speech-log-whisper
                logChannel = Main.getJDA().getTextChannelById(1114949703700848671L);
            case "927685488821829653" -> // #vc -> スレッド#発言ログ (Google)
                logChannel = Main.getJDA().getThreadChannelById(927685488821829653L);
            case "1114961269036945479" -> // #vc -> スレッド#発言ログ (Vosk)
                logChannel = Main.getJDA().getThreadChannelById(1114961269036945479L);
            case "1114961384736821389" -> // #vc -> スレッド#発言ログ (Whisper)
                logChannel = Main.getJDA().getThreadChannelById(1114961384736821389L);
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
