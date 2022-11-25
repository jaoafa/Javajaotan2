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

package com.jaoafa.javajaotan2.lib;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WatchStickers {
    final File file;
    List<StickerGuild> guilds = new ArrayList<>();

    public WatchStickers() {
        file = new File("watch-stickers.json");
        load();
    }

    public void addGuild(@NotNull Guild guild, @NotNull MessageChannel log_channel) {
        guilds.add(new WatchStickers.StickerGuild(guild.getIdLong(), log_channel.getIdLong()));
        save();
    }

    public void removeGuild(@NotNull Guild guild) {
        guilds = guilds.stream().filter(g -> g.guild_id != guild.getIdLong()).collect(Collectors.toList());
        save();
    }

    public Optional<WatchStickers.StickerGuild> getStickerGuild(@NotNull Guild guild) {
        return guilds.stream().filter(g -> g.guild_id == guild.getIdLong()).findFirst();
    }

    public void load() {
        if (!file.exists()) {
            return;
        }
        try {
            guilds.clear();
            String json = Files.readString(file.toPath());
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                guilds.add(new WatchStickers.StickerGuild(obj.getLong("guild_id"), obj.getLong("log_channel_id")));
            }
        } catch (IOException e) {
            e.printStackTrace();

            if (JavajaotanData.getRollbar() != null) {
                JavajaotanData.getRollbar().error(e);
            }
        }
    }

    public void save() {
        JSONArray array = new JSONArray();
        guilds.stream().map(WatchStickers.StickerGuild::asJSONObject).forEach(array::put);
        try {
            Files.writeString(file.toPath(), array.toString());
        } catch (IOException e) {
            e.printStackTrace();

            if (JavajaotanData.getRollbar() != null) {
                JavajaotanData.getRollbar().error(e);
            }
        }
    }

    public record StickerGuild(long guild_id, long log_channel_id) {

        public long getGuildId() {
            return guild_id;
        }

        public long getLogChannelId() {
            return log_channel_id;
        }

        public JSONObject asJSONObject() {
            return new JSONObject()
                .put("guild_id", guild_id)
                .put("log_channel_id", log_channel_id);
        }
    }
}
