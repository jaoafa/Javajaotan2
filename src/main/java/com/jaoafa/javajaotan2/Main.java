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
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jaoafa.javajaotan2.lib.*;
import com.jaoafa.javajaotan2.tasks.Task_CheckMailVerified;
import com.jaoafa.javajaotan2.tasks.Task_MemberOrganize;
import com.jaoafa.javajaotan2.tasks.Task_PermSync;
import com.jaoafa.javajaotan2.tasks.Task_SyncOtherServerPerm;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
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
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.DIRECT_MESSAGE_TYPING, GatewayIntent.GUILD_EMOJIS_AND_STICKERS)
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

        final String prefix = "/";
        builder.setPrefix(prefix);
        builder.setActivity(null);
        builder.setOwnerId(config.getOwnerId());

        Reflections reflections = new Reflections("com.jaoafa.javajaotan2.command");
        Set<Class<? extends Command>> subTypes = reflections.getSubTypesOf(Command.class);
        List<Command> commands = new ArrayList<>();

        for (Class<? extends Command> theClass : subTypes) {

            if (!theClass.getName().startsWith("com.jaoafa.javajaotan2.command.Cmd_")) {
                continue;
            }
            if (theClass.getName().contains("SubCommand") || theClass.getName().contains("SlashCommand")) {
                continue;
            }
            String cmdName = theClass.getName().substring("com.jaoafa.javajaotan2.command.Cmd_".length());

            try {
                commands.add(theClass.getDeclaredConstructor().newInstance());
                getLogger().info("%s: コマンドの登録に成功しました".formatted(cmdName));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                getLogger().error("%s: コマンドの登録に失敗しました".formatted(cmdName));
            }
        }
        builder.addCommands(commands.toArray(new Command[0]));

        builder.useHelpBuilder(false);

        // とりあえずスラッシュコマンドはサポートしない

        return builder.build();
    }

    static void registerEvent(JDABuilder jdaBuilder) {
        Reflections reflections = new Reflections("com.jaoafa.javajaotan2.event");
        Set<Class<? extends ListenerAdapter>> subTypes = reflections.getSubTypesOf(ListenerAdapter.class);
        for (Class<? extends ListenerAdapter> clazz : subTypes) {
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
