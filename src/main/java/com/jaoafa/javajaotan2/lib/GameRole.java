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

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GameRole {
    static Path path = Path.of("gameRoles.json");
    static Path pathMessages = Path.of("gameRoleMessages.json");
    static Pattern emojiPattern = Pattern.compile("<:.+?:(\\d+)>");
    // static long SERVER_ID = 597378876556967936L;
    // static long GAME_ROLE_BORDER_ID = 911556139496374293L;

    static long SERVER_ID = 840825962156654622L;
    static long GAME_ROLE_BORDER_ID = 911629293380075601L;

    public static long getServerId() {
        return SERVER_ID;
    }

    public static long getGameRoleBorderId() {
        return GAME_ROLE_BORDER_ID;
    }

    public static Path getPath() {
        return path;
    }

    public static Path getPathMessages() {
        return pathMessages;
    }

    public static Pattern getEmojiPattern() {
        return emojiPattern;
    }

    public static boolean isGameRole(Role role) {
        JSONArray roles;
        try {
            roles = Files.exists(path) ? new JSONArray(Files.readString(path)) : new JSONArray();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }
        return roles.toList().contains(role.getIdLong());
    }

    public static List<String> getGameRoles() {
        JSONArray roles;
        try {
            roles = Files.exists(path) ? new JSONArray(Files.readString(path)) : new JSONArray();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
        return roles.toList().stream().map(Object::toString).collect(Collectors.toList());
    }

    @Nonnull
    public static List<String> getUsedGameEmojis() {
        JSONObject games;
        try {
            games = Files.exists(pathMessages) ? new JSONObject(Files.readString(pathMessages)) : new JSONObject();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
        if (!games.has("emojis")) {
            games.put("emojis", new JSONObject());
        }
        JSONObject emojis = games.getJSONObject("emojis");
        List<String> ret = new ArrayList<>();
        for (String key : emojis.keySet()) {
            ret.add(emojis.getString(key));
        }
        return ret;
    }

    public static String getGameEmoji(String gameId) {
        JSONObject games;
        try {
            games = Files.exists(pathMessages) ? new JSONObject(Files.readString(pathMessages)) : new JSONObject();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
        if (!games.has("emojis")) {
            games.put("emojis", new JSONObject());
        }
        JSONObject emojis = games.getJSONObject("emojis");
        if (!emojis.has(gameId)) {
            return null;
        }
        return emojis.getString(gameId);
    }

    public static String getGameEmojiFromRoleId(String roleId) {
        JSONObject games;
        try {
            games = Files.exists(pathMessages) ? new JSONObject(Files.readString(pathMessages)) : new JSONObject();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
        if (!games.has("emojis")) {
            games.put("emojis", new JSONObject());
        }
        JSONObject emojis = games.getJSONObject("emojis");
        for (String key : emojis.keySet()) {
            if (emojis.getString(key).equals(roleId)) {
                return key;
            }
        }
        return null;
    }

    public static boolean isGameRoleMessage(Message message) {
        JSONObject games;
        try {
            games = Files.exists(pathMessages) ? new JSONObject(Files.readString(pathMessages)) : new JSONObject();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }
        if (!games.has("messages")) {
            games.put("messages", new JSONArray());
        }
        JSONArray messages = games.getJSONArray("messages");
        for (int i = 0; i < messages.length(); i++) {
            JSONObject object = messages.getJSONObject(i);
            if (!object.getString("channelId").equals(message.getChannel().getId())) {
                continue;
            }
            if (!object.getString("messageId").equals(message.getId())) {
                continue;
            }
            return true;
        }
        return false;
    }
}
