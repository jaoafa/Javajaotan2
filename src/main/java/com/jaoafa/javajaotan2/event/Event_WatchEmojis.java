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
import com.jaoafa.javajaotan2.lib.WatchEmojis;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.emote.EmoteAddedEvent;
import net.dv8tion.jda.api.events.emote.EmoteRemovedEvent;
import net.dv8tion.jda.api.events.emote.update.EmoteUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class Event_WatchEmojis extends ListenerAdapter {
    @Override
    public void onEmoteAdded(@NotNull EmoteAddedEvent event) {
        JDA jda = event.getJDA();
        Guild guild = event.getGuild();
        Emote emote = event.getEmote();
        ListedEmote listedEmote = event.getGuild().retrieveEmoteById(emote.getIdLong()).complete();
        User user = listedEmote.getUser();

        Optional<WatchEmojis.EmojiGuild> optGuild = Main.getWatchEmojis().getEmojiGuild(guild);
        if (optGuild.isEmpty()) {
            return;
        }

        WatchEmojis.EmojiGuild emojiGuild = optGuild.get();
        long log_channel_id = emojiGuild.getLogChannelId();
        TextChannel log_channel = jda.getTextChannelById(log_channel_id);
        if (log_channel == null) {
            return;
        }
        MessageEmbed embed = new EmbedBuilder()
            .setAuthor(user.getAsTag(), "https://discord.com/users/" + user.getId(), user.getAvatarUrl())
            .setTitle(String.format(":new:NEW EMOJI : %s (`:%s:`)", emote.getAsMention(), emote.getName()))
            .setThumbnail(emote.getImageUrl())
            .setTimestamp(Instant.now())
            .build();
        log_channel.sendMessageEmbeds(embed).queue();

        emojiGuild.generateEmojiList(jda);
    }

    @Override
    public void onEmoteUpdateName(@NotNull EmoteUpdateNameEvent event) {
        JDA jda = event.getJDA();
        Guild guild = event.getGuild();
        Emote emote = event.getEmote();

        Optional<WatchEmojis.EmojiGuild> optGuild = Main.getWatchEmojis().getEmojiGuild(guild);
        if (optGuild.isEmpty()) {
            return;
        }

        WatchEmojis.EmojiGuild emojiGuild = optGuild.get();
        long log_channel_id = emojiGuild.getLogChannelId();
        TextChannel log_channel = jda.getTextChannelById(log_channel_id);
        if (log_channel == null) {
            return;
        }

        List<AuditLogEntry> entries = guild.retrieveAuditLogs().type(ActionType.EMOTE_UPDATE).limit(5).complete();
        User user = null;
        if (!entries.isEmpty()) {
            for (AuditLogEntry entry : entries) {
                if (entry.getTargetIdLong() != emote.getIdLong()) {
                    continue;
                }
                user = entry.getUser();
            }
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(String.format(":repeat:CHANGE EMOJI : %s (`:%s:` -> `:%s:`)", emote.getAsMention(), event.getOldName(), event.getNewName()))
            .setThumbnail(emote.getImageUrl())
            .setTimestamp(Instant.now());
        if(user != null) {
            embed.setAuthor(user.getAsTag(), "https://discord.com/users/" + user.getId(), user.getAvatarUrl());
        }
        log_channel.sendMessageEmbeds(embed.build()).queue();

        emojiGuild.generateEmojiList(jda);
    }

    @Override
    public void onEmoteRemoved(@NotNull EmoteRemovedEvent event) {
        JDA jda = event.getJDA();
        Guild guild = event.getGuild();
        Emote emote = event.getEmote();

        Optional<WatchEmojis.EmojiGuild> optGuild = Main.getWatchEmojis().getEmojiGuild(guild);
        if (optGuild.isEmpty()) {
            return;
        }

        WatchEmojis.EmojiGuild emojiGuild = optGuild.get();
        long log_channel_id = emojiGuild.getLogChannelId();
        TextChannel log_channel = jda.getTextChannelById(log_channel_id);
        if (log_channel == null) {
            return;
        }

        List<AuditLogEntry> entries = guild.retrieveAuditLogs().type(ActionType.EMOTE_DELETE).limit(5).complete();
        User user = null;
        if (!entries.isEmpty()) {
            for (AuditLogEntry entry : entries) {
                if (entry.getTargetIdLong() != emote.getIdLong()) {
                    continue;
                }
                user = entry.getUser();
            }
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(String.format(":wave:DELETED EMOJI : (`:%s:`)", emote.getName()))
            .setThumbnail(emote.getImageUrl())
            .setTimestamp(Instant.now());
        if(user != null) {
            embed.setAuthor(user.getAsTag(), "https://discord.com/users/" + user.getId(), user.getAvatarUrl());
        }
        log_channel.sendMessageEmbeds(embed.build()).queue();

        emojiGuild.generateEmojiList(jda);
    }
}
