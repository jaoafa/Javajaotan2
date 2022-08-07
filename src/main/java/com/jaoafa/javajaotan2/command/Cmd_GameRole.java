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

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jaoafa.javajaotan2.Main;
import com.jaoafa.javajaotan2.lib.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cmd_GameRole extends CommandWithActions {
    final Path path = GameRole.getPath();
    final Path pathMessages = GameRole.getPathMessages();
    final Pattern emojiPattern = GameRole.getEmojiPattern();
    final long SERVER_ID = GameRole.getServerId();
    final long GAME_ROLE_BORDER_ID = GameRole.getGameRoleBorderId();

    public Cmd_GameRole() {
        this.name = "gamerole";
        this.help = "ゲームロール関連コマンド";
        this.actions = List.of(
            new CommandAction("new", this::newGameRole, List.of("name...")),
            new CommandAction("create", this::newGameRole, List.of("name...")),
            new CommandAction("add", this::newGameRole, List.of("name...")),
            new CommandAction("color", this::changeColor, List.of("name", "colorCode...")),
            new CommandAction("rename", this::renameRole, List.of("name", "newName")),
            new CommandAction("give", this::giveGameRole, List.of("name...")),
            new CommandAction("join", this::giveGameRole, List.of("name...")),
            new CommandAction("take", this::takeGameRole, List.of("name...")),
            new CommandAction("leave", this::takeGameRole, List.of("name...")),
            new CommandAction("message", List.of(
                new CommandAction("post", this::createGameRoleMessage),
                new CommandAction("update", this::updateGameRoleMessage),
                new CommandAction("set-emoji", this::changeGameRoleEmoji, List.of("role", "emojiId"))
            ))
        );
    }

    @Override
    protected void execute(CommandEvent event) {
        CommandAction.execute(this, event);
    }

    private void newGameRole(CommandEvent event, List<String> argNames) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();
        CommandArgument args = new CommandArgument(event.getArgs(), argNames);

        if (guild.getIdLong() != SERVER_ID) {
            message.reply("このサーバではこのコマンドは使用できません。").queue();
            return;
        }
        Role gameRole = guild.getRoleById(GAME_ROLE_BORDER_ID);
        if (gameRole == null) {
            message.reply("基準ゲームロールが見つからないため、このコマンドを実行できません。運営にお問い合わせください。").queue();
        }
        if (!JavajaotanLibrary.isGrantedRole(member, Roles.Admin.getRole()) &&
            !JavajaotanLibrary.isGrantedRole(member, Roles.Moderator.getRole()) &&
            !JavajaotanLibrary.isGrantedRole(member, Roles.Regular.getRole()) &&
            !JavajaotanLibrary.isGrantedRole(member, Roles.CommunityRegular.getRole())) {
            message.reply("ゲームロールの作成は CommunityRegular から作成できます。").queue();
            return;
        }
        String roleName = args.getString("name...");
        List<Role> matchRoles = guild.getRolesByName(roleName, false);
        if (!matchRoles.isEmpty()) {
            message.reply("同一名のロールがあるため、作成できません。").queue();
            return;
        }
        addGameRole(message, roleName);
        updateMessages();
    }

    private void changeColor(CommandEvent event, List<String> argNames) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();
        CommandArgument args = new CommandArgument(event.getArgs());
        if (guild.getIdLong() != SERVER_ID) {
            message.reply("このサーバではこのコマンドは使用できません。").queue();
            return;
        }
        if (!JavajaotanLibrary.isGrantedRole(member, Roles.Admin.getRole()) &&
            !JavajaotanLibrary.isGrantedRole(member, Roles.Moderator.getRole()) &&
            !JavajaotanLibrary.isGrantedRole(member, Roles.Regular.getRole()) &&
            !JavajaotanLibrary.isGrantedRole(member, Roles.CommunityRegular.getRole())) {
            message.reply("ゲームロールの色変更は CommunityRegular から作成できます。").queue();
            return;
        }
        Color color;
        try {
            color = Color.decode(args.getString("colorCode..."));
        } catch (NumberFormatException e) {
            message.reply("色コードが正しくありません。").queue();
            return;
        }
        String roleName = args.getString("name");
        List<Role> matchRoles = guild.getRolesByName(roleName, false);
        if (matchRoles.isEmpty()) {
            message.reply("マッチするロールが見つかりませんでした。").queue();
            return;
        }
        Role role = matchRoles.get(0);
        role.getManager().setColor(color).queue(
            tmp -> message.reply(
                "ロール `%s` のロールカラーを変更しました。".formatted(
                    role.getName()
                )
            ).queue(),
            e -> message.reply(
                "ロールカラーの変更に失敗しました: `%s`".formatted(e.getMessage())
            ).queue()
        );
    }

    private void renameRole(CommandEvent event, List<String> argNames) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();
        CommandArgument args = new CommandArgument(event.getArgs());
        if (guild.getIdLong() != SERVER_ID) {
            message.reply("このサーバではこのコマンドは使用できません。").queue();
            return;
        }
        if (!JavajaotanLibrary.isGrantedRole(member, Roles.Admin.getRole()) &&
            !JavajaotanLibrary.isGrantedRole(member, Roles.Moderator.getRole()) &&
            !JavajaotanLibrary.isGrantedRole(member, Roles.Regular.getRole()) &&
            !JavajaotanLibrary.isGrantedRole(member, Roles.CommunityRegular.getRole())) {
            message.reply("ゲームロールの名前変更は CommunityRegular から作成できます。").queue();
            return;
        }
        String roleName = args.getString("name");
        List<Role> matchRoles = guild.getRolesByName(roleName, false);
        if (matchRoles.isEmpty()) {
            message.reply("マッチするロールが見つかりませんでした。").queue();
            return;
        }
        String newRoleName = args.getString("newName");
        Role role = matchRoles.get(0);
        role.getManager().setName(newRoleName).queue(
            tmp -> message.reply(
                "ロール `%s` のロール名を `%s` に変更しました。".formatted(
                    role.getName(),
                    newRoleName
                )
            ).queue(),
            e -> message.reply(
                "ロール名の変更に失敗しました: `%s`".formatted(e.getMessage())
            ).queue()
        );
        updateMessages();
    }

    private void giveGameRole(CommandEvent event, List<String> argNames) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();
        CommandArgument args = new CommandArgument(event.getArgs());
        if (guild.getIdLong() != SERVER_ID) {
            message.reply("このサーバではこのコマンドは使用できません。").queue();
            return;
        }
        if (!JavajaotanLibrary.isGrantedRole(member, Roles.MinecraftConnected.getRole())) {
            message.reply("ゲームロールの付与は MinecraftConnected から利用可能です。").queue();
            return;
        }
        String roleName = args.getString("name");
        List<Role> matchRoles = guild.getRolesByName(roleName, false);
        if (matchRoles.isEmpty()) {
            message.reply("マッチするロールが見つかりませんでした。").queue();
            return;
        }
        Role role = matchRoles.get(0);
        if (!GameRole.isGameRole(role)) {
            message.reply("指定されたロール `%s` はゲームロールではありません。".formatted(role.getName())).queue();
            return;
        }
        guild.addRoleToMember(member, role).queue(
            tmp -> message.reply(
                "ゲームロール `%s` を付与しました。解除(剥奪)するには `/gamerole take %s` で可能です。".formatted(
                    role.getName(),
                    role.getName()
                )
            ).queue(),
            e -> message.reply(
                "ロールの付与に失敗しました: `%s`".formatted(e.getMessage())
            ).queue()
        );
        updateMessages();
    }

    private void takeGameRole(CommandEvent event, List<String> argNames) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();
        CommandArgument args = new CommandArgument(event.getArgs());
        if (guild.getIdLong() != SERVER_ID) {
            message.reply("このサーバではこのコマンドは使用できません。").queue();
            return;
        }
        if (!JavajaotanLibrary.isGrantedRole(member, Roles.MinecraftConnected.getRole())) {
            message.reply("ゲームロールの付与は MinecraftConnected から利用可能です。").queue();
            return;
        }
        String roleName = args.getString("name");
        List<Role> matchRoles = guild.getRolesByName(roleName, false);
        if (matchRoles.isEmpty()) {
            message.reply("マッチするロールが見つかりませんでした。").queue();
            return;
        }
        Role role = matchRoles.get(0);
        if (!GameRole.isGameRole(role)) {
            message.reply("指定されたロール `%s` はゲームロールではありません。".formatted(role.getName())).queue();
            return;
        }
        guild.removeRoleFromMember(member, role).queue(
            tmp -> message.reply(
                "ゲームロール `%s` を剥奪しました。".formatted(
                    role.getName()
                )
            ).queue(),
            e -> message.reply(
                "ロールの剥奪に失敗しました: `%s`".formatted(e.getMessage())
            ).queue()
        );
        updateMessages();
    }

    private void createGameRoleMessage(CommandEvent event) {
        Guild guild = event.getGuild();
        MessageChannel channel = event.getChannel();
        Member member = event.getMember();
        Message message = event.getMessage();
        JDA jda = Main.getJDA();
        if (guild.getIdLong() != SERVER_ID) {
            message.reply("このサーバではこのコマンドは使用できません。").queue();
            return;
        }
        if (!JavajaotanLibrary.isGrantedRole(member, Roles.Admin.getRole())) {
            message.reply("ゲームロールメッセージの投稿は Admin のみ利用可能です。").queue();
            return;
        }

        List<String> games = GameRole.getGameRoles();
        if (games == null) {
            message.reply("ゲームロールの取得に失敗しました。").queue();
            return;
        }
        if (games.isEmpty()) {
            message.reply("ゲームロールが見つかりませんでした。").queue();
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("__**:video_game: ゲームロール一覧 :video_game:**__\n");
        sb.append("所持しているゲームの絵文字をこのメッセージにリアクションすると、ゲームロールが付与されます。\n");
        sb.append("ロールの剥奪（解除）は運営側では対応しません。`/gamerole take <GAMENAME>`（GAMENAMEはゲーム名）で解除できます。\n\n");
        List<String> usedEmojis = GameRole.getUsedGameEmojis();
        List<String> emojiIds = guild.getEmojis()
            .stream()
            .map(RichCustomEmoji::getId)
            .filter(s -> !usedEmojis.contains(s))
            .toList();
        List<RichCustomEmoji> emojis = new ArrayList<>();

        for (String gameId : games) {
            Role role = guild.getRoleById(gameId);
            if (role == null) {
                continue;
            }
            String gameName = role.getName();
            String gameEmoji = GameRole.getGameEmoji(gameId);
            while (gameEmoji == null || sb.toString().contains(gameEmoji) || jda.getEmojiById(gameEmoji) == null) {
                gameEmoji = emojiIds.get(new Random().nextInt(emojiIds.size()));
            }
            RichCustomEmoji emoji = jda.getEmojiById(gameEmoji);
            if (emoji == null) {
                continue;
            }
            sb
                .append(emoji.getAsMention())
                .append(" ")
                .append(gameName)
                .append("\n");
            emojis.add(emoji);
        }

        Message postMessage = channel.sendMessage(sb.toString()).complete();
        for (RichCustomEmoji emoji : emojis) {
            postMessage.addReaction(emoji).queue();
        }
        addMessage(postMessage);
    }

    private void updateGameRoleMessage(CommandEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();
        if (guild.getIdLong() != SERVER_ID) {
            message.reply("このサーバではこのコマンドは使用できません。").queue();
            return;
        }
        if (!JavajaotanLibrary.isGrantedRole(member, Roles.Admin.getRole())) {
            message.reply("ゲームロールメッセージの投稿は Admin のみ利用可能です。").queue();
            return;
        }
        updateMessages();
        message.reply("ゲームロールメッセージの更新を行いました。").queue();
    }

    private void changeGameRoleEmoji(CommandEvent event, List<String> argNames) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Message message = event.getMessage();
        CommandArgument args = new CommandArgument(event.getArgs());
        JDA jda = Main.getJDA();
        if (guild.getIdLong() != SERVER_ID) {
            message.reply("このサーバではこのコマンドは使用できません。").queue();
            return;
        }
        if (!JavajaotanLibrary.isGrantedRole(member, Roles.Admin.getRole())) {
            message.reply("ゲームロール絵文字の更新は Admin のみ利用可能です。").queue();
            return;
        }
        String roleId = args.getString("role");
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            message.reply("指定されたロールが見つかりません。").queue();
            return;
        }
        if (!GameRole.isGameRole(role)) {
            message.reply("指定されたロール `%s` はゲームロールではありません。".formatted(role.getName())).queue();
            return;
        }
        String emojiId = args.getString("emojiId");
        Matcher matcher = emojiPattern.matcher(emojiId);
        if (matcher.matches()) {
            emojiId = matcher.group(1);
        }
        RichCustomEmoji emoji = jda.getEmojiById(emojiId);
        if (emoji == null) {
            message.reply("指定された絵文字が見つかりませんでした。").queue();
            return;
        }
        setGameEmoji(role, emojiId);
        message.reply("ゲームロール `%s` の絵文字を %s に変更しました。".formatted(role.getName(), emoji.getAsMention())).queue();
        updateMessages();
    }

    private void addGameRole(Message message, String name) {
        Guild guild = message.getGuild();
        JSONArray roles;
        try {
            roles = Files.exists(path) ? new JSONArray(Files.readString(path)) : new JSONArray();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            message.reply(
                "ロールの作成に失敗しました: `%s`".formatted(e.getMessage())
            ).queue();
            return;
        }
        guild
            .createRole()
            .setName(name)
            .setMentionable(true)
            .setPermissions(0L)
            .queue(
                role -> {
                    guild
                        .modifyRolePositions()
                        .selectPosition(role)
                        .moveTo(Objects.requireNonNull(guild.getRoleById(GAME_ROLE_BORDER_ID)).getPosition() - 1)
                        .queue();

                    message.reply(
                        "ロールの作成に成功しました: %s\nゲームを所持している場合は `/gamerole give %s` で自分にロールを付与しましょう！".formatted(
                            role.getAsMention(),
                            role.getName()
                        )).queue();
                    roles.put(role.getIdLong());
                    try {
                        Files.writeString(path, roles.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                },
                e -> {
                    message.reply(
                        "ロールの作成に失敗しました: `%s`".formatted(e.getMessage())
                    ).queue();
                    e.printStackTrace();
                }
            );
    }

    private void setGameEmoji(Role gameRole, String emojiId) {
        JSONObject games;
        try {
            games = Files.exists(pathMessages) ? new JSONObject(Files.readString(pathMessages)) : new JSONObject();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return;
        }
        if (!games.has("emojis")) {
            games.put("emojis", new JSONObject());
        }
        JSONObject emojis = games.getJSONObject("emojis");
        emojis.put(gameRole.getId(), emojiId);
        try {
            Files.writeString(pathMessages, games.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addMessage(Message message) {
        JSONObject games;
        try {
            games = Files.exists(pathMessages) ? new JSONObject(Files.readString(pathMessages)) : new JSONObject();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return;
        }
        if (!games.has("messages")) {
            games.put("messages", new JSONArray());
        }
        JSONArray messages = games.getJSONArray("messages");
        messages.put(new JSONObject()
            .put("channelId", message.getChannel().getId())
            .put("messageId", message.getId()));
        try {
            Files.writeString(pathMessages, games.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateMessages() {
        JSONObject games;
        try {
            games = Files.exists(pathMessages) ? new JSONObject(Files.readString(pathMessages)) : new JSONObject();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return;
        }
        if (!games.has("messages")) {
            games.put("messages", new JSONArray());
        }
        Guild guild = Main.getJDA().getGuildById(SERVER_ID);
        if (guild == null) {
            return;
        }
        JDA jda = Main.getJDA();
        List<String> gameRoles = GameRole.getGameRoles();
        if (gameRoles == null) {
            return;
        }
        if (gameRoles.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("__**:video_game: ゲームロール一覧 :video_game:**__\n");
        sb.append("所持しているゲームの絵文字をこのメッセージにリアクションすると、ゲームロールが付与されます。\n");
        sb.append("ロールの剥奪（解除）は運営側では対応しません。`/gamerole take <GAMENAME>`（GAMENAMEはゲーム名）で解除できます。\n\n");
        List<String> usedEmojis = GameRole.getUsedGameEmojis();
        List<String> emojiIds = guild.getEmojis()
            .stream()
            .map(RichCustomEmoji::getId)
            .filter(s -> !usedEmojis.contains(s))
            .toList();
        List<RichCustomEmoji> emojis = new ArrayList<>();

        for (String gameId : gameRoles) {
            Role role = guild.getRoleById(gameId);
            if (role == null) {
                continue;
            }
            String gameName = role.getName();
            String gameEmoji = GameRole.getGameEmoji(gameId);
            while (gameEmoji == null || sb.toString().contains(gameEmoji) || jda.getEmojiById(gameEmoji) == null) {
                gameEmoji = emojiIds.get(new Random().nextInt(emojiIds.size()));
            }
            RichCustomEmoji emoji = jda.getEmojiById(gameEmoji);
            if (emoji == null) {
                continue;
            }
            sb
                .append(emoji.getAsMention())
                .append(" ")
                .append(gameName)
                .append("\n");
            emojis.add(emoji);
        }
        JSONArray messages = games.getJSONArray("messages");
        for (int i = 0; i < messages.length(); i++) {
            JSONObject data = messages.getJSONObject(i);
            String channelId = data.getString("channelId");
            String messageId = data.getString("messageId");
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel == null) {
                return;
            }
            String content = sb.toString();
            Message message = channel.editMessageById(messageId, content).complete();
            message.clearReactions().complete();
            for (RichCustomEmoji emoji : emojis) {
                message.addReaction(emoji).queue();
            }
        }
    }

}
