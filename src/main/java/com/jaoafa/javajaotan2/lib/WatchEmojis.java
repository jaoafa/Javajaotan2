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

package com.jaoafa.javajaotan2.lib;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
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

public class WatchEmojis {
    File file;
    List<EmojiGuild> guilds = new ArrayList<>();

    public WatchEmojis() {
        file = new File("watch-emojis.json");
        load();
    }

    public void addGuild(@NotNull Guild guild, @NotNull MessageChannel log_channel, @NotNull MessageChannel list_channel) {
        guilds.add(new EmojiGuild(guild.getIdLong(), log_channel.getIdLong(), list_channel.getIdLong(), new ArrayList<>()));
        save();
    }

    public void removeGuild(@NotNull Guild guild) {
        guilds = guilds.stream().filter(g -> g.guild_id != guild.getIdLong()).collect(Collectors.toList());
        save();
    }

    public Optional<EmojiGuild> getEmojiGuild(@NotNull Guild guild) {
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
                List<Long> list_message_ids = obj.has("list_message_ids") ?
                    obj.getJSONArray("list_message_ids").toList().stream().map(o -> (long) o).collect(Collectors.toList()) :
                    new ArrayList<>();
                guilds.add(new EmojiGuild(obj.getLong("guild_id"), obj.getLong("log_channel_id"), obj.getLong("list_channel_id"), list_message_ids));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        JSONArray array = new JSONArray();
        guilds.stream().map(EmojiGuild::asJSONObject).forEach(array::put);
        try {
            Files.writeString(file.toPath(), array.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class EmojiGuild {
        long guild_id;
        long log_channel_id;
        long list_channel_id;
        List<Long> list_message_ids;

        public EmojiGuild(long guild_id, long log_channel_id, long list_channel_id, List<Long> list_message_ids) {
            this.guild_id = guild_id;
            this.log_channel_id = log_channel_id;
            this.list_channel_id = list_channel_id;
            this.list_message_ids = list_message_ids;
        }

        public long getGuildId() {
            return guild_id;
        }

        public long getLogChannelId() {
            return log_channel_id;
        }

        public long getListChannelId() {
            return list_channel_id;
        }

        public List<Long> getListMessageIds() {
            return list_message_ids;
        }

        public void clearListMessageIds(){
            list_message_ids.clear();
        }

        public void addListMessage(Message message){
            list_message_ids.add(message.getIdLong());
        }

        public JSONObject asJSONObject() {
            return new JSONObject()
                .put("guild_id", guild_id)
                .put("log_channel_id", log_channel_id)
                .put("list_channel_id", list_channel_id);
        }
    }
}
