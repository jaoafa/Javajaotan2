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
import cloud.commandframework.CommandComponent;
import cloud.commandframework.arguments.StaticArgument;
import cloud.commandframework.exceptions.*;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.jda.JDA4CommandManager;
import cloud.commandframework.jda.JDACommandSender;
import cloud.commandframework.jda.JDAGuildSender;
import cloud.commandframework.meta.CommandMeta;
import com.jaoafa.javajaotan2.lib.*;
import com.jaoafa.javajaotan2.tasks.Task_CheckMailVerified;
import com.jaoafa.javajaotan2.tasks.Task_MemberOrganize;
import com.jaoafa.javajaotan2.tasks.Task_PermSync;
import com.jaoafa.javajaotan2.tasks.Task_SyncOtherServerPerm;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Main {
    static boolean isUserDevelopMode = false;
    static boolean isGuildDevelopMode = false;
    static long developUserId;
    static long developGuildId;
    static Logger logger = LoggerFactory.getLogger("Javajaotan2");
    static JavajaotanConfig config;
    static JDA jda;
    static JSONArray commands;
    static WatchEmojis watchEmojis;

    public static void main(String[] args) {
        isUserDevelopMode = new File("../build.json").exists();
        if (isUserDevelopMode) {
            try {
                String json = String.join("\n", Files.readAllLines(new File("../build.json").toPath()));
                JSONObject config = new JSONObject(json);
                logger.warn("開発(ベータ)モードで動作しています。ユーザー「" + config.getString("builder") + "」の行動のみ反応します。");
                developUserId = Long.parseLong(config.getString("builderId"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (new File("this-server-is-development").exists()) {
            isGuildDevelopMode = true;
            try {
                String json = String.join("\n", Files.readAllLines(new File("config.json").toPath()));
                JSONObject config = new JSONObject(json);
                if (!config.has("guild_id")) {
                    logger.error("コンフィグに guild_id が定義されていません。対象とするサーバIDを指定してください");
                    System.exit(1);
                    return;
                }
                logger.warn("開発(ベータ)モードで動作しています。サーバID「" + config.getString("guild_id") + "」での行動のみ反応します。");
                developGuildId = Long.parseLong(config.getString("guild_id"));
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

        defineChannelsAndRoles();
        copyExternalScripts();

        registerCommand(jda);
        registerTask();

        watchEmojis = new WatchEmojis();

        jda.getGuilds().forEach(g -> new InviteLink(g).fetchInvites());

        if (!isUserDevelopMode && !isGuildDevelopMode) {
            new HTTPServer().start();
        }
    }

    static void registerCommand(JDA jda) {
        try {
            final JDA4CommandManager<JDACommandSender> manager = new JDA4CommandManager<>(
                jda,
                message -> "/",
                (sender, perm) -> {
                    logger.info("Check permission: " + perm);
                    MessageReceivedEvent event = sender.getEvent().orElse(null);
                    if (isUserDevelopMode) {
                        if (event == null) {
                            return false;
                        }
                        if (developUserId != event.getMessage().getIdLong()) {
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

                    if (sender instanceof JDAGuildSender jdaGuildSender) {
                        return new JDAGuildSender(event, jdaGuildSender.getMember(), jdaGuildSender.getTextChannel());
                    }

                    return null;
                },
                user -> {
                    MessageReceivedEvent event = user.getEvent().orElse(null);

                    if (user instanceof JDAGuildSender guildUser) {
                        return new JDAGuildSender(event, guildUser.getMember(), guildUser.getTextChannel());
                    }

                    return null;
                }
            );

            manager.registerExceptionHandler(NoSuchCommandException.class, (c, e) ->
                logger.info("NoSuchCommandException: " + e.getSuppliedCommand() + " (From " + c.getUser().getAsTag() + " in " + c.getChannel().getName() + ")")
            );

            manager.registerExceptionHandler(InvalidSyntaxException.class,
                (c, e) -> {
                    logger.info("InvalidSyntaxException: " + e.getMessage() + " (From " + c.getUser().getAsTag() + " in " + c.getChannel().getName() + ")");
                    if (c.getEvent().isPresent()) {
                        if (isGuildDevelopMode && developGuildId != c.getEvent().get().getGuild().getIdLong()) {
                            return;
                        }
                        c.getEvent().get().getMessage().reply(String.format("コマンドの構文が不正です。正しい構文: `%s`", e.getCorrectSyntax())).queue();
                    }
                });

            manager.registerExceptionHandler(NoPermissionException.class, (c, e) -> {
                logger.info("NoPermissionException: " + e.getMessage() + " (From " + c.getUser().getAsTag() + " in " + c.getChannel().getName() + ")");
                if (isUserDevelopMode && developUserId != c.getUser().getIdLong()) {
                    return;
                }
                if (c.getEvent().isPresent()) {
                    if (isGuildDevelopMode && developGuildId != c.getEvent().get().getGuild().getIdLong()) {
                        return;
                    }
                    c.getEvent().get().getMessage().reply("コマンドを使用する権限がありません。").queue();
                }
            });

            manager.registerExceptionHandler(CommandExecutionException.class, (c, e) -> {
                logger.info("CommandExecutionException: " + e.getMessage() + " (From " + c.getUser().getAsTag() + " in " + c.getChannel().getName() + ")");
                e.printStackTrace();
                if (c.getEvent().isPresent()) {
                    if (isGuildDevelopMode && developGuildId != c.getEvent().get().getGuild().getIdLong()) {
                        return;
                    }
                    c.getEvent().get().getMessage().reply(MessageFormat.format("コマンドの実行に失敗しました: {0} ({1})",
                        e.getMessage(),
                        e.getClass().getName())).queue();
                }
            });

            commands = new JSONArray();
            ClassFinder classFinder = new ClassFinder();
            for (Class<?> clazz : classFinder.findClasses("com.jaoafa.javajaotan2.command")) {
                if (!clazz.getName().startsWith("com.jaoafa.javajaotan2.command.Cmd_")) {
                    continue;
                }
                if (clazz.getEnclosingClass() != null) {
                    continue;
                }
                if (clazz.getName().contains("$")) {
                    continue;
                }
                String commandName = clazz.getName().substring("com.jaoafa.javajaotan2.command.Cmd_".length())
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
                    JSONArray subcommands = new JSONArray();
                    cmdPremise.register(builder).getCommands().forEach(cmd -> {
                        try {
                            manager.command(cmd);
                            JSONObject subcommand = new JSONObject();
                            subcommand.put("meta", cmd.getCommandMeta().getAllValues());
                            subcommand.put("senderType", cmd.getSenderType().isPresent() ?
                                cmd.getSenderType().get().getName() : null);
                            subcommand.put("toString", cmd.toString());

                            final Iterator<CommandComponent<JDACommandSender>> iterator = cmd.getComponents().iterator();
                            JSONArray args = new JSONArray();
                            cmd.getArguments().forEach(arg -> {
                                JSONObject obj = new JSONObject();
                                obj.put("name", arg.getName());
                                if (arg instanceof StaticArgument) {
                                    obj.put("alias", ((StaticArgument<?>) arg).getAlternativeAliases());
                                }
                                obj.put("isRequired", arg.isRequired());
                                obj.put("defaultValue", arg.getDefaultValue());
                                obj.put("defaultDescription", arg.getDefaultDescription());
                                obj.put("class", arg.getClass().getName());

                                if (iterator.hasNext()) {
                                    final CommandComponent<JDACommandSender> component = iterator.next();
                                    if (!component.getArgumentDescription().isEmpty()) {
                                        obj.put("description", component.getArgumentDescription().getDescription());
                                    }
                                }
                                args.put(obj);
                            });
                            subcommand.put("arguments", args);
                            subcommands.put(subcommand);
                        } catch (AmbiguousNodeException e) {
                            getLogger().warn(String.format("%s: コマンドの登録に失敗したため、このコマンドは使用できません: AmbiguousNodeException", cmd.toString()));
                            getLogger().warn("このエラーは、コマンドフレームワークがコマンドの引数を見分けられないエラーによるものです。literalを追加して固有なコマンドと見なせるように修正してください。");
                        }
                    });

                    JSONObject details = new JSONObject();
                    details.put("class", instance.getClass().getName());
                    details.put("name", commandName);
                    details.put("command", cmdPremise.details().getName());
                    details.put("description", cmdPremise.details().getDescription());
                    details.put("alias", cmdPremise.details().getAliases());
                    details.put("permissions", JavajaotanCommand.permRoles(cmdPremise.details().getAllowRoles()));
                    details.put("subcommands", subcommands);
                    commands.put(details);

                    getLogger().info(String.format("%s: コマンドの登録に成功しました。", commandName));
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    getLogger().warn(String.format("%s: コマンドの登録に失敗しました。", commandName));
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
            for (Class<?> clazz : classFinder.findClasses("com.jaoafa.javajaotan2.event")) {
                if (!clazz.getName().startsWith("com.jaoafa.javajaotan2.event.Event_")) {
                    continue;
                }
                if (clazz.getEnclosingClass() != null) {
                    continue;
                }
                if (clazz.getName().contains("$")) {
                    continue;
                }
                String eventName = clazz.getName().substring("com.jaoafa.javajaotan2.event.Event_".length());
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

    static void registerTask() {
        SchedulerFactory factory = new StdSchedulerFactory();
        List<TaskConfig> tasks = List.of(
            new TaskConfig(
                Task_MemberOrganize.class,
                "memberOrganize",
                "javajaotan2",
                DailyTimeIntervalScheduleBuilder
                    .dailyTimeIntervalSchedule()
                    .startingDailyAt(TimeOfDay.hourMinuteAndSecondOfDay(0, 0, 0))
                    .endingDailyAfterCount(1)),
            new TaskConfig(
                Task_CheckMailVerified.class,
                "checkMailVerified",
                "javajaotan2",
                DailyTimeIntervalScheduleBuilder
                    .dailyTimeIntervalSchedule()
                    .startingDailyAt(TimeOfDay.hourMinuteAndSecondOfDay(0, 0, 0))
                    .withInterval(5, DateBuilder.IntervalUnit.MINUTE)),
            new TaskConfig(
                Task_PermSync.class,
                "permSync",
                "javajaotan2",
                DailyTimeIntervalScheduleBuilder
                    .dailyTimeIntervalSchedule()
                    .startingDailyAt(TimeOfDay.hourMinuteAndSecondOfDay(0, 0, 0))
                    .withInterval(30, DateBuilder.IntervalUnit.MINUTE)),
            new TaskConfig(
                Task_SyncOtherServerPerm.class,
                "otherServerPermSync",
                "javajaotan2",
                DailyTimeIntervalScheduleBuilder
                    .dailyTimeIntervalSchedule()
                    .startingDailyAt(TimeOfDay.hourMinuteAndSecondOfDay(0, 0, 0))
                    .withInterval(30, DateBuilder.IntervalUnit.MINUTE))
        );

        try {
            Scheduler scheduler = factory.getScheduler();
            scheduler.start();

            for (TaskConfig task : tasks) {
                logger.info("registerTask: " + task.name());
                scheduler.scheduleJob(
                    JobBuilder.newJob(task.clazz())
                        .withIdentity(task.name(), task.group())
                        .build(),
                    TriggerBuilder.newTrigger()
                        .withIdentity(task.name(), task.group())
                        .withSchedule(task.scheduleBuilder())
                        .build()
                );
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    record TaskConfig(Class<? extends Job> clazz, String name, String group,
                      ScheduleBuilder<?> scheduleBuilder) {
    }

    static void defineChannelsAndRoles() {
        if (!new File("defines.json").exists()) {
            return;
        }
        getLogger().info("定義ファイル defines.json が見つかったため、チャンネル・ロールIDを上書きします。");
        try {
            String definesJson = String.join("\n", Files.readAllLines(new File("defines.json").toPath()));
            JSONObject defines = new JSONObject(definesJson);
            JSONObject channels = defines.getJSONObject("channels");
            for (Channels channel : Channels.values()) {
                if (!channels.has(channel.name())) {
                    continue;
                }
                channel.setChannelId(channels.getLong(channel.name()));
            }

            JSONObject roles = defines.getJSONObject("roles");
            for (Roles role : Roles.values()) {
                if (!roles.has(role.name())) {
                    continue;
                }
                role.setRoleId(roles.getLong(role.name()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void copyExternalScripts() {
        String srcDirName = "external_scripts";
        File destDir = new File("external_scripts/");

        final File jarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());

        if (!jarFile.isFile()) {
            logger.warn("仕様によりexternal_scriptsディレクトリをコピーできません。ビルドしてから実行すると、external_scriptsを使用する機能を利用できます。");
            return;
        }
        try (JarFile jar = new JarFile(jarFile)) {
            for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith(srcDirName + "/") && !entry.isDirectory()) {
                    File dest = new File(destDir, entry.getName().substring(srcDirName.length() + 1));
                    File parent = dest.getParentFile();
                    if (parent != null) {
                        //noinspection ResultOfMethodCallIgnored
                        parent.mkdirs();
                    }
                    logger.info("[external_scripts] Copy " + entry.getName().substring(srcDirName.length() + 1));
                    try (FileOutputStream out = new FileOutputStream(dest); InputStream in = jar.getInputStream(entry)) {
                        byte[] buffer = new byte[8 * 1024];
                        int s;
                        while ((s = in.read(buffer)) > 0) {
                            out.write(buffer, 0, s);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static org.slf4j.Logger getLogger() {
        return logger;
    }

    public static JavajaotanConfig getConfig() {
        return config;
    }

    public static void setConfig(JavajaotanConfig config) {
        Main.config = config;
    }

    public static JDA getJDA() {
        return jda;
    }

    public static boolean isDevelopMode() {
        return isUserDevelopMode || isGuildDevelopMode;
    }

    public static boolean isUserDevelopMode() {
        return isUserDevelopMode;
    }

    public static boolean isGuildDevelopMode() {
        return isGuildDevelopMode;
    }

    public static JSONArray getCommands() {
        return commands;
    }

    public static WatchEmojis getWatchEmojis() {
        return watchEmojis;
    }
}
