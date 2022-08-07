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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class Cmd_GenText extends Command {
    public Cmd_GenText() {
        this.name = "gentext";
        this.help = "文章を自動生成します。";
    }

    @Override
    protected void execute(CommandEvent event) {
        // /gentext: defaultソースで1つの文章を生成
        // /gentext <文章数(int)>: defaultソースで指定文章数の文章を生成
        // /gentext <ソース(String)>: 指定ソースで1つの文章を生成
        // /gentext <ソース> <文章数>: 指定ソースで指定文章数の文章を生成
        Message message = event.getMessage();
        MessageChannel channel = event.getChannel();

        CommandArgument args = new CommandArgument(event.getArgs());
        GenTextParam param = new GenTextParam(args);
        if (param.isError()) {
            message.reply(param.getError()).queue();
            return;
        }
        String source = param.getSource();
        int count = param.getCount();

        message.reply(String.format("生成中です…しばらくお待ちください。(ソース: %s / 生成数: %d)", source, count)).queue(
            msg -> generate(msg, source, count),
            error -> {
                channel.sendMessage("メッセージの生成に失敗しました。再度お試しください。").queue();
                error.printStackTrace();
            }
        );

    }

    static class GenTextParam {
        private String source = "default";
        private int count = 1;
        private String error = null;

        public GenTextParam(CommandArgument args) {
            if (args.size() == 1) {
                try {
                    count = args.getInt(0);
                } catch (NumberFormatException e) {
                    source = args.getString(0);
                }
            } else if (args.size() == 2) {
                source = args.getString(0);
                try {
                    count = args.getInt(1);
                } catch (NumberFormatException e) {
                    error = "文章数が数字ではありません。";
                    return;
                }
            } else if (args.size() != 0) {
                error = "引数が不正です。";
                return;
            }
            if (count < 1) {
                error = "文章数は1以上の整数を指定してください。";
            }
        }

        public String getSource() {
            return source;
        }

        public int getCount() {
            return count;
        }

        public String getError() {
            return error;
        }

        public boolean isError() {
            return error != null;
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
