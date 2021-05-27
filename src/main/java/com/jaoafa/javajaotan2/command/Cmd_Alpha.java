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
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.jda.JDACommandSender;
import cloud.commandframework.meta.CommandMeta;
import com.jaoafa.javajaotan2.lib.CommandPremise;
import com.jaoafa.javajaotan2.lib.JavajaotanCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Cmd_Alpha implements CommandPremise {
    @Override
    public JavajaotanCommand.Detail details() {
        return new JavajaotanCommand.Detail(
            "alpha",
            "アルファになったオレをします。"
        );
    }

    @Override
    public JavajaotanCommand.Cmd register(Command.Builder<JDACommandSender> builder) {
        return new JavajaotanCommand.Cmd(
            builder
                .meta(CommandMeta.DESCRIPTION, "アルファになったオレをします。")
                .argument(StringArrayArgument.optional("OreArray", (c, l) -> new ArrayList<>()))
                .handler(context -> execute(context, this::oreAlpha))
                .build()
        );
    }

    private void oreAlpha(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        //ユーザーアルファ
        String[] oreArray = context.getOrDefault("OreArray", null);
        boolean isRandom = false;
        if (oreArray != null) {
            List<String> oreRandomOperationArray = new ArrayList<>(Arrays.asList(oreArray));
            //randomがあったら削除&booleanに記録
            isRandom = oreRandomOperationArray.remove("random");
            oreArray = oreRandomOperationArray.toArray(new String[0]);
        }

        //ユーザーアルファに不足分のデフォルトアルファを補足
        String[] oreMergedArray = getMargedArray(oreArray, new String[]{"アルファ", "ふぁぼら", "エゴサ", "人気", "クソアルファ", "エビフィレオ"});
        message.reply(getOreAlpha(oreMergedArray, isRandom)).queue();
    }

    private String getOreAlpha(String[] oreArray, boolean isRandom) {
        //randomだったらシャッフル
        if (isRandom) Collections.shuffle(Arrays.asList(oreArray));
        return String.format(
            "オ、オオwwwwwwwwオレ%swwwwwwww最近めっちょ%sれてんねんオレwwwwwwww%sとかかけるとめっちょ%sやねんwwwwァァァァァァァwww%sを見下しながら食べる%sは一段とウメェなァァァァwwwwwwww",
            oreArray[0], oreArray[1], oreArray[2], oreArray[3], oreArray[4], oreArray[5]
        );
    }

    private String[] getMargedArray(String[] oreArray, String[] oreDefaultArray) {
        if (oreArray == null) return oreDefaultArray;
        int replaceCount = 0;
        for (String oreStr : oreArray) {
            if (replaceCount > 5) break;
            oreDefaultArray[replaceCount] = oreStr;
            replaceCount++;
        }
        return oreDefaultArray;
    }
}
