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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.awt.*;
import java.time.Instant;

public class Cmd_ToZhJa extends Command {
    Translate.Language[] translateTo;

    public Cmd_ToZhJa() {
        this.name = "tozhja";
        this.translateTo = new Translate.Language[]{
            Translate.Language.ZH,
            Translate.Language.JA
        };
        this.help = "Google翻訳を用いて%sへ翻訳をしたあと、%sへ翻訳を行います。".formatted(
            translateTo[0].getName(),
            translateTo[1].getName()
        );
    }

    @Override
    protected void execute(CommandEvent event) {
        Message message = event.getMessage();
        String text = event.getArgs();
        String displayText = JavajaotanLibrary.getContentDisplay(message, text);

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("翻訳中:hourglass_flowing_sand:")
            .setColor(Color.YELLOW)
            .setTimestamp(Instant.now());
        Message sentMessage = message.replyEmbeds(builder.build()).complete();

        Translate.Language prevLanguage = Translate.Language.UNKNOWN;
        String prevResult = displayText;
        for (Translate.Language language : this.translateTo) {
            Translate.TranslateResult result = Translate.translate(
                prevLanguage,
                language,
                prevResult
            );
            if (result == null) {
                sentMessage.editMessageEmbeds(
                    new EmbedBuilder()
                        .setTitle("翻訳に失敗しました。")
                        .setColor(Color.RED)
                        .setTimestamp(Instant.now())
                        .build()
                ).queue();
                return;
            }

            sentMessage.editMessageEmbeds(
                builder
                    .addField("`%s` -> `%s`".formatted(
                        result.from().toString(),
                        result.to().toString()
                    ), "```%s```".formatted(result.result()), true)
                    .setTimestamp(Instant.now())
                    .build()
            ).queue();
            prevLanguage = language;
            prevResult = result.result();
        }

        sentMessage.editMessageEmbeds(
            builder
                .setTitle("翻訳が完了しました:clap:")
                .setColor(Color.GREEN)
                .build()
        ).queue();
    }
}
