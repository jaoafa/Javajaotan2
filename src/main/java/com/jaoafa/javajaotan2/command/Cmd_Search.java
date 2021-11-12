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
import com.jaoafa.javajaotan2.Main;
import com.jaoafa.javajaotan2.lib.CommandPremise;
import com.jaoafa.javajaotan2.lib.JavajaotanCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class Cmd_Search implements CommandPremise {
    @Override
    public JavajaotanCommand.Detail details() {
        return new JavajaotanCommand.Detail(
            "search",
            "Google検索を用いて検索を行います。"
        );
    }

    @Override
    public JavajaotanCommand.Cmd register(Command.Builder<JDACommandSender> builder) {
        return new JavajaotanCommand.Cmd(
            builder
                .meta(CommandMeta.DESCRIPTION, "Google検索を用いて検索を行います。")
                .argument(StringArgument.greedy("text"))
                .handler(context -> execute(context, this::search))
                .build()
        );
    }

    private void search(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        String gcpKey = Main.getConfig().getGCPKey();
        String cx = Main.getConfig().getCustomSearchCX();
        if (gcpKey == null || cx == null) {
            message.reply("Google Cloud Platform Key または Custom Search 検索エンジン API が定義されていないため、このコマンドを使用できません。").queue();
            return;
        }
        String text = context.get("text");

    }

    List<SearchResult> customSearch(String gcpKey, String cx, String text) {
        String url = "https://www.googleapis.com/customsearch/v1?key=" + gcpKey + "&cx=" + cx + "&q=" + text;
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();
            if (body == null) {
                return null;
            }
            // body.string()
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    record SearchResult() {

    }
}
