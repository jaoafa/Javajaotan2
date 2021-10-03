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

package com.jaoafa.javajaotan2.tasks;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jaoafa.javajaotan2.Main;
import com.jaoafa.javajaotan2.lib.JavajaotanLibrary;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 参加してから10分以内に発言のないユーザーをキックする
 */
public class Task_CheckMailVerified implements Job {
    final boolean dryRun;
    Logger logger;

    public Task_CheckMailVerified() {
        this.dryRun = false;
    }

    public Task_CheckMailVerified(boolean dryRun) {
        this.dryRun = dryRun;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        logger = Main.getLogger();
        Guild guild = Main.getJDA().getGuildById(Main.getConfig().getGuildId());

        if (guild == null) {
            logger.warn("guild == null");
            return;
        }

        Roles.setGuildAndRole(guild);

        ExecutorService service = Executors.newFixedThreadPool(10,
            new ThreadFactoryBuilder().setNameFormat(getClass().getSimpleName() + "-%d").build());
        guild.loadMembers()
            .onSuccess(members ->
                {
                    logger.info("loadMember(): %s".formatted(members.size()));
                    members.forEach(member ->
                        service.execute(new RunCheckMailVerified(member.hasTimeJoined() ? member : guild.retrieveMember(member.getUser()).complete()))
                    );
                }
            );
    }

    class RunCheckMailVerified implements Runnable {
        final Member member;

        public RunCheckMailVerified(Member member) {
            this.member = member;
        }

        @Override
        public void run() {
            if (member.getUser().isBot()) {
                logger.info("[%s] Bot".formatted(
                    member.getUser().getAsTag()
                ));
                return;
            }

            logger.info("[%s] Role: %s".formatted(
                member.getUser().getAsTag(),
                member.getRoles().stream().map(Role::getName).collect(Collectors.joining(", "))
            ));

            boolean isMailVerified = JavajaotanLibrary.isGrantedRole(member, Roles.MailVerified.role);
            boolean isNeedSupport = JavajaotanLibrary.isGrantedRole(member, Roles.NeedSupport.role);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime joinedTime = member.getTimeJoined().atZoneSameInstant(ZoneId.of("Asia/Tokyo")).toLocalDateTime();
            long joinMinutes = ChronoUnit.MINUTES.between(joinedTime, now);
            logger.info("[%s] joinMinutes: %s minutes".formatted(member.getUser().getAsTag(), joinMinutes));

            BiFunction<String, String, Boolean> doKick = (title, description) -> {
                kickDiscord(member, title, description);
                return true;
            };

            if (!isMailVerified && !isNeedSupport && joinMinutes >= 10) {
                // 参加してから10分以内に発言のないユーザーをキックする
                doKick.apply("MailVerifiedキック", "10分以上発言がなかったため、キックしました。");
            }
        }
    }

    private void kickDiscord(Member member, String title, String description) {
        Path path = Path.of("pre-permsync.json");
        JSONObject object = new JSONObject();
        if (Files.exists(path)) {
            try {
                object = new JSONObject(Files.readString(path));
            } catch (IOException e) {
                logger.warn("grantDiscordPerm json load failed.", e);
                return;
            }
        }

        if (!object.has("kick")) object.put("kick", new JSONArray());

        JSONArray kicks = object.getJSONArray("kick");

        if (kicks.toList().contains(member.getId()) && !dryRun) {
            // 処理
            notifyConnection(member, "[PROCESS] " + title, description);
            member.getGuild().kick(member).queue();
            kicks.remove(kicks.toList().indexOf(member.getId()));
        } else {
            // 動作予告
            notifyConnection(member, "[PRE] " + title, "次回処理時、本動作が実施されます。");
            kicks.put(member.getId());
        }

        object.put("kick", kicks);
        try {
            Files.writeString(path, object.toString());
        } catch (IOException e) {
            logger.warn("grantDiscordPerm json save failed.", e);
        }
    }

    private void notifyConnection(Member member, String title, String description) {
        TextChannel channel = Main.getJDA().getTextChannelById(891021520099500082L);

        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(Color.PINK)
            .setAuthor(member.getUser().getAsTag(), "https://discord.com/users/" + member.getId(), member.getUser().getEffectiveAvatarUrl());
        if (dryRun) embed.setFooter("DRY-RUN MODE");

        channel.sendMessageEmbeds(embed.build()).queue();
    }

    public enum Roles {
        Regular(597405176189419554L, null),
        CommunityRegular(888150763421970492L, null),
        Verified(597405176969560064L, null),
        MinecraftConnected(604011598952136853L, null),
        MailVerified(597421078817669121L, null),
        NeedSupport(786110419470254102L, null),
        SubAccount(753047225751568474L, null),
        Unknown(null, null);

        private final Long id;
        private Role role;

        Roles(Long id, Role role) {
            this.id = id;
            this.role = role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        /**
         * Guildからロールを取得します
         *
         * @param guild 対象Guild
         */
        public static void setGuildAndRole(Guild guild) {
            for (Roles role : Roles.values()) {
                if (role.equals(Unknown)) continue;
                role.setRole(guild.getRoleById(role.id));
            }
        }

        /**
         * 名前からロールを取得します
         *
         * @param name ロールの名前
         *
         * @return 取得したロール
         */
        public static Roles get(String name) {
            return Arrays.stream(values())
                .filter(role -> role.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(Roles.Unknown);
        }
    }
}
