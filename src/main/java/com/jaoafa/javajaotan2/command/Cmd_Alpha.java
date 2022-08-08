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
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Cmd_Alpha extends Command {
    public Cmd_Alpha() {
        this.name = "alpha";
        this.help = "アルファになったオレをします。randomを引数のいずれかに指定すると、置換箇所がランダムになります。";
        this.arguments = "[random] [アルファ] [ふぁぼら] [エゴサ] [人気] [クソアルファ] [エビフィレオ]";
    }

    @Override
    protected void execute(CommandEvent event) {
        CommandArgument args = new CommandArgument(event.getArgs());
        boolean isRandom = args
            .getStreamArgs()
            .anyMatch(s -> s.equalsIgnoreCase("random"));
        List<String> input = args
            .getStreamArgs()
            .filter(s -> !s.equalsIgnoreCase("random"))
            .toList();

        int argsSize = input.size();
        String[] array = {
            argsSize > 0 ? input.get(0) : "アルファ",
            argsSize > 1 ? input.get(1) : "ふぁぼら",
            argsSize > 2 ? input.get(2) : "エゴサ",
            argsSize > 3 ? input.get(3) : "人気",
            argsSize > 4 ? input.get(4) : "クソアルファ",
            argsSize > 5 ? input.get(5) : "エビフィレオ",
        };

        if (isRandom) {
            List<String> list = Arrays.asList(array);
            Collections.shuffle(list);
            array = list.toArray(new String[0]);
        }

        MessageChannel channel = event.getChannel();
        channel.sendMessage("オ、オオwwwwwwwwオレ%swwwwwwww最近めっちょ%sれてんねんオレwwwwwwww%sとかかけるとめっちょ%sやねんwwwwァァァァァァァwww%sを見下しながら食べる%sは一段とウメェなァァァァwwwwwwww".formatted(
            array[0],
            array[1],
            array[2],
            array[3],
            array[4],
            array[5]
        )).queue();
    }
}
