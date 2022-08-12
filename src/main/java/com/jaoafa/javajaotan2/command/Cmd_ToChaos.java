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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Cmd_ToChaos extends Command {
    public Cmd_ToChaos() {
        this.name = "tochaos";
        this.help = "Google翻訳を用いてランダムな言語へ複数回翻訳をしたあと、日本語へ翻訳を行います。";
        this.arguments = "<Text...>";
        this.aliases = new String[]{"contorandja"};
    }

    @Override
    protected void execute(CommandEvent event) {
        List<Translate.Language> excluded = Arrays.stream(Translate.Language.values())
            .filter(l -> l != Translate.Language.AUTO)
            .filter(l -> l != Translate.Language.UNKNOWN)
            .filter(l -> l != Translate.Language.JA)
            .toList();

        List<Translate.Language> translateTo = new ArrayList<>();
        for (int i = 0; i < new Random().nextInt(2) + 3; i++) {
            Translate.Language selected = excluded.get(new Random().nextInt(excluded.size()));
            if (translateTo.contains(selected)) {
                continue;
            }
            translateTo.add(selected);
        }
        translateTo.add(Translate.Language.JA);

        Translate.executeTranslate(event, translateTo.toArray(new Translate.Language[0]));
    }
}
