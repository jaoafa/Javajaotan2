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

import com.jaoafa.javajaotan2.Main;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class WatchEmojis {
    final File file;
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

    public record EmojiGuild(long guild_id, long log_channel_id, long list_channel_id,
                             List<Long> list_message_ids) {

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

        public void clearListMessageIds() {
            list_message_ids.clear();
            Main.getWatchEmojis().save();
        }

        public void addListMessage(Message message) {
            list_message_ids.add(message.getIdLong());
            Main.getWatchEmojis().save();
        }

        public void generateEmojiList(JDA jda) {
            long list_channel_id = this.getListChannelId();
            List<Long> list_message_ids = this.getListMessageIds();
            TextChannel channel = jda.getTextChannelById(list_channel_id);
            Guild guild = jda.getGuildById(this.getGuildId());
            if (guild == null || channel == null) {
                return;
            }
            List<String> emoji_list = getEmojiList(guild);
            List<String> split_emojis = split1900chars(emoji_list);

            List<Message> list_messages = list_message_ids.stream().map(s -> getMessage(channel, s)).collect(Collectors.toList());
            if (list_messages.stream().anyMatch(Objects::isNull)) {
                list_messages.forEach(m -> m.delete().queue()); // 一つでもメッセージが存在しなかったら、すべてのメッセージを削除する
                list_messages.clear();
                this.clearListMessageIds();
            }

            for (int i = 0; i < split_emojis.size(); i++) {
                String emoji_list_str = split_emojis.get(i);
                if (i >= list_messages.size()) {
                    // メッセージ作成
                    channel.sendMessage(emoji_list_str).queue(this::addListMessage, Throwable::printStackTrace);
                } else {
                    // 既存メッセージ利用
                    Message message = list_messages.get(i);
                    message.editMessage(emoji_list_str).queue();
                }
            }
        }

        public JSONObject asJSONObject() {
            return new JSONObject()
                .put("guild_id", guild_id)
                .put("log_channel_id", log_channel_id)
                .put("list_channel_id", list_channel_id)
                .put("list_message_ids", list_message_ids);
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
                .sorted(Comparator.comparing(Emote::getName))
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
            if (builder.length() != 0) {
                split.add(builder.toString().trim());
            }
            return split;
        }
    }
}
