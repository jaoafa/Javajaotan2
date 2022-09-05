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

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Event_AntiEnEn extends ListenerAdapter {
    List<String> targetEmojis = List.of(
        "1016289682763558972", // :enen:
        "1016289682763558972" // :hin:
    );
    long cryingRoomId = 858282427746877440L;

    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;

        Guild guild = event.getGuild();

        Member member = event.getMember();
        if (member == null) return;

        // メッセージ内に該当絵文字が存在するか
        Mentions mentions = event.getMessage().getMentions(); // getMentionsでカスタム絵文字を取得できる
        List<CustomEmoji> customEmojis = mentions.getCustomEmojis();

        if (customEmojis.stream().noneMatch(e -> targetEmojis.contains(e.getId()))) {
            return;
        }

        // 発言者がVCにいなかったらなにもしない
        if (member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            return;
        }

        VoiceChannel voiceChannel = guild.getVoiceChannelById(cryingRoomId);
        if (voiceChannel == null) return;

        // 泣き部屋に投げ込む
        guild.moveVoiceMember(member, voiceChannel).queue();
    }
}
