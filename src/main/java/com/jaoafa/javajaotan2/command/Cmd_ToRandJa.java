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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Cmd_ToRandJa extends Command {
    public Cmd_ToRandJa() {
        this.name = "torandja";
        this.help = "Google翻訳を用いてランダムな言語へ翻訳をしたあと、日本語へ翻訳を行います。";
    }

    @Override
    protected void execute(CommandEvent event) {
        Message message = event.getMessage();
        String text = event.getArgs();
        String displayText = JavajaotanLibrary.getContentDisplay(message, text);

        List<Translate.Language> excluded = Arrays.stream(Translate.Language.values())
            .filter(l -> l != Translate.Language.AUTO)
            .filter(l -> l != Translate.Language.UNKNOWN)
            .filter(l -> l != Translate.Language.JA)
            .toList();
        List<Translate.Language> translateTo = List.of(
            excluded.get(new Random().nextInt(excluded.size())),
            Translate.Language.JA
        );


        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("翻訳中:hourglass_flowing_sand:")
            .setColor(Color.YELLOW)
            .setTimestamp(Instant.now());
        Message sentMessage = message.replyEmbeds(builder.build()).complete();

        Translate.Language prevLanguage = Translate.Language.UNKNOWN;
        String prevResult = displayText;
        for (Translate.Language language : translateTo) {
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
                .setTimestamp(Instant.now())
                .build()
        ).queue();
    }
}
