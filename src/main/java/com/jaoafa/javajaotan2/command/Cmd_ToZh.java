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

public class Cmd_ToZh extends Command {
    final Translate.Language translateTo;

    public Cmd_ToZh() {
        this.name = "tozh";
        this.translateTo = Translate.Language.ZH;
        this.help = "Google翻訳を用いて%sへ翻訳を行います。".formatted(this.translateTo.getName());
    }

    @Override
    protected void execute(CommandEvent event) {
        Translate.executeTranslate(event, translateTo);
    }
}
