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
import net.dv8tion.jda.api.EmbedBuilder;
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

        PrivateChannel dm = user.openPrivateChannel().complete();
        if (dm == null || !dm.canTalk()) {
            channel
                .sendMessage(user.getAsMention() + " ダイレクトメッセージの送信に失敗しました。")
                .delay(1, TimeUnit.MINUTES, Main.getScheduler()) // delete 1 minute later
                .flatMap(Message::delete)
                .queue();
            return;
        }

        if (role == null) {
            dm.sendMessageEmbeds(new EmbedBuilder()
                .setTitle("ゲームロール付与失敗")
                .setDescription("指定された絵文字のロールが見つからなかったため、ゲームロール付与に失敗しました。")
                .setColor(0xFF0000)
                .build()).queue();
            return;
        }
        Member member = guild.retrieveMember(user).complete();
        if (member == null) {
            dm.sendMessageEmbeds(new EmbedBuilder()
                .setTitle("ゲームロール付与失敗")
                .setDescription("メンバー情報の取得に失敗したため、ゲームロール付与に失敗しました。")
                .setColor(0xFF0000)
                .build()).queue();
            return;
        }
        if (member.getRoles().contains(role)) {
            dm.sendMessageEmbeds(new EmbedBuilder()
                .setTitle("ゲームロール付与失敗")
                .setDescription("既にあなたにはロール「" + role.getName() + "」が付与されているため、ゲームロール付与に失敗しました。")
                .setColor(0xFFFF00)
                .build()).queue();
            return;
        }
        guild.addRoleToMember(member, role).queue();
        dm.sendMessageEmbeds(new EmbedBuilder()
            .setTitle("ゲームロール付与成功")
            .setDescription("ロール「" + role.getName() + "」が付与されました。")
            .setColor(0x008000)
            .build()).queue();
    }
}
