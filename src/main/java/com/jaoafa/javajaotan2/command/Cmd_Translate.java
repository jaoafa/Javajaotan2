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
import com.jaoafa.javajaotan2.lib.CommandArgument;
import com.jaoafa.javajaotan2.lib.JavajaotanLibrary;
import com.jaoafa.javajaotan2.lib.Translate;
import net.dv8tion.jda.api.entities.Message;

public class Cmd_Translate extends Command {
    public Cmd_Translate() {
        this.name = "translate";
        this.help = "Google翻訳を用いて翻訳を行います。";
        this.arguments = "<from> <to> <Text...>";
        this.aliases = new String[]{"to"};
    }

    @Override
    protected void execute(CommandEvent event) {
        Message message = event.getMessage();
        CommandArgument args = new CommandArgument(event.getArgs());

        String from_raw = args.getString(0);
        String to_raw = args.getString(1);
        String text = args.getGreedyString(2);
        String displayText = JavajaotanLibrary.getContentDisplay(message, text);

        Translate.Language from = Translate.getLanguage(from_raw);
        Translate.Language to = Translate.getLanguage(to_raw);

        Translate.TranslateResult result = Translate.translate(from, to, displayText);
        if (result == null) {
            message.reply("翻訳に失敗しました。").queue();
            return;
        }
        event.reply("%s (`%s` -> `%s`)".formatted(
            result.result(),
            result.from().toString(),
            result.to().toString()
        ));
    }
}
