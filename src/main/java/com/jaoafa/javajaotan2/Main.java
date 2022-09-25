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

package com.jaoafa.javajaotan2;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.command.ContextMenu;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jaoafa.javajaotan2.lib.*;
import com.jaoafa.javajaotan2.tasks.Task_CheckMailVerified;
import com.jaoafa.javajaotan2.tasks.Task_MemberOrganize;
import com.jaoafa.javajaotan2.tasks.Task_PermSync;
import com.jaoafa.javajaotan2.tasks.Task_SyncOtherServerPerm;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Main {
    static final String PREFIX = "/";

    static boolean isUserDevelopMode = false;
    static boolean isGuildDevelopMode = false;
    static long developUserId;
    static long developGuildId;
    static final Logger logger = LoggerFactory.getLogger("Javajaotan2");
    static JavajaotanConfig config;
    static JDA jda;
    static JSONArray commands;
    static WatchEmojis watchEmojis;
    static ScheduledExecutorService scheduler;
    private static CommandClientBuilder builder;
    private static String prefix;

    public static void main(String[] args) {
        logger.info("Starting Javajaotan2...");

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

        EventWaiter waiter = new EventWaiter();
        CommandClient client = getCommandClient();

        // ログイン
        try {
            JDABuilder jdabuilder = JDABuilder.createDefault(config.getToken())
                .enableIntents(
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.GUILD_PRESENCES,
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MESSAGE_TYPING,
                    GatewayIntent.DIRECT_MESSAGE_TYPING,
                    GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS
                )
                .setAutoReconnect(true)
                .setBulkDeleteSplittingEnabled(false)
                .setContextEnabled(false);

            jdabuilder.addEventListeners(waiter, client);
            registerEvent(jdabuilder);

            jda = jdabuilder.build().awaitReady();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        defineChannelsAndRoles();
        copyExternalScripts();

        registerTask();

        watchEmojis = new WatchEmojis();

        scheduler = Executors.newSingleThreadScheduledExecutor();

        jda.getGuilds().forEach(g -> new InviteLink(g).fetchInvites());

        if (!isUserDevelopMode && !isGuildDevelopMode) {
            new HTTPServer().start();
        }
    }

    // see https://github.com/Chewbotcca/Discord/blob/main/src/main/java/pw/chew/chewbotcca/Chewbotcca.java#L153-L205
    static CommandClient getCommandClient() {
        CommandClientBuilder builder = new CommandClientBuilder();

        builder.setPrefix(PREFIX);
        builder.setActivity(null);
        builder.setOwnerId(config.getOwnerId());

        registerCommand(builder);
        registerMenu(builder);

        // とりあえずスラッシュコマンドはサポートしない

        return builder.build();
    }

    static void registerCommand(CommandClientBuilder builder) {
        final String commandPackage = "com.jaoafa.javajaotan2.command";
        Reflections reflections = new Reflections(commandPackage);
        Set<Class<? extends Command>> subTypes = reflections.getSubTypesOf(Command.class);
        List<Command> commands = new ArrayList<>();

        for (Class<? extends Command> theClass : subTypes) {
            if (!theClass.getName().startsWith("%s.Cmd_".formatted(commandPackage))) {
                continue;
            }
            if (theClass.getName().contains("SubCommand") || theClass.getName().contains("SlashCommand")) {
                continue;
            }
            String cmdName = theClass.getName().substring(("%s.Cmd_".formatted(commandPackage)).length());

            try {
                commands.add(theClass.getDeclaredConstructor().newInstance());
                getLogger().info("%s: コマンドの登録に成功しました".formatted(cmdName));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                getLogger().error("%s: コマンドの登録に失敗しました".formatted(cmdName));
            }
        }
        builder.addCommands(commands.toArray(new Command[0]));

        // localhost:31002 でホストされるコマンド一覧APIへのコマンド登録
        // Javajaotan無印で実装済みコマンドを取得する関係上、暫定的にコマンド名のみを返す
        Main.commands = new JSONArray();
        for (Command command : commands) {
            Main.commands.put(new JSONObject().put("name", command.getName()));
        }

        builder.setHelpWord("jaotanhelp");
        /*
        `this.arguments` の値は以下の3種類
        - `<Arg>`: アクションが一つのときだけ使用。説明は this.help を流用
        - `: XXXXX`: アクションが複数あるが、このアクションについては引数がない場合に使用。説明は `:` 以降を利用
        - `<Arg1>: XXXXX\n<Arg2>: XXXXX`: アクションが複数のときに使用。説明は `:` 以降を利用

        --- サンプル ---

        Javajaotan2 コマンド一覧

        - `/test`: テストコマンド
          - `/test`: ルートテストコマンド
          - `/test <Arg>`: Argをテストする
          - `/test <Arg1> <Arg2>`: Arg1 と Arg2 をテストする
        - `/test2 <Arg>`: テストコマンド3
         */
        builder.setHelpConsumer((event) -> {
            Message message = event.getMessage();
            StringBuilder sb = new StringBuilder();
            sb.append("Javajaotan2 コマンド一覧 %s\n\n".formatted(event.getArgs().isEmpty() ? "" : "(フィルタリング: `" + event.getArgs() + "`)"));
            for (Command command : commands) {
                String arguments = command.getArguments();
                if (!event.getArgs().isEmpty() && !String.join("\n", List.of(
                    command.getName(),
                    command.getHelp(),
                    command.getArguments()
                )).contains(event.getArgs())) {
                    continue;
                }

                sb.append("- `")
                    .append(PREFIX)
                    .append(command.getName());
                if (!arguments.contains("\n")) {
                    sb.append(" ").append(arguments);
                }
                sb.append("`: ")
                    .append(command.getHelp())
                    .append("\n");

                if (!event.getArgs().isEmpty() && arguments.contains("\n")) {
                    // 複数のアクション

                    // - `/test`: テストコマンド\n
                    String[] actions = arguments.split("\n");
                    for (String action : actions) {
                        // - `/test <Arg>`: Argをテストする\n
                        if (action.contains(": ")) {
                            String[] split = action.split(": ");
                            sb.append("  - `")
                                .append(PREFIX)
                                .append(command.getName())
                                .append(" ")
                                .append(split[0])
                                .append("`: ")
                                .append(split[1])
                                .append("\n");
                        } else {
                            // 説明なしのアクション？
                            sb.append("  - `")
                                .append(PREFIX)
                                .append(command.getName())
                                .append(" ")
                                .append(action)
                                .append("\n");
                        }
                    }
                }
            }

            sb.append("\n");
            if (event.getArgs().isEmpty()) {
                sb.append("`/jaotanhelp <テキスト>` で検索（フィルタリング）できます。\n");
            }
            sb.append("不具合の報告は対象メッセージに :bug: をつけるか、GitHub の jaoafa/Javajaotan2 リポジトリの Issue にてお願いします。");

            event.replyInDm(
                sb.toString(),
                m -> {
                    if (message.isFromType(ChannelType.TEXT))
                        message.reply("DMにヘルプメッセージを送信しました。").queue();
                },
                t -> message.reply("ヘルプメッセージをDMに送信できませんでした。DM受取設定などをご確認ください。").queue()
            );
        });
    }

    static void registerEvent(JDABuilder jdaBuilder) {
        final String eventPackage = "com.jaoafa.javajaotan2.event";
        Reflections reflections = new Reflections(eventPackage);
        Set<Class<? extends ListenerAdapter>> subTypes = reflections.getSubTypesOf(ListenerAdapter.class);
        for (Class<? extends ListenerAdapter> clazz : subTypes) {
            if (!clazz.getName().startsWith("%s.Event_".formatted(eventPackage))) {
                continue;
            }
            if (clazz.getEnclosingClass() != null) {
                continue;
            }
            if (clazz.getName().contains("$")) {
                continue;
            }
            String eventName = clazz.getName().substring(("%s.Event_".formatted(eventPackage)).length());
            try {
                Constructor<?> construct = clazz.getConstructor();
                Object instance = construct.newInstance();
                if (!(instance instanceof ListenerAdapter)) {
                    return;
                }

                jdaBuilder.addEventListeners(instance);
                getLogger().info("%s: イベントの登録に成功しました。".formatted(eventName));
            } catch (NoSuchMethodException | InstantiationException |
                     IllegalAccessException | InvocationTargetException e) {
                getLogger().error("%s: イベントの登録に失敗しました。".formatted(eventName));
                e.printStackTrace();
            }
        }
    }

    static void registerMenu(CommandClientBuilder builder) {
        final String commandPackage = "com.jaoafa.javajaotan2.menu";
        Reflections reflections = new Reflections(commandPackage);
        Set<Class<? extends ContextMenu>> subTypes = reflections.getSubTypesOf(ContextMenu.class);

        for (Class<? extends ContextMenu> theClass : subTypes) {
            try {
                builder.addContextMenu(theClass.getDeclaredConstructor().newInstance());
                getLogger().info("%s: メニューの登録に成功しました".formatted(theClass.getSimpleName()));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                getLogger().error("%s: メニューの登録に失敗しました".formatted(theClass.getSimpleName()));
            }
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

    public static ScheduledExecutorService getScheduler() {
        return scheduler;
    }
}
