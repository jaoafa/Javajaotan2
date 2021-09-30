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
import com.jaoafa.javajaotan2.lib.Channels;
import com.jaoafa.javajaotan2.lib.InviteLink;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class Event_JoinWhichInvite extends ListenerAdapter {
    final Map<String, String> whereInvite = Map.of(
        "zEGrApgGfB", "GitHub公開コード系",
        "7fvwYQDaQp", "Webサイトのヘッダー",
        "qhRFRNBFSc", "Webサイトのフッター",
        "jDY9AwDS9v", "Webサイトトップページ",
        "KeJWma5UBu", "Webサイト内参加方法ブログ記事",
        "6k8FK78zUy", "ユーザーサイトフッター",
        "bKaqrvhPRc", "Japan Minecraft Serversのサーバページ"
    );

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        if (guild.getIdLong() != Main.getConfig().getGuildId()) {
            return;
        }
        User user = event.getUser();

        List<Invite> invites = guild.retrieveInvites().complete();
        InviteLink inviteLink = new InviteLink(event.getGuild());
        EmbedBuilder embed = new EmbedBuilder();
        if (inviteLink.getInvites() != null) {
            InviteLink.GuildInvite matchInvite = inviteLink
                .getInvites()
                .stream()
                .filter(cacheInvite -> {
                    Invite nowInvite = invites
                        .stream()
                        .filter(o -> o.getCode().equals(cacheInvite.invite().getCode()))
                        .findFirst()
                        .orElse(null);
                    return nowInvite != null && cacheInvite.useCount() != nowInvite.getUses();
                })
                .findFirst()
                .orElse(null);
            if (matchInvite != null) {
                Invite invite = matchInvite.invite();
                User inviter = matchInvite.inviter();
                embed
                    .setTitle("Used invite")
                    .setDescription("%sが招待を利用したようです".formatted(user.getAsTag()))
                    .addField("Code", "[`%s`](%s)".formatted(invite.getCode(), invite.getUrl()), true)
                    .addField("Type", invite.getType().name(), true)
                    .addField("Where", getWhereInvite(invite.getCode()), true)
                    .addField("Uses/MaxUses", "%s/%s".formatted(invite.getUses(), invite.getMaxUses()), true)
                    .addField("Channel", "<#%s>".formatted(invite.getChannel() != null ? invite.getChannel().getId() : null), true)
                    .setColor(Color.YELLOW)
                    .setAuthor(user.getAsTag(), "https://discord.com/users/" + user.getId(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now());
                if (inviter != null) {
                    embed.addField("Inviter", inviter.getAsTag(), true);
                }
                embed.addField("CreatedAt", invite.getTimeCreated().atZoneSameInstant(ZoneId.of("Asia/Tokyo")).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSSXXX VV")), false);
            }else{
                embed
                    .setTitle("Used invite (unknown)")
                    .setDescription("%sが参加しましたが、どの招待リンクを利用したか不明です。バニティURLを利用した可能性があります。".formatted(user.getAsTag()))
                    .setAuthor(user.getAsTag(), "https://discord.com/users/" + user.getId(), user.getEffectiveAvatarUrl())
                    .setColor(Color.YELLOW)
                    .setTimestamp(Instant.now());
            }

            TextChannel channel = Channels.invite_checker.getChannel();
            if (channel == null) {
                return;
            }
            channel.sendMessageEmbeds(embed.build()).queue();
        }

        boolean bool = inviteLink.fetchInvites();
        if (!bool) {
            Main.getLogger().warn("inviteLink.fetchInvites failed");
        }
    }

    @Override
    public void onGuildInviteCreate(@Nonnull GuildInviteCreateEvent event) {
        if (event.getGuild().getIdLong() != Main.getConfig().getGuildId()) {
            return;
        }
        InviteLink inviteLink = new InviteLink(event.getGuild());
        Invite invite = event.getInvite().isExpanded() ? event.getInvite() : event.getInvite().expand().complete();
        User inviter = invite.getInviter();

        boolean bool = inviteLink.fetchInvites();
        if (!bool) {
            Main.getLogger().warn("inviteLink.fetchInvites failed");
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("Created invite")
            .addField("Code", "[`%s`](%s)".formatted(invite.getCode(), invite.getUrl()), true)
            .addField("Type", invite.getType().name(), true)
            .addField("Uses/MaxUses", "%s/%s".formatted(invite.getUses(), invite.getMaxUses()), true)
            .addField("Channel", "<#%s>".formatted(invite.getChannel() != null ? invite.getChannel().getId() : null), false)
            .setTimestamp(invite.getTimeCreated())
            .setColor(Color.GREEN);
        if (inviter != null) {
            embed.setAuthor(inviter.getAsTag(), "https://discord.com/users/" + inviter.getId(), inviter.getEffectiveAvatarUrl());
        }

        TextChannel channel = Channels.invite_checker.getChannel();
        if (channel == null) {
            return;
        }
        channel.sendMessageEmbeds(embed.build()).queue();
    }

    @Override
    public void onGuildInviteDelete(@Nonnull GuildInviteDeleteEvent event) {
        if (event.getGuild().getIdLong() != Main.getConfig().getGuildId()) {
            return;
        }
        InviteLink inviteLink = new InviteLink(event.getGuild());
        String code = event.getCode();
        InviteLink.GuildInvite guildInvite = null;
        if (inviteLink.getInvites() != null) {
            guildInvite = inviteLink
                .getInvites()
                .stream()
                .filter(i -> i.invite().getCode().equals(code))
                .findFirst()
                .orElse(null);
        }

        boolean bool = inviteLink.fetchInvites();
        if (!bool) {
            Main.getLogger().warn("inviteLink.fetchInvites failed");
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("Deleted invite")
            .addField("Code", "`%s`".formatted(event.getCode()), true)
            .setColor(Color.RED);
        if (guildInvite != null) {
            Invite invite = guildInvite.invite();
            User inviter = guildInvite.inviter();
            embed
                .addField("Type", invite.getType().name(), true)
                .addField("Uses/MaxUses", "%s/%s".formatted(invite.getUses(), invite.getMaxUses()), true)
                .addField("Channel", "<#%s>".formatted(invite.getChannel() != null ? invite.getChannel().getId() : null), false)
                .setTimestamp(invite.getTimeCreated());
            if (inviter != null) {
                embed.addField("Inviter", inviter.getAsTag(), false);
            }
        }

        TextChannel channel = Channels.invite_checker.getChannel();
        if (channel == null) {
            return;
        }
        channel.sendMessageEmbeds(embed.build()).queue();
    }

    String getWhereInvite(String code){
        return whereInvite.getOrDefault(code, "不明");
    }
}
