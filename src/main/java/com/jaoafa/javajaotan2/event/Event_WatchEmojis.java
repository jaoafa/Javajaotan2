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
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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

        generateEmojiList(jda, emojiGuild);
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

        generateEmojiList(jda, emojiGuild);
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

        generateEmojiList(jda, emojiGuild);
    }

    private void generateEmojiList(JDA jda, WatchEmojis.EmojiGuild emojiGuild) {
        long list_channel_id = emojiGuild.getListChannelId();
        List<Long> list_message_ids = emojiGuild.getListMessageIds();
        TextChannel channel = jda.getTextChannelById(list_channel_id);
        Guild guild = jda.getGuildById(emojiGuild.getGuildId());
        if (guild == null || channel == null) {
            return;
        }
        List<String> emoji_list = getEmojiList(guild);
        List<String> split_emojis = split1900chars(emoji_list);

        List<Message> list_messages = list_message_ids.stream().map(s -> getMessage(channel, s)).collect(Collectors.toList());
        if (list_messages.stream().anyMatch(Objects::isNull)) {
            list_messages.forEach(m -> m.delete().queue()); // 一つでもメッセージが存在しなかったら、すべてのメッセージを削除する
            list_messages.clear();
            emojiGuild.clearListMessageIds();
        }

        for (int i = 0; i < split_emojis.size(); i++) {
            String emoji_list_str = split_emojis.get(i);
            if (i >= list_messages.size()) {
                // メッセージ作成
                channel.sendMessage(emoji_list_str).queue(emojiGuild::addListMessage, Throwable::printStackTrace);
            } else {
                // 既存メッセージ利用
                Message message = list_messages.get(i);
                message.editMessage(emoji_list_str).queue();
            }
        }
    }

    private Message getMessage(TextChannel channel, long message_id) {
        try {
            return channel.retrieveMessageById(message_id).complete();
        } catch (ErrorResponseException e) {
            return null;
        }
    }

    private List<String> getEmojiList(Guild guild) {
        return guild
            .getEmotes()
            .stream()
            .map(e -> String.format("%s = `:%s:`", e.getAsMention(), e.getName()))
            .collect(Collectors.toList());
    }

    private List<String> split1900chars(List<String> strList) {
        List<String> split = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        for (String str : strList) {
            if (builder.length() + str.length() > 1900) {
                // この項目を入れると1900文字を超えてしまう
                split.add(builder.toString().trim());
                builder = new StringBuilder();
            }
            builder.append(str);
            builder.append("\n");
        }
        if(builder.length() != 0){
            split.add(builder.toString().trim());
        }
        return split;
    }
}
