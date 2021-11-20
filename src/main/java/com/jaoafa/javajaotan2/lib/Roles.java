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
import net.dv8tion.jda.api.entities.Role;

public enum Roles {
    Admin(597405109290532864L),
    Moderator(597405110683041793L),
    Regular(597405176189419554L),
    CommunityRegular(888150763421970492L),
    Verified(597405176969560064L),
    MinecraftConnected(604011598952136853L),
    MailVerified(597421078817669121L),
    NeedSupport(786110419470254102L),
    SubAccount(753047225751568474L),
    FruitPlayers(598135930347323396L),
    SabamisoPlayers(598136046743715846L);

    private long role_id;

    Roles(long role_id) {
        this.role_id = role_id;
    }

    public long getRoleId() {
        return role_id;
    }

    public void setRoleId(long role_id) {
        this.role_id = role_id;
    }

    public Role getRole() {
        return Main.getJDA().getRoleById(role_id);
    }
}
