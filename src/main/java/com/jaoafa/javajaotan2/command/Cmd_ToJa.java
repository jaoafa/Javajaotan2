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
import com.jaoafa.javajaotan2.lib.JavajaotanLibrary;
import com.jaoafa.javajaotan2.lib.Translate;
import net.dv8tion.jda.api.entities.Message;

public class Cmd_ToJa extends Command {
    final Translate.Language translateTo;

    public Cmd_ToJa() {
        this.name = "toja";
        this.translateTo = Translate.Language.JA;
        this.help = "Google翻訳を用いて%sへ翻訳を行います。".formatted(this.translateTo.getName());
    }

    @Override
    protected void execute(CommandEvent event) {
        Message message = event.getMessage();
        String text = event.getArgs();
        String displayText = JavajaotanLibrary.getContentDisplay(message, text);
        Translate.TranslateResult result = Translate.translate(
            Translate.Language.UNKNOWN,
            translateTo,
            displayText
        );
        if (result == null) {
            message.reply("翻訳に失敗しました。").queue();
            return;
        }
        event.reply("```%s```\n`%s` -> `%s`".formatted(
            result.result(),
            result.from().toString(),
            result.to().toString()
        ));
    }
}
