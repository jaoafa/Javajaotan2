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
import com.jaoafa.javajaotan2.lib.Channels;
import com.jaoafa.javajaotan2.lib.JavajaotanLibrary;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Event_BugReaction extends ListenerAdapter {
    final String targetReaction = "\uD83D\uDC1B"; // :bug:
    final String repo = "jaoafa/jao-Minecraft-Server";

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (Main.getConfig().getGuildId() != event.getGuild().getIdLong()) return;

        User user = event.getUser();

        if (user == null) user = event.retrieveUser().complete();
        if (user.isBot()) return;

        EmojiUnion emote = event.getEmoji();

        if (emote.getType() != Emoji.Type.UNICODE) return;
        if (!emote.asUnicode().getName().equals(targetReaction)) return;

        Message message = event.retrieveMessage().complete();
        List<User> users = message.retrieveReactionUsers(Emoji.fromUnicode(targetReaction)).complete();

        if (users.size() != 1) {
            // 1人以外 = 0もしくは2人以上 = 既に報告済み
            return;
        }
        MessageChannelUnion channel = event.getChannel();

        // Issueを作成する
        ZonedDateTime createdAt = message.getTimeCreated().atZoneSameInstant(ZoneId.of("Asia/Tokyo"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String title = user.getAsTag() + " による #" + message.getChannel().getName() + " での不具合報告";
        String body = """
            ## 不具合と思われるメッセージ (または報告)
                        
            %s
                        
            URL: %s
                        
            ## 不具合報告者
                        
            %s
            """.formatted(
            createdAt.format(formatter) + " に送信された `" + message.getAuthor().getAsTag() + "` による `#" + message.getChannel().getName() + "` でのメッセージ",
            message.getJumpUrl(),
            "`" + user.getAsTag() + "`"
        );

        JavajaotanLibrary.CreateIssueResponse response = JavajaotanLibrary.createIssue(repo, title, body);
        JavajaotanLibrary.IssueResponseType responseType = response.responseType();
        int issueNumber = response.issueNumber();

        // スレッドを立てる
        TextChannel developmentChannel = Channels.development.getChannel();
        if (developmentChannel == null) {
            // チャンネルが見つからない
            channel
                .sendMessage(user.getAsMention() + " スレッドの作成に失敗しました。developmentチャンネルが見つかりません。")
                .delay(1, TimeUnit.MINUTES, Main.getScheduler()) // delete 1 minute later
                .flatMap(Message::delete)
                .queue();
            return;
        }

        List<String> messages = new ArrayList<>();
        if (responseType == JavajaotanLibrary.IssueResponseType.SUCCESS) {
            messages.add("[LINKED-ISSUE:jaoafa/jao-Minecraft-Server#" + issueNumber + "]");
            messages.add("");
        }
        messages.add("<@&959313488113717298> / " + user.getAsMention() + " / " + message.getAuthor().getAsMention());

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(":bug: リアクションによる不具合の報告")
            .addField("不具合と思われるメッセージ (または報告)", "%s に送信された %s による %s でのメッセージ\n\n%s".formatted(createdAt.format(formatter), message.getAuthor().getAsMention(), message.getChannel().getAsMention(), message.getJumpUrl()), false)
            .addField("不具合報告者", user.getAsMention(), false)
            .setTimestamp(Instant.now())
            .setFooter("投稿者、または報告者は不具合内容についての説明（何が不具合と思ったのか、期待される動作など）をお願いします。")
            .setColor(Color.YELLOW);

        if (responseType == JavajaotanLibrary.IssueResponseType.SUCCESS) {
            embed.addField("Issue Url", "https://github.com/jaoafa/jao-Minecraft-Server/issues/" + issueNumber, false);
        }

        String threadTitle = (responseType == JavajaotanLibrary.IssueResponseType.SUCCESS ? "*" + issueNumber + " " : "") + title;
        ThreadChannel thread = developmentChannel.createThreadChannel(threadTitle).complete();
        thread.sendMessage(new MessageBuilder()
            .setContent(String.join("\n", messages))
            .setEmbeds(embed.build())
            .build()).queue();

        message.addReaction(Emoji.fromUnicode(targetReaction)).queue();
    }
}
