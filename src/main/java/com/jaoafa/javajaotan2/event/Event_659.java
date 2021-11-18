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
import com.jaoafa.javajaotan2.lib.Channels;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Event_659 extends ListenerAdapter {
    Map<String, List<Time>> msgTimes = Map.of(
        "334", List.of(
            new Time(3, 34, 0, 0),
            new Time(15, 34, 0, 0)
        ),
        "ｻﾝｼﾞﾊﾝ!!", List.of(
            new Time(3, 30, 0, 0),
            new Time(15, 30, 0, 0)
        ),
        "659", List.of(
            new Time(6, 59, 59, 999),
            new Time(18, 59, 59, 999)
        ),
        "6時59分", List.of(
            new Time(6, 59, 59, 999),
            new Time(18, 59, 59, 999)
        ),
        "ななじ", List.of(
            new Time(7, 0, 0, 0),
            new Time(19, 0, 0, 0)
        ),
        "ななじすき", List.of(
            new Time(7, 0, 0, 0),
            new Time(19, 0, 0, 0)
        ),
        "ななじすぎ", List.of(
            new Time(7, 1, 0, 0),
            new Time(19, 1, 0, 0)
        )
    );

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (Main.getConfig().getGuildId() != event.getGuild().getIdLong()) {
            return;
        }
        if (Channels.c659.getChannelId() != event.getChannel().getIdLong()) {
            return;
        }
        Member member = event.getMember();
        if (member == null) {
            return;
        }
        if (member.getUser().isBot()) {
            return;
        }
        Message message = event.getMessage();
        String contents = message.getContentDisplay();
        TextChannel channel = event.getTextChannel();
        ZonedDateTime created_at = message.getTimeCreated().atZoneSameInstant(ZoneId.of("Asia/Tokyo"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        Map.Entry<String, List<Time>> match = msgTimes.entrySet().stream()
            .sorted((o1, o2) -> Integer.compare(o2.getKey().length(), o1.getKey().length()))
            .filter(o -> contents.contains(o.getKey())).findFirst()
            .orElse(null);
        Time time = match != null ? match.getValue().stream()
            .min(Comparator.comparingLong(v -> v.getDifference(created_at)))
            .orElse(null) : null;
        String appendMessage = "";
        if (match != null && time != null) {
            long diff_mill = time.getDifference(created_at);
            String formatted = formatMillisecond(diff_mill);
            int rank = getRank(time, diff_mill, member);
            appendMessage = " (`" + time + "`との差: `" + formatted + "` | " + rank + "位)";
        }

        channel.sendMessage(created_at.format(formatter) + appendMessage).reference(message).mentionRepliedUser(false).queue();
    }

    String formatMillisecond(long millisecond) {
        StringBuilder builder = new StringBuilder();

        double year = Math.floor(millisecond / 31536000000.0);
        int year_remain = (int) Math.floor(millisecond % 31536000000L);
        if (year != 0) {
            builder.append(year).append("年");
        }
        int month = (int) Math.floor(year_remain / 2592000000.0);
        int month_remain = (int) Math.floor(year_remain % 2592000000L);
        if (month != 0) {
            builder.append(month).append("か月");
        }
        int day = (int) Math.floor(month_remain / 86400000.0);
        int day_remain = (int) Math.floor(month_remain % 86400000L);
        if (day != 0) {
            builder.append(day).append("日");
        }
        int hour = (int) Math.floor(day_remain / 3600000.0);
        int hour_remain = (int) Math.floor(day_remain % 3600000L);
        if (hour != 0) {
            builder.append(hour).append("時間");
        }
        int minute = (int) Math.floor(hour_remain / 60000.0);
        int minute_remain = (int) Math.floor(hour_remain % 60000L);
        if (minute != 0) {
            builder.append(minute).append("分");
        }
        int sec = (int) Math.floor(minute_remain / 1000.0);
        int mill = (int) Math.floor(minute_remain % 1000L);
        if (sec != 0) {
            builder.append(sec);
        } else {
            builder.append("0");
        }
        if (mill != 0) {
            builder.append(".").append(mill);
        }
        if (sec != 0 && mill != 0) {
            builder.append("秒");
        }

        return builder.toString();
    }

    int getRank(Time time, long diff, Member member) {
        try {
            Path path = Path.of("659ranking.json");
            JSONObject object = new JSONObject();
            if (Files.exists(path)) {
                object = new JSONObject(Files.readString(path));
            }
            if (!object.has(time.toString())) {
                object.put(time.toString(), new JSONObject().put(String.valueOf(diff), new JSONArray().put(member.getIdLong())));
                Files.writeString(path, object.toString());
                return 1;
            }
            JSONObject time_object = object.getJSONObject(time.toString());
            if (time_object.has(String.valueOf(diff))) {
                time_object.put(String.valueOf(diff), time_object.getJSONArray(String.valueOf(diff)).put(member.getIdLong()));
            } else {
                time_object.put(String.valueOf(diff), new JSONArray().put(member.getIdLong()));
            }
            object.put(time.toString(), time_object);
            Files.writeString(path, object.toString());
            return new LinkedList<>(time_object.keySet().stream().sorted(Comparator.comparingLong(Long::parseLong)).toList()).indexOf(String.valueOf(diff)) + 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    record Time(int hour, int minute, int second, int millisecond) {
        public long getDifference(ZonedDateTime dateTime) {
            return Math.abs(ChronoUnit.MILLIS.between(getTodayDateTime(), dateTime));
        }

        public ZonedDateTime getTodayDateTime() {
            ZonedDateTime now = ZonedDateTime.now();
            return ZonedDateTime.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth(), hour, minute, second, millisecond * 1000000, ZoneId.of("Asia/Tokyo"));
        }

        @Override
        public String toString() {
            return hour + ":" + minute + ":" + second + "." + millisecond;
        }
    }
}
