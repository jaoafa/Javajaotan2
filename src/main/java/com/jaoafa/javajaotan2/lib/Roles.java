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

public class Roles {
    public static Roles Admin = new Roles(597405109290532864L);
    public static Roles Moderator = new Roles(597405110683041793L);
    public static Roles Regular = new Roles(597405176189419554L);
    public static Roles Verified = new Roles(597405176969560064L);
    public static Roles MinecraftConnected = new Roles(604011598952136853L);
    public static Roles MailVerified = new Roles(597421078817669121L);
    public static Roles NeedSupport = new Roles(786110419470254102L);
    public static Roles SubAccount = new Roles(753047225751568474L);
    public static Roles FruitPlayers = new Roles(598135930347323396L);
    public static Roles SabamisoPlayers = new Roles(598136046743715846L);

    private long role_id;

    private Roles(long role_id) {
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
