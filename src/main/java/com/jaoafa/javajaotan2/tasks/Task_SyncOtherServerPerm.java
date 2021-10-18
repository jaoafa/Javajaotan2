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
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Task_SyncOtherServerPerm implements Job {
    final boolean dryRun;
    Logger logger;
    Guild guild;
    List<GuildMember> fruitMembers;
    List<GuildMember> sabamisoMembers;
    List<GuildMember> toroMembers;

    public Task_SyncOtherServerPerm() {
        this.dryRun = false;
    }

    public Task_SyncOtherServerPerm(boolean dryRun) {
        this.dryRun = dryRun;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        logger = Main.getLogger();
        logger.info("Task_SyncOtherServerPerm");
        guild = Main.getJDA().getGuildById(Main.getConfig().getGuildId());

        if (guild == null) {
            logger.warn("guild == null");
            return;
        }

        Roles.setGuildAndRole(guild);

        logger.info("loading fruitMembers");
        fruitMembers = getGuildMember(String.valueOf(Roles.FruitPlayers.guild_id));
        logger.info("fruitMembers count: %s".formatted(fruitMembers != null ? fruitMembers.size() : null));
        logger.info("sabamisoMembers");
        sabamisoMembers = getGuildMember(String.valueOf(Roles.SabamisoPlayers.guild_id));
        logger.info("sabamisoMembers count: %s".formatted(sabamisoMembers != null ? sabamisoMembers.size() : null));
        logger.info("toroMembers");
        toroMembers = getGuildMember(String.valueOf(Roles.TOROPlayers.guild_id));
        logger.info("toroMembers count: %s".formatted(toroMembers != null ? toroMembers.size() : null));

        if (fruitMembers == null || sabamisoMembers == null || toroMembers == null) {
            logger.error("いずれかの項目がnullだったため、処理を続行できません。");
            return;
        }

        ExecutorService service = Executors.newFixedThreadPool(10,
            new ThreadFactoryBuilder().setNameFormat(getClass().getSimpleName() + "-%d").build());
        guild.loadMembers()
            .onSuccess(members ->
                {
                    logger.info("loadMember(): %s".formatted(members.size()));
                    members.forEach(member ->
                        service.execute(new RunSyncOtherServerPerm(member.hasTimeJoined() ? member : guild.retrieveMember(member.getUser()).complete()))
                    );
                }
            );
    }

    class RunSyncOtherServerPerm implements Runnable {
        final Member member;

        public RunSyncOtherServerPerm(Member member) {
            this.member = member;
        }

        @Override
        public void run() {
            if (member.getUser().isBot()) {
                return;
            }

            boolean isFruitMember = fruitMembers.stream().anyMatch(m -> m.user_id.equals(member.getId()));
            boolean isSabamisoMember = sabamisoMembers.stream().anyMatch(m -> m.user_id.equals(member.getId()));
            boolean isTOROMember = toroMembers.stream().anyMatch(m -> m.user_id.equals(member.getId()));

            boolean isFruitPlayers = JavajaotanLibrary.isGrantedRole(member, Roles.FruitPlayers.role);
            boolean isSabamisoPlayers = JavajaotanLibrary.isGrantedRole(member, Roles.SabamisoPlayers.role);
            boolean isTOROPlayers = JavajaotanLibrary.isGrantedRole(member, Roles.TOROPlayers.role);

            // Fruit
            if (isFruitMember && !isFruitPlayers) {
                // 鯖にいるけどロールついてない
                notifyConnection(member, "FruitPlayers役職付与", "FruitServerのメンバーであるため、FruitPlayers役職を付与しました。", Color.getHSBColor(247, 79, 38));
                if (!dryRun) guild.addRoleToMember(member, Roles.FruitPlayers.role).queue();
            }

            if (!isFruitMember && isFruitPlayers) {
                // 鯖にいないのにロールついてる
                notifyConnection(member, "FruitPlayers役職剥奪", "FruitServerのメンバーでないため、FruitPlayers役職を剥奪しました。", Color.getHSBColor(247, 79, 38));
                if (!dryRun) guild.removeRoleFromMember(member, Roles.FruitPlayers.role).queue();
            }

            // Sabamiso
            if (isSabamisoMember && !isSabamisoPlayers) {
                // 鯖にいるけどロールついてない
                notifyConnection(member, "SabamisoPlayers役職付与", "SabamisoServerのメンバーであるため、SabamisoPlayers役職を付与しました。", Color.getHSBColor(144, 33, 93));
                if (!dryRun) guild.addRoleToMember(member, Roles.SabamisoPlayers.role).queue();
            }

            if (!isSabamisoMember && isSabamisoPlayers) {
                // 鯖にいないのにロールついてる
                notifyConnection(member, "SabamisoPlayers役職剥奪", "SabamisoServerのメンバーでないため、SabamisoPlayers役職を剥奪しました。", Color.getHSBColor(144, 33, 93));
                if (!dryRun) guild.removeRoleFromMember(member, Roles.SabamisoPlayers.role).queue();
            }

            // toro
            if (isTOROMember && !isTOROPlayers) {
                // 鯖にいるけどロールついてない
                notifyConnection(member, "TOROPlayers役職付与", "TOROServerのメンバーであるため、TOROPlayers役職を付与しました。", Color.BLACK);
                if (!dryRun) guild.addRoleToMember(member, Roles.TOROPlayers.role).queue();
            }

            if (!isTOROMember && isTOROPlayers) {
                // 鯖にいないのにロールついてる
                notifyConnection(member, "TOROPlayers役職剥奪", "TOROServerのメンバーでないため、TOROPlayers役職を剥奪しました。", Color.BLACK);
                if (!dryRun) guild.removeRoleFromMember(member, Roles.TOROPlayers.role).queue();
            }
        }
    }

    private List<GuildMember> getGuildMember(String guildId) {
        try {
            String output = getGuildMembers(guildId);
            if (output == null) {
                logger.warn("getGuildMember(%s): output == null".formatted(guildId));
                return null;
            }
            JSONArray array = new JSONArray(output);
            List<GuildMember> members = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                try {
                    members.add(new GuildMember(array.getJSONObject(i)));
                } catch (JSONException e) {
                    logger.warn(array.getJSONObject(i).toString(), e);
                }
            }
            return members;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static class GuildMember {
        List<String> roles;
        String nick;
        ZonedDateTime joined_at;
        String user_id;
        String user_name;
        String user_avatar;
        String user_discriminator;
        int user_public_flags;

        private GuildMember(JSONObject object) {
            this.roles = object.getJSONArray("roles")
                .toList()
                .stream()
                .map(Object::toString)
                .collect(Collectors.toList());
            this.nick = object.optString("nick");
            this.joined_at = ZonedDateTime.parse(object.getString("joined_at"), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            this.user_id = object.getJSONObject("user").getString("id");
            this.user_name = object.getJSONObject("user").getString("username");
            this.user_avatar = object.getJSONObject("user").optString("avatar");
            this.user_discriminator = object.getJSONObject("user").getString("discriminator");
            this.user_public_flags = object.getJSONObject("user").getInt("public_flags");
        }
    }

    private String getGuildMembers(String guild_id) throws IOException {
        File tmp = File.createTempFile("mem", ".json");
        Process p;
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(Arrays.asList("python3", "main.py", "--guild-id", guild_id, "--output", tmp.getAbsolutePath()));
            builder.redirectErrorStream(true);
            builder.directory(new File("external_scripts/getguildmembers/"));
            p = builder.start();
            boolean bool = p.waitFor(3, TimeUnit.MINUTES);
            if (!bool) {
                return null;
            }
        } catch (InterruptedException e) {
            return null;
        }
        String output = Files.readString(tmp.toPath());
        boolean bool = tmp.delete();
        if (!bool) logger.warn("delete temp file failed");
        return output;
    }

    private void notifyConnection(Member member, String title, String description, Color color) {
        TextChannel channel = Main.getJDA().getTextChannelById(597772663770972162L);

        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(color)
            .setAuthor(member.getUser().getAsTag(), "https://discord.com/users/" + member.getId(), member.getUser().getEffectiveAvatarUrl());
        if (dryRun) embed.setFooter("DRY-RUN MODE");

        channel.sendMessageEmbeds(embed.build()).queue();
    }

    public enum Roles {
        FruitPlayers(598135930347323396L, 239487519488475136L, null),
        SabamisoPlayers(598136046743715846L, 334770123220975628L, null),
        TOROPlayers(894050834638331944L, 337838758441517057L, null),
        Unknown(null, null, null);

        private final Long id;
        private final Long guild_id;
        private Role role;

        Roles(Long id, Long guild_id, Role role) {
            this.id = id;
            this.guild_id = guild_id;
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
