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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record InviteLink(Guild guild) {
    public static final Map<Long, List<GuildInvite>> invites = new HashMap<>();

    @Nullable
    public List<GuildInvite> getInvites() {
        return invites.getOrDefault(guild.getIdLong(), null);
    }

    public boolean fetchInvites() {
        Main.getLogger().info("Fetching Invite Links...");
        List<GuildInvite> newInvites = new ArrayList<>();
        try {
            guild
                .retrieveInvites()
                .complete()
                .forEach(i -> {
                    Invite inv = i.isExpanded() ? i : i.expand().complete();
                    newInvites.add(new GuildInvite(inv, inv.getInviter(), inv.getUses()));
                });

            invites.put(guild.getIdLong(), newInvites);
            Main.getLogger().info("Fetched Invite Links: %s -> %d count".formatted(guild.getName(), newInvites.size()));
            return true;
        } catch (Exception e) {
            if (JavajaotanData.getRollbar() != null) {
                JavajaotanData.getRollbar().error(e);
            }
            return false;
        }
    }

    public record GuildInvite(Invite invite, User inviter, int useCount) {
        @Override
        public String toString() {
            return "GuildInvite{" +
                "invite=" + invite +
                ", inviter=" + inviter +
                ", useCount=" + useCount +
                '}';
        }
    }
}
