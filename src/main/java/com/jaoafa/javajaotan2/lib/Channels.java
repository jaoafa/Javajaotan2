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

package com.jaoafa.javajaotan2.lib;

import com.jaoafa.javajaotan2.Main;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jetbrains.annotations.Nullable;

public enum Channels {
    general(597419057251090443L),
    jaotan(597423444501463040L),
    invite_checker(893166232772161577L),
    c659(621632815599190016L), // 最初を数字にできない…
    meeting_vote(597423974816808970L);

    private long channel_id;

    Channels(long channel_id) {
        this.channel_id = channel_id;
    }

    public long getChannelId() {
        return channel_id;
    }

    public void setChannelId(long channel_id) {
        this.channel_id = channel_id;
    }

    @Nullable
    public TextChannel getChannel() {
        return Main.getJDA().getTextChannelById(channel_id);
    }
}
