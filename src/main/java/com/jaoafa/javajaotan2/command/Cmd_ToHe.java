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

public class Cmd_ToHe implements CommandPremise {
    @Override
    public JavajaotanCommand.Detail details() {
        return new JavajaotanCommand.Detail(
            "tosw",
            "Google翻訳を用いてヘブライ語へ翻訳を行います。"
        );
    }

    @Override
    public JavajaotanCommand.Cmd register(Command.Builder<JDACommandSender> builder) {
        return new JavajaotanCommand.Cmd(
            builder
                .meta(CommandMeta.DESCRIPTION, "Google翻訳を用いてヘブライ語へ翻訳を行います。")
                .argument(StringArgument.greedy("text"))
                .handler(context -> execute(context, this::translateHe))
                .build()
        );
    }

    private void translateHe(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        String text = context.get("text");
        String displayText = JavajaotanLibrary.getContentDisplay(message, text);

        Translate.TranslateResult result = Translate.translate(
            Translate.Language.UNKNOWN,
            Translate.Language.HE,
            displayText
        );
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
