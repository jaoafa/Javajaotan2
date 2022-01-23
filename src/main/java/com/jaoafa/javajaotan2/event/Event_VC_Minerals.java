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

package com.jaoafa.javajaotan2.event;

import com.jaoafa.javajaotan2.Main;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Event_VC_Minerals extends ListenerAdapter {
    long lastWarn = 0L;

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.getChannel().getIdLong() != 623153228267388958L) {
            return;
        }
        Path path = Path.of("vc-minerals.json");
        if (!Files.exists(path)) {
            return;
        }
        if (System.currentTimeMillis() - lastWarn < 1000 * 60 * 30) {
            return; // 30min
        }
        try {
            JSONObject object = new JSONObject(Files.readString(path));
            List<String> words = object.getJSONArray("words").toList().stream().map(Object::toString).toList();
            List<String> users = object.getJSONArray("users").toList().stream().map(Object::toString).toList();
            if (!users.contains(event.getAuthor().getId())) {
                return;
            }
            boolean isWarn = false;
            for (String word : words) {
                if (event.getMessage().getContentRaw().contains(word)) {
                    isWarn = true;
                    break;
                }
            }
            if (!isWarn) {
                return;
            }
            event
                .getMessage()
                .reply(":warning: <#774232617624797214> <#597790849249574933>")
                .delay(1, TimeUnit.MINUTES, Main.getScheduler()) // delete 1 minute later
                .flatMap(Message::delete)
                .queue();
            lastWarn = System.currentTimeMillis();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
