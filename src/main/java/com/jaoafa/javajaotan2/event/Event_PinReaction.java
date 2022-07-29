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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Event_PinReaction extends ListenerAdapter {
    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        Message message = event.retrieveMessage().complete();
        MessageReaction reaction = event.getReaction();
        EmojiUnion emoji = reaction.getEmoji();
        if (emoji.getType() != EmojiUnion.Type.UNICODE) {
            return;
        }
        if (!emoji.getName().equals("📌")) {
            return;
        }
        if (message.isPinned()) {
            return;
        }
        List<User> users = message.retrieveReactionUsers(Emoji.fromUnicode("📌")).complete();
        if (users.size() < 2) {
            return;
        }
        if (users.stream().anyMatch(user -> event.getJDA().getSelfUser().getIdLong() == user.getIdLong())) {
            return;
        }
        Member member = event.getMember();
        if (member == null) {
            return;
        }
        message.pin().queue(
            s -> message.addReaction(Emoji.fromUnicode("📌")).queue(),
            e -> event
                .getChannel()
                .sendMessage("<@" + member.getId() + "> Failed to pin message: " + e.getMessage())
                .delay(1, TimeUnit.MINUTES, Main.getScheduler()) // delete 1 minute later
                .flatMap(Message::delete)
                .queue()
        );
    }
}
