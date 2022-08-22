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
import net.dv8tion.jda.api.entities.TextChannel;
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
import java.sql.Timestamp;
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
 * - 最終ログインから2ヶ月が経過している場合、警告リプライを#generalで送信する
 * - 最終ログインから3ヶ月が経過している場合、linkをdisabledにし、MinecraftConnected権限を剥奪する
 * - メインアカウントが1か月以上前に退出しているのにも関わらず、SubAccount役職がついている利用者から本役職を外す。
 * - SubAccount役職がついているのにサブアカウントではない場合、SubAccount役職を剥奪する
 * - サブアカウントなのにSubAccount役職がついていない場合、SubAccount役職を付与する
 * <p>
 * - 解除関連処理時、ExpiredDateが設定されている場合は、期限日は最終ログインから3ヶ月後かExpiredDateのいずれか遅い方を使用する
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
            return;
        }

        List<DiscordMinecraftLink> connections;
        try {
            connections = DiscordMinecraftLink.getAllForDiscord();
        } catch (SQLException e) {
            e.printStackTrace();
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
            Notified notified = new Notified(member);
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
            LocalDateTime loginDate = dml != null ? dml.getLastLogin().toLocalDateTime() : null;
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

            if (isMinecraftConnected && uuid != null) {
                Timestamp expired_date = dml.getExpiredAt();
                if (expired_date != null) {
                    expired_date.setTime(expired_date.getTime() - (1000L * 60 * 60 * 24 * 90)); // 最終ログイン日時が期限日の90日前と仮定
                }
                logger.info("[%s] loginDate: %s".formatted(member.getUser().getAsTag(), loginDate));
                logger.info("[%s] expired_date: %s".formatted(member.getUser().getAsTag(), expired_date));
                Timestamp checkTS = getMaxTimestamp(dml.getLastLogin(), expired_date);
                boolean isExpiredDate = expired_date != null && checkTS.equals(expired_date);
                long checkDays = loginDate != null ? ChronoUnit.DAYS.between(checkTS.toLocalDateTime(), now) : -1;
                logger.info("[%s] checkDays: %s".formatted(member.getUser().getAsTag(), checkDays));

                if (checkDays < 60 && notified.is2MonthNotified()) {
                    notified.resetNotified();
                }

                // 最終ログインから2ヶ月(60日)が経過している場合、警告リプライを#generalで送信する
                if (checkDays >= 60 && checkDays < 90 && !notified.is2MonthNotified()) {
                    notifyConnection(member, "2か月経過", "最終ログインから2か月が経過したため、#generalで通知します。(isExpiredDate: " + isExpiredDate + ")", Color.MAGENTA, dml);
                    if (!dryRun) {
                        EmbedBuilder embed = new EmbedBuilder()
                            .setTitle(":exclamation:最終ログインから2か月経過のお知らせ", "https://users.jaoafa.com/%s".formatted(dml.getMinecraftUUID().toString()))
                            .setDescription("""
                                あなたのDiscordアカウントに接続されているMinecraftアカウント「`%s`」が**最終ログインから2ヶ月経過**致しました。
                                **サーバルール及び個別規約により、3ヶ月を経過すると建築物や自治体の所有権がなくなり、運営によって撤去・移動ができる**ようになり、またMinecraftアカウントとの連携が自動的に解除されます。
                                本日から1ヶ月以内にjao Minecraft Serverにログインがなされない場合、上記のような対応がなされる場合がございますのでご注意ください。
                                （自動連携解除延期措置が実施されている場合、このメッセージは期限日の1か月前をお知らせするものです）""".formatted(
                                dml.getMinecraftName()
                            ))
                            .setColor(Color.YELLOW)
                            .setFooter("最終ログイン日時: %s".formatted(checkTS.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        Channel_General.sendMessage(new MessageCreateBuilder()
                            .setEmbeds(embed.build())
                            .setContent("<@%s>".formatted(member.getId()))
                            .build()
                        ).queue();
                        notified.set2MonthNotified();
                    }
                }

                // 最終ログインから3ヶ月が経過している場合、linkをdisabledにし、MinecraftConnected権限を剥奪する
                if (checkDays >= 90) {
                    notifyConnection(member, "3monthリンク切断", "最終ログインから3か月が経過したため、linkを切断し、役職を剥奪します。(isExpiredDate: " + isExpiredDate + ")", Color.ORANGE, dml);
                    if (!dryRun) {
                        EmbedBuilder embed = new EmbedBuilder()
                            .setTitle(":bangbang:最終ログインから3か月経過のお知らせ", "https://users.jaoafa.com/%s".formatted(dml.getMinecraftUUID().toString()))
                            .setDescription("""
                                あなたのDiscordアカウントに接続されていたMinecraftアカウント「`%s`」が最終ログインから3ヶ月経過致しました。
                                サーバルール及び個別規約により、建築物や自治体の所有権がなくなり、Minecraftアカウントとの接続が自動的に切断されました。""".formatted(
                                dml.getMinecraftName()
                            ))
                            .setColor(Color.RED)
                            .setFooter("最終ログイン日時: %s".formatted(checkTS.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        Channel_General.sendMessage(new MessageCreateBuilder()
                            .setEmbeds(embed.build())
                            .setContent("<@%s>".formatted(member.getId()))
                            .build()
                        ).queue();
                        try {
                            dml.disconnect();
                        } catch (SQLException e) {
                            logger.warn("Failed to disconnect member: " + dml.getMinecraftUUID().toString(), e);
                        }
                        guild.removeRoleFromMember(member, Roles.MinecraftConnected.getRole()).queue();
                        notified.resetNotified();
                    }
                    isMinecraftConnected = false;
                }
                if (isMinecraftConnected && isSubAccount) {
                    if (!dryRun) {
                        guild.removeRoleFromMember(member, Roles.SubAccount.getRole()).queue();
                    }
                }
            }
        }

        Timestamp getMaxTimestamp(Timestamp a, Timestamp b) {
            if (a == null && b != null) return b;
            if (a != null && b == null) return a;
            if (a == null) return null;
            if (a.before(b)) {
                return b;
            } else if (b.before(a)) {
                return a;
            } else if (a.equals(b)) {
                return a;
            }
            return null;
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
            }
            return kicked;
        }

        class Notified {
            final Path path = Path.of("permsync-notified.json");
            final Member member;
            final String memberId;

            Notified(Member member) {
                this.member = member;
                this.memberId = member.getId();
            }

            private boolean is2MonthNotified() {
                JSONObject object = load();
                return object.has(memberId) && object.getJSONArray(memberId).toList().contains(NotifiedType.MONTH2.name());
            }

            private void set2MonthNotified() {
                JSONObject object = load();
                JSONArray userObject = object.has(memberId) ? object.getJSONArray(memberId) : new JSONArray();
                userObject.put(NotifiedType.MONTH2.name());
                object.put(memberId, userObject);
                try {
                    Files.writeString(path, object.toString());
                } catch (IOException e) {
                    logger.warn("Notified.setNotified is failed.", e);
                }
            }

            private void resetNotified() {
                JSONObject object = load();
                object.remove(memberId);
                try {
                    Files.writeString(path, object.toString());
                } catch (IOException e) {
                    logger.warn("Notified.setNotified is failed.", e);
                }
            }

            private JSONObject load() {
                JSONObject object = new JSONObject();
                if (Files.exists(path)) {
                    try {
                        object = new JSONObject(Files.readString(path));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return object;
            }

            enum NotifiedType {
                MONTH2
            }
        }
    }
}