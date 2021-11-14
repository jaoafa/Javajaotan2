/*
 * jaoLicense
 *
 * Copyright (c) 2021 jao Minecraft Server
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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Cmd_Translate implements CommandPremise {
    @Override
    public JavajaotanCommand.Detail details() {
        return new JavajaotanCommand.Detail(
            "translate",
            List.of("to"),
            "Google翻訳を用いて翻訳を行います。"
        );
    }

    @Override
    public JavajaotanCommand.Cmd register(Command.Builder<JDACommandSender> builder) {
        return new JavajaotanCommand.Cmd(
            builder
                .meta(CommandMeta.DESCRIPTION, "Google翻訳を用いて翻訳を行います。")
                .argument(StringArgument.of("from"))
                .argument(StringArgument.of("to"))
                .argument(StringArgument.greedy("text"))
                .handler(context -> execute(context, this::translate))
                .build()
        );
    }

    private void translate(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        String from_raw = context.get("from");
        String to_raw = context.get("to");
        String text = context.get("text");
        String displayText = JavajaotanLibrary.getContentDisplay(message, text);

        Translate.Language from = Translate.getLanguage(from_raw);
        Translate.Language to = Translate.getLanguage(to_raw);

        if (to == Translate.Language.UNKNOWN) {
            message.reply("翻訳先の言語が不明です。").queue();
            return;
        }

        Translate.TranslateResult result = Translate.translate(from, to, displayText);
        if (result == null) {
            message.reply("翻訳に失敗しました。").queue();
            return;
        }

        message.reply("```%s```\n`%s` -> `%s`".formatted(
            result.result(),
            result.from().toString(),
            result.to().toString()
        )).queue();
    }
}
