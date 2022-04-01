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
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Event_BugReaction extends ListenerAdapter {
    String targetReaction = "\uD83D\uDC1B"; // :bug:
    String repo = "jaoafa/jao-Minecraft-Server";

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (Main.getConfig().getGuildId() != event.getGuild().getIdLong()) return;

        User user = event.getUser();

        if (user == null) user = event.retrieveUser().complete();
        if (user.isBot()) return;

        MessageReaction.ReactionEmote emote = event.getReactionEmote();

        if (!emote.isEmoji()) return;
        if (!emote.getEmoji().equals(targetReaction)) return;

        Message message = event.retrieveMessage().complete();
        List<User> users = message.retrieveReactionUsers(targetReaction).complete();

        if (users.size() != 1) {
            // 1人以外 = 0もしくは2人以上 = 既に報告済み
            return;
        }
        TextChannel channel = event.getTextChannel();

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

        String mainMessage = """
            ## 不具合と思われるメッセージ (または報告)
                        
            %s
                        
            URL: %s
                        
            ## 不具合報告者
                        
            %s
            """.formatted(
            createdAt.format(formatter) + " に送信された " + message.getAuthor().getAsMention() + " による " + message.getChannel().getAsMention() + " でのメッセージ",
            message.getJumpUrl(),
            user.getAsMention()
        );

        List<String> messages = new ArrayList<>();
        if (responseType == JavajaotanLibrary.IssueResponseType.SUCCESS) {
            messages.add("[LINKED-ISSUE:jaoafa/jao-Minecraft-Server#" + issueNumber + "]");
            messages.add("");
        }
        messages.add(":bug: リアクションにより、不具合の報告がなされました。");
        messages.add("投稿者、または報告者は不具合内容についての説明（何が不具合と思ったのか、期待される動作など）をお願いします。");
        messages.add("");
        messages.add(mainMessage);
        if (responseType == JavajaotanLibrary.IssueResponseType.SUCCESS) {
            messages.add("Issue: https://github.com/jaoafa/jao-Minecraft-Server/issues/" + issueNumber);
        }
        messages.add("<@959313488113717298>");

        String threadTitle = (responseType == JavajaotanLibrary.IssueResponseType.SUCCESS ? "*" + issueNumber + " " : "") + title;
        ThreadChannel thread = developmentChannel.createThreadChannel(threadTitle).complete();
        thread.sendMessage(String.join("\n", messages)).queue();

        message.addReaction(targetReaction).queue();
    }
}
