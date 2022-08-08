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

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Cmd_ToRandJa extends Command {
    public Cmd_ToRandJa() {
        this.name = "torandja";
        this.help = "Google翻訳を用いてランダムな言語へ翻訳をしたあと、日本語へ翻訳を行います。";
        this.arguments = "<Text...>";
    }

    @Override
    protected void execute(CommandEvent event) {
        List<Translate.Language> excluded = Arrays.stream(Translate.Language.values())
            .filter(l -> l != Translate.Language.AUTO)
            .filter(l -> l != Translate.Language.UNKNOWN)
            .filter(l -> l != Translate.Language.JA)
            .toList();
        Translate.Language[] translateTo = List.of(
            excluded.get(new Random().nextInt(excluded.size())),
            Translate.Language.JA
        ).toArray(new Translate.Language[0]);

        Translate.executeTranslate(event, translateTo);
    }
}
