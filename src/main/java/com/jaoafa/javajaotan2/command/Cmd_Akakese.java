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
import com.jaoafa.javajaotan2.Main;
import com.jaoafa.javajaotan2.lib.JavajaotanLibrary;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.io.InputStream;

@SuppressWarnings("unused")
public class Cmd_Akakese extends Command {
    public Cmd_Akakese() {
        this.name = "akakese";
        this.help = "垢消せ。";
    }

    @Override
    protected void execute(CommandEvent event) {
        Message message = event.getMessage();
        MessageReference ref = event.getMessage().getMessageReference();
        IMentionable user = getUser(ref != null && ref.getMessage() != null ? ref.getMessage().getAuthor() : event.getAuthor(), event.getArgs());
        if (user == null) {
            message.reply("ユーザー情報を取得できませんでした。").queue();
            return;
        }

        MessageChannel channel = event.getChannel();
        MessageAction action = channel.sendMessage(user.getAsMention() + ", なンだおまえ!!!!帰れこのやろう!!!!!!!!人間の分際で!!!!!!!!寄るな触るな近づくな!!!!!!!!垢消せ!!!!垢消せ!!!!!!!! ┗(‘o’≡’o’)┛!!!!!!!!!!!!!!!! https://twitter.com/settings/accounts/confirm_deactivation");
        InputStream akakeseStream = Main.class.getResourceAsStream("/images/akakese1_slow.gif");
        if (akakeseStream != null) {
            action = action.addFile(akakeseStream, "akakese.gif");
        }
        action.queue();
    }

    /**
     * Userを取得する。
     * <p>
     * - 指定しない場合: 投稿者 or リプライ先
     * - <@XXXXXXXXXX>: メンション
     * - XXXXXXXXXX: ユーザID
     *
     * @param user 投稿者 or リプライ先
     * @param args 引数
     *
     * @return User
     */
    private IMentionable getUser(User user, String args) {
        if (args.isEmpty()) {
            return user;
        }

        if (args.startsWith("<@") && args.endsWith(">")) {
            String id = args.substring(2, args.length() - 1);
            return UserSnowflake.fromId(id);
        }
        if (JavajaotanLibrary.isLong(args)) {
            return UserSnowflake.fromId(args);
        }
        return null;
    }
}
