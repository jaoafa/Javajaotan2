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

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.*;
import net.dv8tion.jda.api.entities.channel.attribute.*;
import net.dv8tion.jda.api.entities.channel.middleman.*;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavajaotanLibrary {
    /**
     * 文字列がInteger値に変換可能かどうか調べます
     *
     * @param str 調べる文字列
     *
     * @return Integer値に変換可能かどうか
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isInt(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 文字列がLong値に変換可能かどうか調べます
     *
     * @param str 調べる文字列
     *
     * @return Long値に変換可能かどうか
     */
    public static boolean isLong(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 指定されたRoleをMemberが持っているかどうかを確認します
     *
     * @param member チェックされるメンバー
     * @param role   チェックするロール
     *
     * @return ロールを持っているか(所属しているか)
     */
    public static boolean isGrantedRole(Member member, Role role) {
        return member
            .getRoles()
            .stream()
            .map(ISnowflake::getIdLong)
            .anyMatch(i -> role.getIdLong() == i);
    }

    public static String getContentDisplay(Message message, String raw) {
        String tmp = raw;
        for (User user : message.getMentions().getUsers()) {
            String name;
            if (message.isFromGuild() && message.getGuild().isMember(user)) {
                Member member = message.getGuild().getMember(user);
                assert member != null;
                name = member.getEffectiveName();
            } else {
                name = user.getName();
            }
            tmp = tmp.replaceAll("<@!?" + Pattern.quote(user.getId()) + '>', '@' + Matcher.quoteReplacement(name));
        }
        for (CustomEmoji emoji : message.getMentions().getCustomEmojis()) {
            tmp = tmp.replace(emoji.getAsMention(), ":" + emoji.getName() + ":");
        }
        for (GuildChannel mentionedChannel : message.getMentions().getChannels()) {
            tmp = tmp.replace(mentionedChannel.getAsMention(), '#' + mentionedChannel.getName());
        }
        for (Role mentionedRole : message.getMentions().getRoles()) {
            tmp = tmp.replace(mentionedRole.getAsMention(), '@' + mentionedRole.getName());
        }
        return tmp;
    }

    public enum IssueResponseType {
        SUCCESS,
        FAILED
    }
}
