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

import com.jaoafa.javajaotan2.Main;
import net.dv8tion.jda.api.entities.*;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
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

    @Nonnull
    public static CreateIssueResponse createIssue(String repo, String title, String body) {
        String githubToken = Main.getConfig().getGitHubAPIToken();
        if (githubToken == null || githubToken.isEmpty()) {
            return new CreateIssueResponse(
                IssueResponseType.FAILED,
                "GitHub API Token が設定されていません。",
                -1
            );
        }
        String url = String.format("https://api.github.com/repos/%s/issues", repo);
        JSONObject json = new JSONObject()
            .put("title", title)
            .put("body", body)
            .put("labels", new JSONArray()
                .put("\uD83D\uDC1Bbug"));

        try {
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=UTF-8"));
            Request request = new Request.Builder()
                .url(url)
                .header("Authorization", String.format("token %s", githubToken))
                .post(requestBody)
                .build();
            JSONObject obj;
            try (Response response = client.newCall(request).execute()) {
                if (response.code() != 201) {
                    return new CreateIssueResponse(
                        IssueResponseType.FAILED,
                        "Issue の作成に失敗しました。",
                        -1
                    );
                }
                obj = new JSONObject(Objects.requireNonNull(response.body()).string());
            }

            int issueNum = obj.getInt("number");
            return new CreateIssueResponse(
                IssueResponseType.SUCCESS,
                "Issue の作成に成功しました。",
                issueNum
            );
        } catch (IOException e) {
            e.printStackTrace();
            return new CreateIssueResponse(
                IssueResponseType.FAILED,
                "Issue の作成に失敗しました。" + e.getMessage(),
                -1
            );
        }
    }

    public record CreateIssueResponse(IssueResponseType responseType, String message, int issueNumber) {
    }

    @Nonnull
    public static CreateIssueCommentResponse createIssueComment(String repo, int issueNum, String body) {
        String githubToken = Main.getConfig().getGitHubAPIToken();
        if (githubToken == null || githubToken.isEmpty()) {
            return new CreateIssueCommentResponse(
                IssueResponseType.FAILED,
                "GitHub API Token が設定されていません。"
            );
        }
        String url = String.format("https://api.github.com/repos/%s/issues/%s/comments", repo, issueNum);
        JSONObject json = new JSONObject()
            .put("body", body);

        try {
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=UTF-8"));
            Request request = new Request.Builder()
                .url(url)
                .header("Authorization", String.format("token %s", githubToken))
                .post(requestBody)
                .build();
            JSONObject obj;
            try (Response response = client.newCall(request).execute()) {
                if (response.code() != 201) {
                    return new CreateIssueCommentResponse(
                        IssueResponseType.FAILED,
                        "Issue コメントの作成に失敗しました。"
                    );
                }
                obj = new JSONObject(Objects.requireNonNull(response.body()).string());
            }

            return new CreateIssueCommentResponse(
                IssueResponseType.SUCCESS,
                "Issue コメントの作成に成功しました。"
            );
        } catch (IOException e) {
            e.printStackTrace();
            return new CreateIssueCommentResponse(
                IssueResponseType.FAILED,
                "Issue コメントの作成に失敗しました。" + e.getMessage()
            );
        }
    }

    public record CreateIssueCommentResponse(IssueResponseType responseType, String message) {
    }

    public enum IssueResponseType {
        SUCCESS,
        FAILED
    }
}
