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
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class Event_Greeting extends ListenerAdapter {
    private static final List<Long> jaoPlayers = new ArrayList<>();

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (Main.getConfig().getGuildId() != event.getGuild().getIdLong()) {
            return;
        }
        if (Main.getJDA().getSelfUser().getIdLong() == event.getAuthor().getIdLong()) {
            return;
        }
        if (Channels.greeting.getChannelId() != event.getChannel().getIdLong()) {
            return;
        }
        if (!event.isFromType(ChannelType.TEXT)) {
            return;
        }
        JDA jda = event.getJDA();
        Message message = event.getMessage();
        Guild guild = event.getGuild();
        MessageChannelUnion channel = event.getChannel();
        String content = message.getContentRaw();
        Member member = event.getMember();
        if (member == null) {
            return;
        }
        if (!content.equals("jao") && !content.equals("afa")) {
            message.delete().queue();
            return;
        }
        Role role = jda.getRoleById(597421078817669121L);
        if (role == null) {
            channel.sendMessage("<@221991565567066112> ROLE IS NOT FOUND").queue();
            return;
        }
        if (content.equals("jao")) {
            List<Role> roles = member.getRoles().stream().filter(_role -> _role.getIdLong() == role.getIdLong()).toList();
            if (roles.size() == 0) {
                message.addReaction(Emoji.fromUnicode("\u2753")).queue(); // ?
                jaoPlayers.add(member.getIdLong());
            } else {
                message.addReaction(Emoji.fromUnicode("\u274C")).queue(); // x
            }
            return;
        }
        if (!jaoPlayers.contains(member.getIdLong())) {
            message.addReaction(Emoji.fromUnicode("\u274C")).queue(); // x
            return;
        }
        guild.addRoleToMember(member, role).queue();
        message.addReaction(Emoji.fromUnicode("\u2B55")).queue(); // o
        message.reply("""
            あいさつしていただきありがとうございます！これにより、多くのチャンネルを閲覧できるようになりました。
            このあとは<#597419057251090443>などで__**「`/link`」を実行(投稿)して、MinecraftアカウントとDiscordアカウントを連携**__しましょう！
            **<#706818240759988224>に記載されているメッセージもお読みください！**""").queue();
        jaoPlayers.remove(member.getIdLong());
    }
}
