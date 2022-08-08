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

package com.jaoafa.javajaotan2.command;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jaoafa.javajaotan2.lib.Translate;

public class Cmd_ToSw extends Command {
    final Translate.Language translateTo;

    public Cmd_ToSw() {
        this.name = "tosw";
        this.translateTo = Translate.Language.SW;
        this.help = "Google翻訳を用いて%sへ翻訳を行います。".formatted(this.translateTo.getName());
        this.arguments = "<Text...>";
    }

    @Override
    protected void execute(CommandEvent event) {
        Translate.executeTranslate(event, translateTo);
    }
}
