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
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.jda.JDACommandSender;
import cloud.commandframework.meta.CommandMeta;
import com.jaoafa.javajaotan2.lib.CommandPremise;
import com.jaoafa.javajaotan2.lib.JavajaotanCommand;
import com.jaoafa.javajaotan2.lib.JavajaotanLibrary;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Cmd_GenText implements CommandPremise {
    @Override
    public JavajaotanCommand.Detail details() {
        return new JavajaotanCommand.Detail(
            "gentext",
            "文章を自動生成します"
        );
    }

    @Override
    public JavajaotanCommand.Cmd register(Command.Builder<JDACommandSender> builder) {
        return new JavajaotanCommand.Cmd(
            builder
                .meta(CommandMeta.DESCRIPTION, "デフォルトソースから文章を生成します。")
                .handler(context -> execute(context, this::generateDefault))
                .build(),
            builder
                .meta(CommandMeta.DESCRIPTION, "ソースと生成回数を指定して文章を生成します。")
                .argument(StringArgument.of("sourceOrGenCount"))
                .argument(IntegerArgument
                    .<JDACommandSender>newBuilder("count")
                    .withMin(1)
                    .asOptional())
                .handler(context -> execute(context, this::generateSourceCount))
                .build()
        );
    }

    private void generateDefault(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        message.reply("生成中です…しばらくお待ちください。").queue(
            msg -> generate(msg, "default", 1),
            error -> {
                channel.sendMessage("メッセージの生成に失敗しました。再度お試しください。").queue();
                error.printStackTrace();
            }
        );
    }

    private void generateSourceCount(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        String source = getSource(context);
        int count = getCount(context);
        if (count <= 0) {
            message.reply("生成数は1以上の整数を指定してください。").queue();
            return;
        }

        message.reply(String.format("生成中です…しばらくお待ちください。(ソース: %s / 生成数: %d)", source, count)).queue(
            msg -> generate(msg, source, count),
            error -> {
                channel.sendMessage("メッセージの生成に失敗しました。再度お試しください。").queue();
                error.printStackTrace();
            }
        );
    }

    private String getSource(@NotNull CommandContext<JDACommandSender> context) {
        if (context.contains("count")) {
            return context.get("sourceOrGenCount");
        } else if (!JavajaotanLibrary.isInt(context.get("sourceOrGenCount"))) {
            return context.get("sourceOrGenCount");
        } else {
            return "default";
        }
    }

    private int getCount(@NotNull CommandContext<JDACommandSender> context) {
        if (context.contains("count")) {
            return context.get("count");
        } else if (!JavajaotanLibrary.isInt(context.get("sourceOrGenCount"))) {
            return 1;
        } else {
            return Integer.parseInt(context.get("sourceOrGenCount"));
        }
    }

    private void generate(Message reply, String source, int generateCount) {
        if (!Files.exists(Paths.get("external_scripts", "gentext", "main.py"))) {
            reply.editMessage("Error: メッセージ生成用のスクリプトが見つかりませんでした。").queue();
            return;
        }
        try {
            String output = getRunCommand("python3",
                "external_scripts/gentext/main.py",
                "--source",
                source.toLowerCase(Locale.ROOT),
                "--count",
                String.valueOf(generateCount));
            if (output == null) {
                reply.editMessage("Error: タイムアウト(3分) または プロセス割り込みなどの理由で、正常に実行できませんでした。").queue();
                return;
            }
            reply.editMessage(output).queue();
        } catch (IOException e) {
            e.printStackTrace();
            reply.editMessage(String.format("Error: IOエラーが発生 (%s)", e.getMessage())).queue();
        }
    }

    private String getRunCommand(String... command) throws IOException {
        Process p;
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(command);
            builder.redirectErrorStream(true);
            p = builder.start();
            boolean bool = p.waitFor(3, TimeUnit.MINUTES);
            if (!bool) {
                return null;
            }
        } catch (InterruptedException e) {
            return null;
        }
        try (InputStream is = p.getInputStream()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder text = new StringBuilder();
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    text.append(line).append("\n");
                }
                return text.toString();
            }
        }
    }
}
