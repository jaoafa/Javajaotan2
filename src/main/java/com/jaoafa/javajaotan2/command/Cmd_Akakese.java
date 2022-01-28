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
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.jda.JDACommandSender;
import cloud.commandframework.jda.parsers.UserArgument;
import cloud.commandframework.meta.CommandMeta;
import com.jaoafa.javajaotan2.Main;
import com.jaoafa.javajaotan2.lib.CommandPremise;
import com.jaoafa.javajaotan2.lib.JavajaotanCommand;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Cmd_Akakese implements CommandPremise {
    @Override
    public JavajaotanCommand.Detail details() {
        return new JavajaotanCommand.Detail(
            "akakese",
            "垢消せ。"
        );
    }

    @Override
    public JavajaotanCommand.Cmd register(Command.Builder<JDACommandSender> builder) {
        return new JavajaotanCommand.Cmd(
            builder
                .meta(CommandMeta.DESCRIPTION, "垢消せ。")
                .argument(UserArgument.<JDACommandSender>newBuilder("user")
                    .withParsers(Arrays.stream(UserArgument.ParserMode.values()).collect(Collectors.toSet()))
                    .withIsolationLevel(UserArgument.Isolation.GUILD)
                    .asOptionalWithDefault("222018383556771840")
                    .build())
                .handler(context -> execute(context, this::akakese))
                .build()
        );
    }

    private void akakese(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        User user = context.get("user");

        MessageAction action = channel.sendMessage(user.getAsMention() + ", なンだおまえ!!!!帰れこのやろう!!!!!!!!人間の分際で!!!!!!!!寄るな触るな近づくな!!!!!!!!垢消せ!!!!垢消せ!!!!!!!! ┗(‘o’≡’o’)┛!!!!!!!!!!!!!!!! https://twitter.com/settings/accounts/confirm_deactivation");
        InputStream akakeseStream = Main.class.getResourceAsStream("/images/akakese1_slow.gif");
        if (akakeseStream != null) {
            action = action.addFile(akakeseStream, "akakese.gif");
        }
        action.queue();
    }
}
