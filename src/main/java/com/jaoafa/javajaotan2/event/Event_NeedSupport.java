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

package com.jaoafa.javajaotan2.event;

import com.jaoafa.javajaotan2.Main;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

/**
 * #support にチャットしたユーザーに対して NeedSupport 権限を付与する。ただし MinecraftConnected がついている場合は除外する
 */
public class Event_NeedSupport extends ListenerAdapter {
    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        JDA jda = event.getJDA();
        Guild guild = event.getGuild();
        Message message = event.getMessage();
        if (event.getChannel().getIdLong() != 597423370589700098L) {
            return; // #support 以外
        }

        if (message.getType() != MessageType.DEFAULT) {
            return;
        }

        Member member = event.getMember();
        if (member == null) {
            return;
        }

        Role needSupport = jda.getRoleById(786110419470254102L);
        if (needSupport == null) {
            Main.getLogger().error("ROLE(NeedSupport) IS NOT FOUND.");
            return;
        }
        Role minecraftConnected = jda.getRoleById(604011598952136853L);
        if (minecraftConnected == null) {
            Main.getLogger().error("ROLE(MinecraftConnected) IS NOT FOUND.");
            return;
        }

        boolean isNeedSupport = member.getRoles().stream().anyMatch(_role -> _role.getIdLong() == needSupport.getIdLong());
        boolean isMinecraftConnected = member.getRoles().stream().anyMatch(_role -> _role.getIdLong() == minecraftConnected.getIdLong());
        if (isNeedSupport || isMinecraftConnected) {
            return;
        }
        guild.addRoleToMember(member, needSupport).queue();
        message.addReaction("\uD83D\uDC40").queue(); // :eyes:
    }
}
