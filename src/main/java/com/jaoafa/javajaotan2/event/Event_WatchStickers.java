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
import com.jaoafa.javajaotan2.lib.WatchStickers;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.sticker.GuildSticker;
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake;
import net.dv8tion.jda.api.events.sticker.GuildStickerAddedEvent;
import net.dv8tion.jda.api.events.sticker.GuildStickerRemovedEvent;
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateAvailableEvent;
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateDescriptionEvent;
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateNameEvent;
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateTagsEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class Event_WatchStickers extends ListenerAdapter {
    @Override
    public void onGuildStickerAdded(@NotNull GuildStickerAddedEvent event) {
        JDA jda = event.getJDA();
        Guild guild = event.getGuild();
        GuildSticker sticker = event.getGuild().retrieveSticker(StickerSnowflake.fromId(event.getSticker().getIdLong())).complete();
        User user = sticker.getOwner();
        if (user == null) {
            user = sticker.retrieveOwner().complete();
        }
        if (user == null) {
            Main.getLogger().warn("StickerOwner is null: " + sticker.getId());
            return;
        }

        Optional<WatchStickers.StickerGuild> optGuild = Main.getWatchStickers().getStickerGuild(guild);
        if (optGuild.isEmpty()) {
            return;
        }

        WatchStickers.StickerGuild stickerGuild = optGuild.get();
        long log_channel_id = stickerGuild.getLogChannelId();
        TextChannel log_channel = jda.getTextChannelById(log_channel_id);
        if (log_channel == null) {
            return;
        }
        MessageEmbed embed = new EmbedBuilder()
            .setAuthor(user.getAsTag(), "https://discord.com/users/" + user.getId(), user.getAvatarUrl())
            .setTitle(String.format(":new:NEW STICKER : `%s` - :%s:", sticker.getName(), String.join(": :", sticker.getTags())))
            .setThumbnail(sticker.getIconUrl())
            .setTimestamp(Instant.now())
            .build();
        log_channel.sendMessageEmbeds(embed).queue();
    }

    @Override
    public void onGuildStickerUpdateName(@NotNull GuildStickerUpdateNameEvent event) {
        onStickerUpdate(
            event.getJDA(),
            event.getGuild(),
            event.getSticker(),
            new StickerUpdateRecord(
                StickerUpdateType.NAME,
                event.getOldValue(),
                event.getNewValue()
            )
        );
    }

    @Override
    public void onGuildStickerUpdateDescription(@NotNull GuildStickerUpdateDescriptionEvent event) {
        onStickerUpdate(
            event.getJDA(),
            event.getGuild(),
            event.getSticker(),
            new StickerUpdateRecord(
                StickerUpdateType.DESCRIPTION,
                event.getOldValue(),
                event.getNewValue()
            )
        );
    }

    @Override
    public void onGuildStickerUpdateTags(@NotNull GuildStickerUpdateTagsEvent event) {
        onStickerUpdate(
            event.getJDA(),
            event.getGuild(),
            event.getSticker(),
            new StickerUpdateRecord(
                StickerUpdateType.TAGS,
                String.join(", ", event.getOldValue()),
                String.join(", ", event.getNewValue())
            )
        );
    }

    @Override
    public void onGuildStickerUpdateAvailable(@NotNull GuildStickerUpdateAvailableEvent event) {
        onStickerUpdate(
            event.getJDA(),
            event.getGuild(),
            event.getSticker(),
            new StickerUpdateRecord(
                StickerUpdateType.AVAILABLE,
                event.getOldValue().toString(),
                event.getNewValue().toString()
            )
        );
    }

    void onStickerUpdate(JDA jda, Guild guild, GuildSticker sticker, StickerUpdateRecord record) {
        Optional<WatchStickers.StickerGuild> optGuild = Main.getWatchStickers().getStickerGuild(guild);
        if (optGuild.isEmpty()) {
            return;
        }

        WatchStickers.StickerGuild emojiGuild = optGuild.get();
        long log_channel_id = emojiGuild.getLogChannelId();
        TextChannel log_channel = jda.getTextChannelById(log_channel_id);
        if (log_channel == null) {
            return;
        }

        List<AuditLogEntry> entries = guild.retrieveAuditLogs().type(ActionType.STICKER_UPDATE).limit(5).complete();
        User user = null;
        if (!entries.isEmpty()) {
            for (AuditLogEntry entry : entries) {
                if (entry.getTargetIdLong() != sticker.getIdLong()) {
                    continue;
                }
                user = entry.getUser();
            }
        }

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle(":repeat:UPDATED EMOJI(%s) : %s".formatted(record.type.name(), sticker.getName()))
            .setThumbnail(sticker.getIconUrl())
            .addField("Old", record.oldValue, true)
            .addField("New", record.newValue, true)
            .setTimestamp(Instant.now());

        if (user != null) {
            builder.setAuthor(user.getAsTag(), "https://discord.com/users/" + user.getId(), user.getAvatarUrl());
        }
        log_channel.sendMessageEmbeds(builder.build()).queue();
    }

    @Override
    public void onGuildStickerRemoved(@NotNull GuildStickerRemovedEvent event) {
        JDA jda = event.getJDA();
        Guild guild = event.getGuild();
        GuildSticker sticker = event.getSticker();

        Optional<WatchStickers.StickerGuild> optGuild = Main.getWatchStickers().getStickerGuild(guild);
        if (optGuild.isEmpty()) {
            return;
        }

        WatchStickers.StickerGuild emojiGuild = optGuild.get();
        long log_channel_id = emojiGuild.getLogChannelId();
        TextChannel log_channel = jda.getTextChannelById(log_channel_id);
        if (log_channel == null) {
            return;
        }

        List<AuditLogEntry> entries = guild.retrieveAuditLogs().type(ActionType.STICKER_DELETE).limit(5).complete();
        User user = null;
        if (!entries.isEmpty()) {
            for (AuditLogEntry entry : entries) {
                if (entry.getTargetIdLong() != sticker.getIdLong()) {
                    continue;
                }
                user = entry.getUser();
            }
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(String.format(":wave:DELETED EMOJI : (`%s`)", sticker.getName()))
            .setThumbnail(sticker.getIconUrl())
            .setTimestamp(Instant.now());
        if (user != null) {
            embed.setAuthor(user.getAsTag(), "https://discord.com/users/" + user.getId(), user.getAvatarUrl());
        }
        log_channel.sendMessageEmbeds(embed.build()).queue();
    }

    record StickerUpdateRecord(
        StickerUpdateType type,
        String oldValue,
        String newValue
    ) {
    }

    enum StickerUpdateType {
        NAME,
        DESCRIPTION,
        TAGS,
        AVAILABLE,
    }
}
