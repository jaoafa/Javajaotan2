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
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Minecraft-Discord Connect(/link) に基づき、以下の利用者整理処理を行う
 * ここでの1ヶ月は30日とする。
 * (dryRun引数がTrueの場合は対象のメンバーのみを抽出)
 * <p>
 * - 参加してから1週間以内にlink・サブアカウント登録・サポートへの問い合わせがない場合はキックする
 * - 参加してから3週間後にサポート問い合わせのみの場合はキックする
 * - linkされているのに、Guildにいない利用者のlinkを解除する
 * - メインアカウントが1か月以上前に退出しているのにも関わらず、SubAccount役職がついている利用者から本役職を外す。
 * - SubAccount役職がついているのにサブアカウントではない場合、SubAccount役職を剥奪する
 * - サブアカウントなのにSubAccount役職がついていない場合、SubAccount役職を付与する
 * <p>
 */
public class Task_MemberOrganize implements Job {
    final boolean dryRun;
    Logger logger;
    Connection conn;

    TextChannel Channel_General;

    public Task_MemberOrganize() {
        this.dryRun = false;
    }

    public Task_MemberOrganize(boolean dryRun) {
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

        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        if (manager == null) {
            return;
        }
        try {
            conn = manager.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();

            if (JavajaotanData.getRollbar() != null) {
                JavajaotanData.getRollbar().error(e);
            }
            return;
        }

        List<DiscordMinecraftLink> connections;
        try {
            connections = DiscordMinecraftLink.getAllForDiscord();
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

        ExecutorService service = Executors.newFixedThreadPool(10,
            new ThreadFactoryBuilder().setNameFormat(getClass().getSimpleName() + "-%d").build());
        guild.loadMembers()
            .onSuccess(members ->
                {
                    logger.info("loadMember(): %s".formatted(members.size()));
                    members.forEach(member ->
                        service.execute(new RunMemberOrganize(connections, member.hasTimeJoined() ? member : guild.retrieveMember(member.getUser()).complete()))
                    );
                }
            );
    }

    class RunMemberOrganize implements Runnable {
        final List<DiscordMinecraftLink> connections;
        final Member member;

        public RunMemberOrganize(List<DiscordMinecraftLink> connections, Member member) {
            this.connections = connections;
            this.member = member;
        }

        @Override
        public void run() {
            try {
                organizeMember();
            } catch (Exception e) {
                logger.error("Error in RunMemberOrganize", e);

                if (JavajaotanData.getRollbar() != null) {
                    JavajaotanData.getRollbar().error(e);
                }
            }
        }

        void organizeMember() {
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

            boolean isMinecraftConnected = JavajaotanLibrary.isGrantedRole(member, Roles.MinecraftConnected.getRole());
            boolean isNeedSupport = JavajaotanLibrary.isGrantedRole(member, Roles.NeedSupport.getRole());
            boolean isSubAccount = JavajaotanLibrary.isGrantedRole(member, Roles.SubAccount.getRole());

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

            boolean doReturn = false;
            BiFunction<String, String, Boolean> doKick = (title, description) -> {
                boolean kicked = kickDiscord(member, title, description);
                if (kicked) {
                    EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(":wave:自動キックのお知らせ (%s)".formatted(title), "https://discord.com/users/%s".formatted(member.getId()))
                        .setDescription(description)
                        .setColor(Color.RED)
                        .setFooter("サーバ参加日時: %s".formatted(joinedTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                    Channel_General.sendMessage(new MessageCreateBuilder()
                        .setEmbeds(embed.build())
                        .setContent("<@%s> (%s)".formatted(member.getId(), member.getUser().getAsTag()))
                        .build()
                    ).queue();
                }
                return true;
            };

            if (!isMinecraftConnected && !isSubAccount && !isNeedSupport && dml == null && joinDays >= 7) {
                // 参加してから1週間以内にlink・サブアカウント登録・サポートへの問い合わせがない場合はキックする
                doReturn = doKick.apply("1weekキック", "1週間(7日)以上link・サブアカウント登録・サポートへの問い合わせがなかったため、キックしました。");
            } else if (!isMinecraftConnected && !isSubAccount && isNeedSupport && dml == null && joinDays >= 21) {
                // 参加してから3週間後にサポート問い合わせのみの場合はキックする
                doReturn = doKick.apply("3weeksキック (NeedSupport)", "3週間(21日)以上link・サブアカウント登録がなかったため、キックしました。");
            }

            if (doReturn) return;

            SubAccount subAccount = new SubAccount(member);

            if (isSubAccount && !subAccount.isSubAccount()) {
                // SubAccount役職がついているのにサブアカウントではない場合、SubAccount役職を剥奪する
                notifyConnection(member, "SubAccount役職剥奪", "SubAccount役職が付与されていましたが、サブアカウント登録がなされていないため剥奪しました。", Color.RED, dml);
                if (!dryRun) guild.removeRoleFromMember(member, Roles.SubAccount.getRole()).queue();
                isSubAccount = false;
            }


            if (!isSubAccount && subAccount.isSubAccount()) {
                // サブアカウントなのにSubAccount役職がついていない場合、SubAccount役職を付与する
                notifyConnection(member, "SubAccount役職付与", "サブアカウント登録がされていましたが、SubAccount役職が付与されていなかったため付与しました。", Color.RED, dml);
                if (!dryRun) guild.addRoleToMember(member, Roles.SubAccount.getRole()).queue();
                isSubAccount = true;
            }

            if (isSubAccount) {
                // メインアカウントが1か月以上前に退出しているのにも関わらず、SubAccount役職がついている利用者から本役職を外す。
                DiscordMinecraftLink subDml = connections
                    .stream()
                    .filter(c -> c.getDiscordId().equals(subAccount.getMainAccount().getUser().getId()))
                    .findFirst()
                    .orElse(null);

                //description,isSubAccount
                Function<String, Boolean> doSubAccountRemove = description -> {
                    notifyConnection(member, "SubAccount役職剥奪", description, Color.YELLOW, dml);
                    if (!dryRun) {
                        subAccount.removeMainAccount();
                        guild.addRoleToMember(member, Roles.SubAccount.getRole()).queue();
                    }
                    return false;
                };

                if (subDml == null) {
                    //取得失敗
                    isSubAccount = doSubAccountRemove.apply("メインアカウントの情報が取得できなかったため、剥奪しました。");
                } else if (!subDml.isLinked()) {
                    long deadDays = ChronoUnit.DAYS.between(subDml.getDisconnectAt().toLocalDateTime(), now);
                    if (deadDays >= 30) {
                        // disabled & 30日経過
                        isSubAccount = doSubAccountRemove.apply("メインアカウントが1か月(30日)以上前にlinkを切断しているため、剥奪しました。");
                    }
                }
            }

            // サブアカウントの場合、10分チェックとかのみ (Minecraft linkされている場合は除外)
            if (isSubAccount && !isMinecraftConnected) return;

            if (isMinecraftConnected && uuid != null & isSubAccount && !dryRun) {
                guild.removeRoleFromMember(member, Roles.SubAccount.getRole()).queue();
            }
        }

        private void notifyConnection(Member member, String title, String description, Color color, DiscordMinecraftLink mdc) {
            TextChannel channel = Main.getJDA().getTextChannelById(891021520099500082L);

            if (channel == null) return;

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setAuthor(member.getUser().getAsTag(), "https://discord.com/users/" + member.getId(), member.getUser().getEffectiveAvatarUrl());
            if (mdc != null) {
                embed.addField("MinecraftDiscordConnection", "[%s](https://users.jaoafa.com/%s)".formatted(mdc.getMinecraftName(), mdc.getMinecraftUUID().toString()), false);
            }
            if (dryRun) embed.setFooter("DRY-RUN MODE");

            channel.sendMessageEmbeds(embed.build()).queue();
        }

        private boolean kickDiscord(Member member, String title, String description) {
            Path path = Path.of("pre-permsync.json");
            JSONObject object = new JSONObject();
            if (Files.exists(path)) {
                try {
                    object = new JSONObject(Files.readString(path));
                } catch (IOException e) {
                    logger.warn("grantDiscordPerm json load failed.", e);

                    if (JavajaotanData.getRollbar() != null) {
                        JavajaotanData.getRollbar().error(e);
                    }
                    return false;
                }
            }

            if (!object.has("kick")) object.put("kick", new JSONArray());

            JSONArray kicks = object.getJSONArray("kick");

            boolean kicked = false;
            if (kicks.toList().contains(member.getId()) && !dryRun) {
                // 処理
                notifyConnection(member, "[PROCESS] " + title, description, Color.PINK, null);
                member.getGuild().kick(member).queue();
                kicks.remove(kicks.toList().indexOf(member.getId()));
                kicked = true;
            } else {
                // 動作予告
                notifyConnection(member, "[PRE] " + title, "次回処理時、本動作が実施されます。", Color.PINK, null);
                kicks.put(member.getId());
            }

            object.put("kick", kicks);
            try {
                Files.writeString(path, object.toString());
            } catch (IOException e) {
                logger.warn("grantDiscordPerm json save failed.", e);

                if (JavajaotanData.getRollbar() != null) {
                    JavajaotanData.getRollbar().error(e);
                }
            }
            return kicked;
        }
    }
}