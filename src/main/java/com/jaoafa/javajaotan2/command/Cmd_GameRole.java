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
import com.jaoafa.javajaotan2.lib.Roles;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class Cmd_GameRole implements CommandPremise {
    Path path = Path.of("gameRoles.json");
    long SERVER_ID = 597378876556967936L;
    long GAME_ROLE_BORDER_ID = 911556139496374293L;

    @Override
    public JavajaotanCommand.Detail details() {
        return new JavajaotanCommand.Detail(
            "gamerole",
            "ゲームロール関連コマンド"
        );
    }

    @Override
    public JavajaotanCommand.Cmd register(Command.Builder<JDACommandSender> builder) {
        return new JavajaotanCommand.Cmd(
            builder
                .meta(CommandMeta.DESCRIPTION, "ゲームロールを新規追加します。（CommunityRegular以上のみ使用可能）")
                .literal("new", "create", "add")
                .argument(StringArgument.greedy("name"))
                .handler(context -> execute(context, this::newGameRole))
                .build(),
            builder
                .meta(CommandMeta.DESCRIPTION, "ゲームロールの色を変更します。名前にスペースが入る場合はダブルクォーテーションで囲む必要があります。（CommunityRegular以上のみ使用可能）")
                .literal("color")
                .argument(StringArgument.quoted("name"))
                .argument(StringArgument.greedy("colorCode"))
                .handler(context -> execute(context, this::changeColor))
                .build(),
            builder
                .meta(CommandMeta.DESCRIPTION, "ゲームロールの色を変更します。（CommunityRegular以上のみ使用可能）")
                .literal("rename")
                .argument(StringArgument.quoted("name"))
                .argument(StringArgument.quoted("newName"))
                .handler(context -> execute(context, this::renameRole))
                .build(),
            builder
                .meta(CommandMeta.DESCRIPTION, "自分にゲームロールを付与します。")
                .literal("give", "join")
                .argument(StringArgument.greedy("name"))
                .handler(context -> execute(context, this::giveGameRole))
                .build(),
            builder
                .meta(CommandMeta.DESCRIPTION, "自分からゲームロールを剥奪します。")
                .literal("take", "leave")
                .argument(StringArgument.of("name"))
                .handler(context -> execute(context, this::takeGameRole))
                .build()
        );
    }

    private void newGameRole(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
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
        String roleName = context.get("name");
        List<Role> matchRoles = guild.getRolesByName(roleName, false);
        if (!matchRoles.isEmpty()) {
            message.reply("同一名のロールがあるため、作成できません。").queue();
            return;
        }
        addGameRole(message, roleName);
    }

    private void changeColor(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
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
            color = Color.decode(context.get("colorCode"));
        } catch (NumberFormatException e) {
            message.reply("色コードが正しくありません。").queue();
            return;
        }
        String roleName = context.get("name");
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

    private void renameRole(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
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
        String roleName = context.get("name");
        List<Role> matchRoles = guild.getRolesByName(roleName, false);
        if (matchRoles.isEmpty()) {
            message.reply("マッチするロールが見つかりませんでした。").queue();
            return;
        }
        String newRoleName = context.get("newName");
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
    }

    private void giveGameRole(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        if (guild.getIdLong() != SERVER_ID) {
            message.reply("このサーバではこのコマンドは使用できません。").queue();
            return;
        }
        if (!JavajaotanLibrary.isGrantedRole(member, Roles.MinecraftConnected.getRole())) {
            message.reply("ゲームロールの付与は MinecraftConnected から利用可能です。").queue();
            return;
        }
        String roleName = context.get("name");
        List<Role> matchRoles = guild.getRolesByName(roleName, false);
        if (matchRoles.isEmpty()) {
            message.reply("マッチするロールが見つかりませんでした。").queue();
            return;
        }
        Role role = matchRoles.get(0);
        if (!isGameRole(role)) {
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
    }

    private void takeGameRole(@NotNull Guild guild, @NotNull MessageChannel channel, @NotNull Member member, @NotNull Message message, @NotNull CommandContext<JDACommandSender> context) {
        if (guild.getIdLong() != SERVER_ID) {
            message.reply("このサーバではこのコマンドは使用できません。").queue();
            return;
        }
        if (!JavajaotanLibrary.isGrantedRole(member, Roles.MinecraftConnected.getRole())) {
            message.reply("ゲームロールの付与は MinecraftConnected から利用可能です。").queue();
            return;
        }
        String roleName = context.get("name");
        List<Role> matchRoles = guild.getRolesByName(roleName, false);
        if (matchRoles.isEmpty()) {
            message.reply("マッチするロールが見つかりませんでした。").queue();
            return;
        }
        Role role = matchRoles.get(0);
        if (!isGameRole(role)) {
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

    private boolean isGameRole(Role role) {
        JSONArray roles;
        try {
            roles = Files.exists(path) ? new JSONArray(Files.readString(path)) : new JSONArray();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }
        return roles.toList().contains(role.getIdLong());
    }
}
