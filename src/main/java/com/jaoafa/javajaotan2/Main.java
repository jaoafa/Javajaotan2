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

package com.jaoafa.javajaotan2;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.exceptions.InvalidSyntaxException;
import cloud.commandframework.exceptions.NoPermissionException;
import cloud.commandframework.exceptions.NoSuchCommandException;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.jda.JDA4CommandManager;
import cloud.commandframework.jda.JDACommandSender;
import cloud.commandframework.jda.JDAGuildSender;
import cloud.commandframework.meta.CommandMeta;
import com.jaoafa.javajaotan2.lib.ClassFinder;
import com.jaoafa.javajaotan2.lib.CommandPremise;
import com.jaoafa.javajaotan2.lib.JavajaotanCommand;
import com.jaoafa.javajaotan2.lib.JavajaotanConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Arrays;

public class Main {
    static boolean isDevelopMode = false;
    static long developUserId;
    static Logger logger;
    static JavajaotanConfig config;
    static JDA jda;

    public static void main(String[] args) {
        logger = LoggerFactory.getLogger("Javajaotan2");

        isDevelopMode = new File("../build.json").exists();
        if (isDevelopMode) {
            try {
                String json = String.join("\n", Files.readAllLines(new File("../build.json").toPath()));
                JSONObject config = new JSONObject(json);
                logger.warn("開発(ベータ)モードで動作しています。ユーザー「" + config.getString("builder") + "」の行動のみ反応します。");
                developUserId = Long.parseLong(config.getString("builderId"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            config = new JavajaotanConfig();
        } catch (RuntimeException e) {
            System.exit(1);
            return;
        }

        // ログイン
        try {
            JDABuilder jdabuilder = JDABuilder.createDefault(config.getToken())
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES,
                    GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.DIRECT_MESSAGE_TYPING)
                .setAutoReconnect(true)
                .setBulkDeleteSplittingEnabled(false)
                .setContextEnabled(false);

            registerEvent(jdabuilder);

            jda = jdabuilder.build().awaitReady();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        commandRegister(jda);
    }

    static void commandRegister(JDA jda) {
        try {
            final JDA4CommandManager<JDACommandSender> manager = new JDA4CommandManager<>(
                jda,
                message -> "/",
                (sender, perm) -> {
                    MessageReceivedEvent event = sender.getEvent().orElse(null);
                    if (isDevelopMode) {
                        if (event == null) {
                            return false;
                        }
                        if (getDevelopUserId() != event.getMessage().getIdLong()) {
                            return false;
                        }
                    }
                    if (perm == null) {
                        return true; // 対象ロール設定がされていない場合許可
                    }
                    if (event == null || !event.isFromGuild() || event.getMember() == null) {
                        return false; // イベントがNULL、もしくはサーバからのメッセージ送信ではない場合不許可
                    }
                    Guild guild = event.getGuild();
                    long guild_id = guild.getIdLong();
                    if (guild_id != Main.getConfig().getGuildId()) {
                        return true; // 設定ファイルで定義されているサーバIDと違う場合許可
                    }
                    Member member = event.getMember();

                    return Arrays.stream(perm.split("\\|"))
                        .map(guild::getRoleById)
                        .anyMatch(role -> member.getRoles().contains(role));
                },
                AsynchronousCommandExecutionCoordinator.simpleCoordinator(),
                sender -> {
                    MessageReceivedEvent event = sender.getEvent().orElse(null);

                    if (sender instanceof JDAGuildSender) {
                        JDAGuildSender jdaGuildSender = (JDAGuildSender) sender;
                        return new JDAGuildSender(event, jdaGuildSender.getMember(), jdaGuildSender.getTextChannel());
                    }

                    return null;
                },
                user -> {
                    MessageReceivedEvent event = user.getEvent().orElse(null);

                    if (user instanceof JDAGuildSender) {
                        JDAGuildSender guildUser = (JDAGuildSender) user;
                        return new JDAGuildSender(event, guildUser.getMember(), guildUser.getTextChannel());
                    }

                    return null;
                }
            );

            manager.registerExceptionHandler(NoSuchCommandException.class, (c, e) -> {
            }); // コマンドがなくてもなにもしない

            manager.registerExceptionHandler(InvalidSyntaxException.class,
                (c, e) -> {
                    if (c.getEvent().isPresent()) {
                        c.getEvent().get().getMessage().reply(String.format("コマンドの構文が不正です。正しい構文: `%s`", e.getCorrectSyntax())).queue();
                    }
                });

            manager.registerExceptionHandler(NoPermissionException.class, (c, e) -> {
                if (isDevelopMode && getDevelopUserId() != c.getUser().getIdLong()) {
                    return;
                }
                if (c.getEvent().isPresent()) {
                    c.getEvent().get().getMessage().reply("コマンドを使用する権限がありません。").queue();
                }
            });

            manager.registerExceptionHandler(Exception.class, (c, e) -> {
                if (e instanceof NoSuchCommandException || e instanceof InvalidSyntaxException || e instanceof NoPermissionException) {
                    return;
                }
                if (c.getEvent().isPresent()) {
                    c.getEvent().get().getMessage().reply(MessageFormat.format("コマンドの実行に失敗しました: {0} ({1})",
                        e.getMessage(),
                        e.getClass().getName())).queue();
                }
            });


            ClassFinder classFinder = new ClassFinder();
            for (Class<?> clazz : classFinder.findClasses("com.jaoafa.mymaid4.command")) {
                if (!clazz.getName().startsWith("com.jaoafa.mymaid4.command.Cmd_")) {
                    continue;
                }
                if (clazz.getEnclosingClass() != null) {
                    continue;
                }
                if (clazz.getName().contains("$")) {
                    continue;
                }
                String commandName = clazz.getName().substring("com.jaoafa.mymaid4.command.Cmd_".length())
                    .toLowerCase();

                try {
                    Constructor<?> construct = clazz.getConstructor();
                    Object instance = construct.newInstance();
                    CommandPremise cmdPremise = (CommandPremise) instance;

                    Command.Builder<JDACommandSender> builder = manager.commandBuilder(
                        commandName,
                        ArgumentDescription.of(cmdPremise.details().getDescription()),
                        cmdPremise.details().getAliases().toArray(new String[0])
                    )
                        .meta(CommandMeta.DESCRIPTION, cmdPremise.details().getDescription());
                    if (cmdPremise.details().getAllowRoles() != null) {
                        builder = builder.permission(
                            JavajaotanCommand.permRoles(cmdPremise.details().getAllowRoles())
                        );
                    }
                    cmdPremise.register(builder).getCommands().forEach(manager::command);
                    System.out.println(commandName + " register successful");
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    System.out.println(commandName + " register failed");
                    e.printStackTrace();
                }
            }

            System.out.println(manager.getCommands());
        } catch (InterruptedException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    static void registerEvent(JDABuilder jdaBuilder) {
        try {
            ClassFinder classFinder = new ClassFinder();
            for (Class<?> clazz : classFinder.findClasses("com.jaoafa.mymaid4.event")) {
                if (!clazz.getName().startsWith("com.jaoafa.mymaid4.event.Event_")) {
                    continue;
                }
                if (clazz.getEnclosingClass() != null) {
                    continue;
                }
                if (clazz.getName().contains("$")) {
                    continue;
                }
                String eventName = clazz.getName().substring("com.jaoafa.mymaid4.event.Event_".length());
                Constructor<?> construct = clazz.getConstructor();
                Object instance = construct.newInstance();
                if (!(instance instanceof ListenerAdapter)) {
                    return;
                }

                jdaBuilder.addEventListeners(instance);
                getLogger().info(String.format("%s: イベントの登録に成功しました。", eventName));
            }
        } catch (ClassNotFoundException | IOException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            getLogger().error("イベントの登録に失敗しました。");
            e.printStackTrace();
        }
    }

    public static org.slf4j.Logger getLogger() {
        return logger;
    }

    public static JavajaotanConfig getConfig() {
        return config;
    }

    public static JDA getJDA() {
        return jda;
    }

    public static boolean isDevelopMode() {
        return isDevelopMode;
    }

    public static long getDevelopUserId() {
        return developUserId;
    }
}
