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
import com.jaoafa.javajaotan2.lib.WatchEmojis;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.emoji.EmojiAddedEvent;
import net.dv8tion.jda.api.events.emoji.EmojiRemovedEvent;
import net.dv8tion.jda.api.events.emoji.update.EmojiUpdateNameEvent;
import net.dv8tion.jda.api.events.emoji.update.EmojiUpdateRolesEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Event_WatchEmojis extends ListenerAdapter {
    @Override
    public void onEmojiAdded(@NotNull EmojiAddedEvent event) {
        JDA jda = event.getJDA();
        Guild guild = event.getGuild();
        RichCustomEmoji emoji = event.getGuild().retrieveEmoji(event.getEmoji()).complete();
        User user = emoji.getOwner();
        if (user == null) {
            Main.getLogger().warn("EmojiAddedEvent#onEmojiAdded: User is null");
            return;
        }

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
            .setTitle(String.format(":new:NEW EMOJI : %s (`:%s:`)", emoji.getAsMention(), emoji.getName()))
            .setThumbnail(emoji.getImageUrl())
            .setTimestamp(Instant.now())
            .build();
        log_channel.sendMessageEmbeds(embed).queue();

        emojiGuild.generateEmojiList(jda);
    }

    @Override
    public void onEmojiUpdateName(@NotNull EmojiUpdateNameEvent event) {
        onEmojiUpdate(
            event.getJDA(),
            event.getGuild(),
            event.getEmoji(),
            new EmojiUpdateRecord(
                EmojiUpdateType.NAME,
                event.getOldName(),
                event.getNewName()
            )
        );
    }

    @Override
    public void onEmojiUpdateRoles(@NotNull EmojiUpdateRolesEvent event) {
        onEmojiUpdate(
            event.getJDA(),
            event.getGuild(),
            event.getEmoji(),
            new EmojiUpdateRecord(
                EmojiUpdateType.ROLES,
                rolesToString(event.getOldRoles()),
                rolesToString(event.getNewRoles())
            )
        );
    }

    String rolesToString(List<Role> roles) {
        return roles
            .stream()
            .map(Role::getName)
            .collect(Collectors.joining(", "));
    }

    void onEmojiUpdate(JDA jda, Guild guild, RichCustomEmoji emoji, EmojiUpdateRecord record) {
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

        List<AuditLogEntry> entries = guild.retrieveAuditLogs().type(ActionType.EMOJI_UPDATE).limit(5).complete();
        User user = null;
        if (!entries.isEmpty()) {
            for (AuditLogEntry entry : entries) {
                if (entry.getTargetIdLong() != emoji.getIdLong()) {
                    continue;
                }
                user = entry.getUser();
            }
        }

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle(":repeat:UPDATED EMOJI(%s) : %s".formatted(record.type.name(), emoji.getAsMention()))
            .setThumbnail(emoji.getImageUrl())
            .addField("Old", record.oldValue, true)
            .addField("New", record.newValue, true)
            .setTimestamp(Instant.now());

        if (user != null) {
            builder.setAuthor(user.getAsTag(), "https://discord.com/users/" + user.getId(), user.getAvatarUrl());
        }
        log_channel.sendMessageEmbeds(builder.build()).queue();

        emojiGuild.generateEmojiList(jda);
    }

    @Override
    public void onEmojiRemoved(@NotNull EmojiRemovedEvent event) {
        JDA jda = event.getJDA();
        Guild guild = event.getGuild();
        RichCustomEmoji emoji = event.getEmoji();

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

        List<AuditLogEntry> entries = guild.retrieveAuditLogs().type(ActionType.EMOJI_DELETE).limit(5).complete();
        User user = null;
        if (!entries.isEmpty()) {
            for (AuditLogEntry entry : entries) {
                if (entry.getTargetIdLong() != emoji.getIdLong()) {
                    continue;
                }
                user = entry.getUser();
            }
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(String.format(":wave:DELETED EMOJI : (`:%s:`)", emoji.getName()))
            .setThumbnail(emoji.getImageUrl())
            .setTimestamp(Instant.now());
        if (user != null) {
            embed.setAuthor(user.getAsTag(), "https://discord.com/users/" + user.getId(), user.getAvatarUrl());
        }
        log_channel.sendMessageEmbeds(embed.build()).queue();

        emojiGuild.generateEmojiList(jda);
    }

    record EmojiUpdateRecord(
        EmojiUpdateType type,
        String oldValue,
        String newValue
    ) {
    }

    enum EmojiUpdateType {
        NAME,
        ROLES
    }
}
