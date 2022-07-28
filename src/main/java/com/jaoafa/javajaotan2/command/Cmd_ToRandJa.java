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

import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.jda.JDACommandSender;
import cloud.commandframework.meta.CommandMeta;
import com.jaoafa.javajaotan2.lib.CommandPremise;
import com.jaoafa.javajaotan2.lib.JavajaotanCommand;
import com.jaoafa.javajaotan2.lib.JavajaotanLibrary;
import com.jaoafa.javajaotan2.lib.Translate;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Cmd_ToRandJa implements CommandPremise {
    @Override
    public JavajaotanCommand.Detail details() {
        return new JavajaotanCommand.Detail(
            "torandja",
            List.of("torandomja"),
            "Google翻訳を用いてランダムな言語へ翻訳をしたあと、日本語へ翻訳を行います。"
        );
    }

    @Override
    public JavajaotanCommand.Cmd register(Command.Builder<JDACommandSender> builder) {
        return new JavajaotanCommand.Cmd(
            builder
                .meta(CommandMeta.DESCRIPTION, "Google翻訳を用いてランダムな言語へ翻訳をしたあと、日本語へ翻訳を行います。")
                .argument(StringArgument.greedy("text"))
                .handler(context -> execute(context, this::translateRandJa))
                .build()
        );
    }

    private void translateRandJa(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        String text = context.get("text");
        String displayText = JavajaotanLibrary.getContentDisplay(message, text);

        List<Translate.Language> ignoreAutoUnknown = Arrays.stream(Translate.Language.values())
            .filter(l -> l != Translate.Language.AUTO)
            .filter(l -> l != Translate.Language.UNKNOWN)
            .toList();
        Translate.Language lang1 = ignoreAutoUnknown.get(new Random().nextInt(ignoreAutoUnknown.size()));
        Translate.Language lang2 = Translate.Language.JA;

        Translate.TranslateResult result1 = Translate.translate(
            Translate.Language.UNKNOWN,
            lang1,
            displayText
        );
        if (result1 == null) {
            message.reply("翻訳に失敗しました。").queue();
            return;
        }

        Translate.TranslateResult result2 = Translate.translate(
            lang1,
            lang2,
            result1.result()
        );
        if (result2 == null) {
            message.reply("翻訳に失敗しました。").queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("翻訳が成功しました:clap:")
            .addField("`%s` -> `%s`".formatted(result1.from().toString(), lang1.toString()),
                "```%s```".formatted(result1.result()),
                true)
            .addField("`%s` -> `%s`".formatted(lang1.toString(), lang2.toString()),
                "```%s```".formatted(result2.result()),
                true)
            .setColor(Color.PINK)
            .setTimestamp(Instant.now());
        message.replyEmbeds(embed.build()).queue();
    }
}
