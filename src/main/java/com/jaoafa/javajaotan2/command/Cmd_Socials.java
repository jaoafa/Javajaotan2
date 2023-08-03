/*
 * jaoLicense
 *
 * Copyright (c) 2023 jao Minecraft Server
 *
 * The following license applies to this project: jaoLicense
 *
 * Japanese: https://github.com/jaoafa/jao-Minecraft-Server/blob/master/jaoLICENSE.md
 * English: https://github.com/jaoafa/jao-Minecraft-Server/blob/master/jaoLICENSE-en.md
 */

package com.jaoafa.javajaotan2.command;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jaoafa.javajaotan2.Main;
import com.jaoafa.javajaotan2.lib.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONObject;

import java.awt.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public class Cmd_Socials extends CommandWithActions {
    final long SERVER_ID = Main.getConfig().getGuildId();

    public Cmd_Socials() {
        this.name = "socials";
        this.help = "ソーシャルアカウントを登録します。";
        this.actions = List.of(
            new CommandAction("twitter", this::setTwitterAccount, List.of("screenName"), "Twitterアカウントを登録します。"),
            new CommandAction("github", this::setGitHubAccount, List.of("loginId"), "GitHubアカウントを登録します。"),
            new CommandAction("home", this::setHomeUrl, List.of("url"), "Webサイトを登録します。"),
            new CommandAction("status", this::getStatus, "登録されているソーシャルアカウントの情報を表示します。")
        );
        this.arguments = CommandAction.getArguments(this.actions);
    }

    @Override
    protected void execute(CommandEvent event) {
        event.getMessage().reply("当機能は一時的または恒久的に停止しています。").queue();
        // CommandAction.execute(this, event);
    }

    private void setTwitterAccount(CommandEvent event, List<String> argNames) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();
        CommandArgument args = new CommandArgument(event.getArgs(), argNames, CommandAction.getStartArgumentIndex(this, event));

        if (guild.getIdLong() != SERVER_ID) {
            message.reply("このサーバではこのコマンドは使用できません。").queue();
            return;
        }

        UUID uuid = getUUID(member.getIdLong());
        if (uuid == null) {
            message.reply("あなたはまだMinecraftアカウントとの連携がされていません。`/link`を実行して連携してください。").queue();
            return;
        }

        String screenName = args.getString("screenName");
        // https://twitter.com/ から始まる場合は、取り除く
        screenName = screenName.replaceFirst("https://twitter.com/", "");
        // @ から始まる場合は、取り除く
        screenName = screenName.replaceFirst("@", "");
        if (!isTwitterScreenName(screenName)) {
            message.reply("Twitterスクリーンネームの形式が正しくありません。").queue();
            return;
        }

        String twitterId = getTwitterUserId(screenName);
        if (twitterId == null) {
            message.reply("""
                指定されたTwitterアカウントの情報を取得できませんでした。
                ユーザーが存在しないか、処理に失敗している恐れがあります。
                時間をおいてもう一度お試しください。""").queue();
            return;
        }

        try {
            Socials socials = Socials.get(uuid);
            socials.setTwitterId(twitterId);
        } catch (SQLException e) {
            e.printStackTrace();
            message.reply("データベース処理に失敗しました。").queue();
            return;
        }

        message.reply("Twitterアカウントを登録しました。\nhttps://twitter.com/" + screenName).queue();
    }

    private void setGitHubAccount(CommandEvent event, List<String> argNames) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();
        CommandArgument args = new CommandArgument(event.getArgs(), argNames, CommandAction.getStartArgumentIndex(this, event));

        if (guild.getIdLong() != SERVER_ID) {
            message.reply("このサーバではこのコマンドは使用できません。").queue();
            return;
        }

        UUID uuid = getUUID(member.getIdLong());
        if (uuid == null) {
            message.reply("あなたはまだMinecraftアカウントとの連携がされていません。`/link`を実行して連携してください。").queue();
            return;
        }

        String loginId = args.getString("loginId");

        if (!isGitHubLogin(loginId)) {
            message.reply("GitHubログイン名の形式が正しくありません。").queue();
            return;
        }

        String url = "https://api.github.com/users/" + loginId;

        if (!isExistsUrl(url)) {
            message.reply("指定されたGitHubアカウントは存在しません。").queue();
            return;
        }

        try {
            Socials socials = Socials.get(uuid);
            socials.setGitHubId(loginId);
        } catch (SQLException e) {
            e.printStackTrace();
            message.reply("データベース処理に失敗しました。").queue();
            return;
        }

        message.reply("GitHubアカウントを登録しました。\nhttps://github.com/" + loginId).queue();
    }

    private void setHomeUrl(CommandEvent event, List<String> argNames) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();
        CommandArgument args = new CommandArgument(event.getArgs(), argNames, CommandAction.getStartArgumentIndex(this, event));

        if (guild.getIdLong() != SERVER_ID) {
            message.reply("このサーバではこのコマンドは使用できません。").queue();
            return;
        }

        UUID uuid = getUUID(member.getIdLong());
        if (uuid == null) {
            message.reply("あなたはまだMinecraftアカウントとの連携がされていません。`/link`を実行して連携してください。").queue();
            return;
        }

        String url = args.getString("url");

        try {
            Socials socials = Socials.get(uuid);
            socials.setHomeUrl(url);
        } catch (SQLException e) {
            e.printStackTrace();
            message.reply("データベース処理に失敗しました。").queue();
            return;
        }

        message.reply("WebサイトURLを登録しました。\n`" + url + "`").queue();
    }

    private void getStatus(CommandEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();

        if (guild.getIdLong() != SERVER_ID) {
            message.reply("このサーバではこのコマンドは使用できません。").queue();
            return;
        }

        UUID uuid = getUUID(member.getIdLong());
        if (uuid == null) {
            message.reply("あなたはまだMinecraftアカウントとの連携がされていません。`/link`を実行して連携してください。").queue();
            return;
        }

        try {
            Socials socials = Socials.get(uuid);
            String twitterId = socials.getTwitterId();
            String githubId = socials.getGitHubId();
            String homeUrl = socials.getHomeUrl();
            Timestamp updatedAt = socials.getUpdatedAt();

            MessageEmbed embed = new EmbedBuilder()
                .setTitle("Socials Status")
                .addField("Twitter", twitterId == null ? "未設定" : "https://twitter.com/intent/user?user_id=" + twitterId, false)
                .addField("GitHub", githubId == null ? "未設定" : "https://github.com/" + githubId, false)
                .addField("Webサイト", homeUrl == null ? "未設定" : homeUrl, false)
                .setColor(Color.GREEN)
                .setTimestamp(updatedAt.toInstant())
                .build();
            message.replyEmbeds(embed).queue();
        } catch (SQLException e) {
            e.printStackTrace();
            message.reply("データベース処理に失敗しました。").queue();
        }
    }

    private String getTwitterUserId(String screenName) {
        String url = "https://api.twitter.com/1.1/users/show.json?screen_name=" + screenName;
        try {
            OkHttpClient client = new OkHttpClient();
            String bearerToken = Main.getConfig().getTwitterBearerToken();
            Request request = new Request.Builder().url(url).header("Authorization", "Bearer " + bearerToken).build();
            try (Response response = client.newCall(request).execute()) {
                ResponseBody body = response.body();
                JSONObject object = new JSONObject(body.string());
                return object.getString("id_str");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isExistsUrl(String url) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            try (Response response = client.newCall(request).execute()) {
                return response.code() == 200;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTwitterScreenName(String screenName) {
        return screenName.matches("^[0-9a-zA-Z_]{1,15}$");
    }

    private boolean isGitHubLogin(String login) {
        return login.matches("^[0-9a-zA-Z-]{1,39}$");
    }

    private UUID getUUID(long discordId) {
        try {
            DiscordMinecraftLink dml = DiscordMinecraftLink.get(discordId);
            if (dml == null) {
                return null;
            }
            return dml.getMinecraftUUID();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
