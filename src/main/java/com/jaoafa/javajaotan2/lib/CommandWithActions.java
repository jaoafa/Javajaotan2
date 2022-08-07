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

import com.jagrosh.jdautilities.command.Command;

import java.util.List;

public abstract class CommandWithActions extends Command {
    protected List<CommandAction> actions;

    public List<CommandAction> getActions() {
        return actions;
    }
}
