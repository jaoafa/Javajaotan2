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

package com.jaoafa.javajaotan2.tasks;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jaoafa.javajaotan2.Main;
import com.jaoafa.javajaotan2.lib.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * - Minecraft-Discord Connectがなされていて、MinecraftConnected役職か付与されていない利用者に対して、MinecraftConnected役職を付与する
 * - Minecraft-Discord Connectがなされておらず、MinecraftConnected役職か付与されている利用者に対して、MinecraftConnected役職を剥奪する
 * - MinecraftConnected役職がついている場合、Verified, Regularの役職に応じて役職を付与する
 * - MinecraftConnected役職がついていない場合、Verified, Community Regular, Regular役職を剥奪する
 * - Nitroの場合(gif画像がアイコンの場合)、Nitrotanを付与する
 * - Nitrotan役職がついていて、GIFアイコンでなく、アニメーション/外部絵文字の最終メッセージ送信日時が1週間前の場合、Nitrotan役職を剥奪する
 */
public class Task_PermSync implements Job {
    final boolean dryRun;
    Logger logger;
    Connection conn;
    TextChannel Channel_General;
    JSONObject nitrotan = new JSONObject();

    public Task_PermSync() {
        this.dryRun = false;
    }

    public Task_PermSync(boolean dryRun) {
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

        List<DiscordMinecraftLink> connections;
        try {
            connections = DiscordMinecraftLink.getAllForMinecraft();
        } catch (SQLException e) {
            e.printStackTrace();

            if (JavajaotanData.getRollbar() != null) {
                JavajaotanData.getRollbar().error(e);
            }
            return;
        }

        Channel_General = Channels.general.getChannel();
        if (Channel_General == null) {
            logger.warn("Channel_General == null");
            return;
        }

        Path path = Path.of("nitrotan.json");
        try {
            if (Files.exists(path)) {
                nitrotan = new JSONObject(Files.readString(path));
            }
        } catch (IOException e) {
            e.printStackTrace();

            if (JavajaotanData.getRollbar() != null) {
                JavajaotanData.getRollbar().error(e);
            }
        }

        ExecutorService service = Executors.newFixedThreadPool(10,
            new ThreadFactoryBuilder().setNameFormat(getClass().getSimpleName() + "-%d").build());
        guild.loadMembers()
            .onSuccess(members ->
                {
                    logger.info("loadMember(): %s".formatted(members.size()));
                    members.forEach(member ->
                        service.execute(new RunPermSync(connections, member.hasTimeJoined() ? member : guild.retrieveMember(member.getUser()).complete()))
                    );
                }
            );
    }

    private Timestamp getLeastLogin(UUID uuid) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM login WHERE uuid = ? AND login_success = ? ORDER BY id DESC LIMIT 1")) {

            stmt.setString(1, uuid.toString());
            stmt.setBoolean(2, true);

            ResultSet result = stmt.executeQuery();

            if (!result.next()) return null;
            else return result.getTimestamp("date");
        } catch (SQLException e) {

            if (JavajaotanData.getRollbar() != null) {
                JavajaotanData.getRollbar().error(e);
            }
            return null;
        }
    }

    class RunPermSync implements Runnable {
        final List<DiscordMinecraftLink> connections;
        final Member member;

        public RunPermSync(List<DiscordMinecraftLink> connections, Member member) {
            this.connections = connections;
            this.member = member;
        }

        @Override
        public void run() {
            try {
                runPermSync();
            } catch (Exception e) {
                logger.error("Error in RunPermSync", e);

                if (JavajaotanData.getRollbar() != null) {
                    JavajaotanData.getRollbar().error(e);
                }
            }
        }

        void runPermSync() {
            Guild guild = member.getGuild();
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
            DiscordMinecraftLink dml = connections
                .stream()
                .filter(c -> c.getDiscordId().equals(member.getId()))
                .findFirst()
                .orElse(null);

            boolean isRegular = JavajaotanLibrary.isGrantedRole(member, Roles.Regular.getRole());
            boolean isCommunityRegular = JavajaotanLibrary.isGrantedRole(member, Roles.CommunityRegular.getRole());
            boolean isVerified = JavajaotanLibrary.isGrantedRole(member, Roles.Verified.getRole());
            boolean isMinecraftConnected = JavajaotanLibrary.isGrantedRole(member, Roles.MinecraftConnected.getRole());
            boolean isSubAccount = JavajaotanLibrary.isGrantedRole(member, Roles.SubAccount.getRole());
            boolean isNitrotan = JavajaotanLibrary.isGrantedRole(member, Roles.Nitrotan.getRole());

            // Nitroの場合(gif画像がアイコンの場合)、Nitrotanを付与する
            if (member.getUser().getEffectiveAvatarUrl().endsWith(".gif") && !isNitrotan) {
                notifyConnection(member, "Nitrotan役職付与", "アイコンがGIF画像だったので、Nitrotan役職を付与しました。", Color.LIGHT_GRAY, dml);
                if (!dryRun) guild.addRoleToMember(member, Roles.Nitrotan.getRole()).queue();
                isNitrotan = true;
            }
            // Nitrotan役職がついていて、GIFアイコンでなく、アニメーション/外部絵文字の最終メッセージ送信日時が1週間前の場合、Nitrotan役職を剥奪する
            // アニメーション/外部絵文字の最終メッセージ送信日時が何ミリ秒前か？
            long lastNitroAgo = nitrotan.has(member.getId()) ? new Date().getTime() - nitrotan.getLong(member.getId()) : -1;
            // 7日 * 24時間 * 60分 * 60秒 * 1000秒
            if (isNitrotan && !member.getUser().getEffectiveAvatarUrl().endsWith(".gif") && (lastNitroAgo == -1 || lastNitroAgo > 7 * 24 * 60 * 60 * 1000)) {
                notifyConnection(member, "Nitrotan役職剥奪", "アイコンがGIF画像ではなく、またアニメーション/外部絵文字を使用したメッセージが1週間以上前だったためNitrotan役職を剥奪しました。", Color.LIGHT_GRAY, dml);
                if (!dryRun) guild.removeRoleFromMember(member, Roles.Nitrotan.getRole()).queue();
            }

            String minecraftId;
            UUID uuid = null;
            if (dml != null) {
                minecraftId = dml.getMinecraftName();
                uuid = dml.getMinecraftUUID();
                logger.info("[%s] Minecraft link: %s (%s)".formatted(
                    member.getUser().getAsTag(),
                    minecraftId,
                    uuid
                ));
            } else {
                logger.info("[%s] Minecraft link: NOT LINKED".formatted(
                    member.getUser().getAsTag()
                ));
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime joinedTime = member.getTimeJoined().atZoneSameInstant(ZoneId.of("Asia/Tokyo")).toLocalDateTime();
            long joinDays = ChronoUnit.DAYS.between(joinedTime, now);
            logger.info("[%s] joinDays: %s days".formatted(member.getUser().getAsTag(), joinDays));

            // サブアカウントの場合、10分チェックとかのみ (Minecraft linkされている場合は除外)
            if (isSubAccount && !isMinecraftConnected) return;

            //giveRole,description,isMinecraftConnected
            BiFunction<Boolean, String, Boolean> doMinecraftConnectedManage = (giveRole, description) -> {
                notifyConnection(member, "MinecraftConnected役職" + (giveRole ? "付与" : "剥奪"), description, Color.BLUE, dml);
                if (!dryRun) {
                    if (giveRole) {
                        guild.addRoleToMember(member, Roles.MinecraftConnected.getRole()).queue();
                    } else {
                        guild.removeRoleFromMember(member, Roles.MinecraftConnected.getRole()).queue();
                    }
                }
                return true;
            };

            if (dml != null && dml.isLinked() && !isMinecraftConnected) {
                // Minecraft-Discord Connectがなされていて、MinecraftConnected役職か付与されていない利用者に対して、MinecraftConnected役職を付与する
                isMinecraftConnected = doMinecraftConnectedManage.apply(true, "linkがされていたため、MinecraftConnected役職を付与しました。");
            }
            if ((dml == null || !dml.isLinked()) && isMinecraftConnected) {
                // Minecraft-Discord Connectがなされておらず、MinecraftConnected役職か付与されている利用者に対して、MinecraftConnected役職を剥奪する
                isMinecraftConnected = doMinecraftConnectedManage.apply(false, "linkがされていなかったため、MinecraftConnected役職を剥奪しました。");
            }

            if (isMinecraftConnected && uuid != null) {
                // MinecraftConnected役職がついている場合、Verified, Regularの役職に応じて役職を付与する

                MinecraftPermGroup group = null;
                try {
                    group = new MinecraftPermGroup(uuid);
                    logger.info("[%s] Minecraft Group: %s".formatted(
                        member.getUser().getAsTag(), group.getGroup().name()));
                    if (!group.isFound()) {
                        group = null;
                    }
                } catch (SQLException e) {
                    logger.error("[%s] Minecraft permission group error".formatted(member.getUser().getAsTag()), e);

                    if (JavajaotanData.getRollbar() != null) {
                        JavajaotanData.getRollbar().error(e);
                    }
                }
                if (!isVerified && group != null && group.getGroup() == MinecraftPermGroup.Group.VERIFIED) {
                    notifyConnection(member, "Verified役職付与", "Minecraft鯖内の権限に基づき、Verified役職を付与しました。", Color.CYAN, dml);
                    if (!dryRun) guild.addRoleToMember(member, Roles.Verified.getRole()).queue();
                }
                if (!isRegular && group != null && group.getGroup() == MinecraftPermGroup.Group.REGULAR) {
                    notifyConnection(member, "Regular役職付与", "Minecraft鯖内の権限に基づき、Regular役職を付与しました。", Color.CYAN, dml);
                    if (!dryRun) guild.addRoleToMember(member, Roles.Regular.getRole()).queue();
                }
            } else {
                // MinecraftConnected役職がついていない場合、Verified, Community Regular, Regular役職を剥奪する
                List<Roles> roles = new ArrayList<>();

                if (isVerified) roles.add(Roles.Verified);
                if (isCommunityRegular) roles.add(Roles.CommunityRegular);
                if (isRegular) roles.add(Roles.Regular);

                for (Roles role : roles) {
                    notifyConnection(member, "%s役職剥奪".formatted(role.name()), "linkが解除されているため、%s役職を剥奪しました。".formatted(role.name()), Color.GREEN, dml);
                    if (!dryRun) guild.removeRoleFromMember(member, role.getRole()).queue();
                }
            }
        }

        private void notifyConnection(Member member, String title, String description, Color color, DiscordMinecraftLink dml) {
            TextChannel channel = Main.getJDA().getTextChannelById(891021520099500082L);

            if (channel == null) return;

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setAuthor(member.getUser().getAsTag(), "https://discord.com/users/" + member.getId(), member.getUser().getEffectiveAvatarUrl());
            if (dml != null) {
                embed.addField("MinecraftDiscordConnection", "[%s](https://users.jaoafa.com/%s)".formatted(dml.getMinecraftName(), dml.getMinecraftUUID().toString()), false);
            }
            if (dryRun) embed.setFooter("DRY-RUN MODE");

            channel.sendMessageEmbeds(embed.build()).queue();
        }
    }
}
