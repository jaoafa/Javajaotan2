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
import com.jaoafa.javajaotan2.lib.GameRole;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Event_GameRole extends ListenerAdapter {
    long SERVER_ID = GameRole.getServerId();

    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        if (event.getGuild().getIdLong() != SERVER_ID) {
            return;
        }
        User user = event.retrieveUser().complete();
        if (user.isBot()) {
            return;
        }

        TextChannel channel = event.getTextChannel();
        Message message = event.retrieveMessage().complete();
        if (!GameRole.isGameRoleMessage(message)) {
            return;
        }
        MessageReaction reaction = event.getReaction();
        reaction.removeReaction(user).queue();

        List<String> emojis = GameRole.getUsedGameEmojis();
        if (!emojis.contains(reaction.getReactionEmote().getId())) {
            return;
        }

        String gameRoleId = GameRole.getGameEmojiFromRoleId(reaction.getReactionEmote().getId());
        if (gameRoleId == null) {
            return;
        }
        Guild guild = event.getGuild();
        Role role = guild.getRoleById(gameRoleId);
        if (role == null) {
            channel
                .sendMessage(user.getAsMention() + " 指定された絵文字のロールは見つかりません。")
                .delay(1, TimeUnit.MINUTES, Main.getScheduler()) // delete 1 minute later
                .flatMap(Message::delete)
                .queue();
            return;
        }
        Member member = guild.getMember(user);
        if (member == null) {
            channel
                .sendMessage(user.getAsMention() + " メンバー情報の取得に失敗しました。")
                .delay(1, TimeUnit.MINUTES, Main.getScheduler()) // delete 1 minute later
                .flatMap(Message::delete)
                .queue();
            return;
        }
        if (member.getRoles().contains(role)) {
            channel
                .sendMessage(user.getAsMention() + " 既にあなたはロール「" + role.getName() + "」が付与されています。")
                .delay(1, TimeUnit.MINUTES, Main.getScheduler()) // delete 1 minute later
                .flatMap(Message::delete)
                .queue();
            return;
        }
        guild.addRoleToMember(member, role).queue();
        channel
            .sendMessage(user.getAsMention() + " ロール「" + role.getName() + "」が付与されました。")
            .delay(1, TimeUnit.MINUTES, Main.getScheduler()) // delete 1 minute later
            .flatMap(Message::delete)
            .queue();
    }
}
