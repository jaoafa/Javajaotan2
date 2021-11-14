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

import net.dv8tion.jda.api.entities.*;

import java.text.SimpleDateFormat;
import java.util.Date;
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
     * Dateをyyyy/MM/dd HH:mm:ss形式でフォーマットします。
     *
     * @param date フォーマットするDate
     *
     * @return フォーマットされた結果文字列
     */
    public static String sdfFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return sdf.format(date);
    }


    /**
     * DateをHH:mm:ss形式でフォーマットします。
     *
     * @param date フォーマットするDate
     *
     * @return フォーマットされた結果文字列
     */
    protected static String sdfTimeFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(date);
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
        for (User user : message.getMentionedUsers()) {
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
        for (Emote emote : message.getEmotes()) {
            tmp = tmp.replace(emote.getAsMention(), ":" + emote.getName() + ":");
        }
        for (TextChannel mentionedChannel : message.getMentionedChannels()) {
            tmp = tmp.replace(mentionedChannel.getAsMention(), '#' + mentionedChannel.getName());
        }
        for (Role mentionedRole : message.getMentionedRoles()) {
            tmp = tmp.replace(mentionedRole.getAsMention(), '@' + mentionedRole.getName());
        }
        return tmp;
    }
}
