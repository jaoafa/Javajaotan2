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
import com.jaoafa.javajaotan2.lib.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
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
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
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

        List<RunMemberOrganize.MinecraftDiscordConnection> connections = getConnections();

        if (connections == null) {
            logger.warn("connections == null");
            return;
        }

        Roles.setGuildAndRole(guild);

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

    private List<RunMemberOrganize.MinecraftDiscordConnection> getConnections() {
        List<RunMemberOrganize.MinecraftDiscordConnection> connections = new ArrayList<>();
        List<UUID> inserted = new ArrayList<>();
        try {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM discordlink ORDER BY id DESC")) {
                try (ResultSet res = stmt.executeQuery()) {
                    while (res.next()) {
                        if (inserted.contains(UUID.fromString(res.getString("uuid")))) continue;
                        connections.add(new RunMemberOrganize.MinecraftDiscordConnection(
                            res.getString("player"),
                            UUID.fromString(res.getString("uuid")),
                            res.getString("disid"),
                            res.getString("discriminator"),
                            getLeastLogin(UUID.fromString(res.getString("uuid"))),
                            res.getTimestamp("expired_date"),
                            res.getTimestamp("dead_at"),
                            res.getBoolean("disabled")
                        ));
                        inserted.add(UUID.fromString(res.getString("uuid")));
                    }
                }
            }
            return connections;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Timestamp getLeastLogin(UUID uuid) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM login WHERE uuid = ? AND login_success = ? ORDER BY id DESC LIMIT 1")) {

            stmt.setString(1, uuid.toString());
            stmt.setBoolean(2, true);

            ResultSet result = stmt.executeQuery();

            if (!result.next()) return null;
            else return result.getTimestamp("date");
        } catch (SQLException e) {
            return null;
        }
    }

    class RunMemberOrganize implements Runnable {
        final List<MinecraftDiscordConnection> connections;
        final Member member;

        public RunMemberOrganize(List<MinecraftDiscordConnection> connections, Member member) {
            this.connections = connections;
            this.member = member;
        }

        @Override
        public void run() {
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
            MinecraftDiscordConnection mdc = connections
                .stream()
                .filter(c -> c.disid().equals(member.getId()))
                .findFirst()
                .orElse(null);

            boolean isMinecraftConnected = JavajaotanLibrary.isGrantedRole(member, Roles.MinecraftConnected.role);
            boolean isNeedSupport = JavajaotanLibrary.isGrantedRole(member, Roles.NeedSupport.role);
            boolean isSubAccount = JavajaotanLibrary.isGrantedRole(member, Roles.SubAccount.role);

            String minecraftId;
            UUID uuid = null;
            if (mdc != null) {
                minecraftId = mdc.player();
                uuid = mdc.uuid();
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
            LocalDateTime loginDate = mdc != null ? mdc.loginDate().toLocalDateTime() : null;
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
                    Channel_General.sendMessage(new MessageBuilder()
                        .setEmbeds(embed.build())
                        .setContent("<@%s> (%s)".formatted(member.getId(), member.getUser().getAsTag()))
                        .build()
                    ).queue();
                }
                return true;
            };

            if (!isMinecraftConnected && !isSubAccount && !isNeedSupport && mdc == null && joinDays >= 7) {
                // 参加してから1週間以内にlink・サブアカウント登録・サポートへの問い合わせがない場合はキックする
                doReturn = doKick.apply("1weekキック", "1週間(7日)以上link・サブアカウント登録・サポートへの問い合わせがなかったため、キックしました。");
            } else if (!isMinecraftConnected && !isSubAccount && isNeedSupport && mdc == null && joinDays >= 21) {
                // 参加してから3週間後にサポート問い合わせのみの場合はキックする
                doReturn = doKick.apply("3weeksキック (NeedSupport)", "3週間(21日)以上link・サブアカウント登録がなかったため、キックしました。");
            }

            if (doReturn) return;

            SubAccount subAccount = new SubAccount(member);

            if (isSubAccount && !subAccount.isSubAccount()) {
                // SubAccount役職がついているのにサブアカウントではない場合、SubAccount役職を剥奪する
                notifyConnection(member, "SubAccount役職剥奪", "SubAccount役職が付与されていましたが、サブアカウント登録がなされていないため剥奪しました。", Color.RED, mdc);
                if (!dryRun) guild.removeRoleFromMember(member, Roles.SubAccount.role).queue();
                isSubAccount = false;
            }


            if (!isSubAccount && subAccount.isSubAccount()) {
                // サブアカウントなのにSubAccount役職がついていない場合、SubAccount役職を付与する
                notifyConnection(member, "SubAccount役職付与", "サブアカウント登録がされていましたが、SubAccount役職が付与されていなかったため付与しました。", Color.RED, mdc);
                if (!dryRun) guild.addRoleToMember(member, Roles.SubAccount.role).queue();
                isSubAccount = true;
            }

            if (isSubAccount) {
                // メインアカウントが1か月以上前に退出しているのにも関わらず、SubAccount役職がついている利用者から本役職を外す。
                MinecraftDiscordConnection subMdc = connections
                    .stream()
                    .filter(c -> c.disid().equals(subAccount.getMainAccount().getUser().getId()))
                    .findFirst()
                    .orElse(null);

                //description,isSubAccount
                Function<String, Boolean> doSubAccountRemove = description -> {
                    notifyConnection(member, "SubAccount役職剥奪", description, Color.YELLOW, mdc);
                    if (!dryRun) {
                        subAccount.removeMainAccount();
                        guild.addRoleToMember(member, Roles.SubAccount.role).queue();
                    }
                    return false;
                };

                if (subMdc == null) {
                    //取得失敗
                    isSubAccount = doSubAccountRemove.apply("メインアカウントの情報が取得できなかったため、剥奪しました。");
                } else if (subMdc.disabled()) {
                    long deadDays = ChronoUnit.DAYS.between(subMdc.dead_at().toLocalDateTime(), now);
                    if (deadDays >= 30) {
                        // disabled & 30日経過
                        isSubAccount = doSubAccountRemove.apply("メインアカウントが1か月(30日)以上前にlinkを切断しているため、剥奪しました。");
                    }
                }
            }

            // サブアカウントの場合、10分チェックとかのみ (Minecraft linkされている場合は除外)
            if (isSubAccount && !isMinecraftConnected) return;

            if (isMinecraftConnected && uuid != null) {
                Timestamp checkTS = getMaxTimestamp(mdc.loginDate(), mdc.expired_date());
                long checkDays = loginDate != null ? ChronoUnit.DAYS.between(checkTS.toLocalDateTime(), now) : -1;
                logger.info("[%s] checkDays: %s".formatted(member.getUser().getAsTag(), checkDays));
                // 最終ログインから2ヶ月(60日)が経過している場合、警告リプライを#generalで送信する
                if (checkDays >= 60 && !notified.isNotified(Notified.NotifiedType.MONTH2)) {
                    notifyConnection(member, "2か月経過", "最終ログインから2か月が経過したため、#generalで通知します。", Color.MAGENTA, mdc);
                    if (!dryRun) {
                        EmbedBuilder embed = new EmbedBuilder()
                            .setTitle(":exclamation:最終ログインから2か月経過のお知らせ", "https://users.jaoafa.com/%s".formatted(mdc.uuid().toString()))
                            .setDescription("""
                                あなたのDiscordアカウントに接続されているMinecraftアカウント「`%s`」が**最終ログインから2ヶ月経過**致しました。
                                **サーバルール及び個別規約により、3ヶ月を経過すると建築物や自治体の所有権がなくなり、運営によって撤去・移動ができる**ようになり、またMinecraftアカウントとの連携が自動的に解除されます。
                                本日から1ヶ月以内にjao Minecraft Serverにログインがなされない場合、上記のような対応がなされる場合がございますのでご注意ください。""".formatted(
                                mdc.player()
                            ))
                            .setColor(Color.YELLOW)
                            .setFooter("最終ログイン日時: %s".formatted(checkTS.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        Channel_General.sendMessage(new MessageBuilder()
                            .setEmbeds(embed.build())
                            .setContent("<@%s>".formatted(member.getId()))
                            .build()
                        ).queue();
                    }
                    notified.setNotified(Notified.NotifiedType.MONTH2);
                }

                // 最終ログインから3ヶ月が経過している場合、linkをdisabledにし、MinecraftConnected権限を剥奪する
                if (checkDays >= 90) {
                    notifyConnection(member, "3monthリンク切断", "最終ログインから3か月が経過したため、linkを切断し、役職を剥奪します。", Color.ORANGE, mdc);
                    if (!dryRun) {
                        EmbedBuilder embed = new EmbedBuilder()
                            .setTitle(":bangbang:最終ログインから3か月経過のお知らせ", "https://users.jaoafa.com/%s".formatted(mdc.uuid().toString()))
                            .setDescription("""
                                あなたのDiscordアカウントに接続されていたMinecraftアカウント「`%s`」が最終ログインから3ヶ月経過致しました。
                                サーバルール及び個別規約により、建築物や自治体の所有権がなくなり、Minecraftアカウントとの接続が自動的に切断されました。""".formatted(
                                mdc.player()
                            ))
                            .setColor(Color.RED)
                            .setFooter("最終ログイン日時: %s".formatted(checkTS.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        Channel_General.sendMessage(new MessageBuilder()
                            .setEmbeds(embed.build())
                            .setContent("<@%s>".formatted(member.getId()))
                            .build()
                        ).queue();
                        disableLink(mdc, uuid);
                        guild.removeRoleFromMember(member, Roles.MinecraftConnected.role).queue();
                    }
                    isMinecraftConnected = false;
                }
                if (isMinecraftConnected && isSubAccount) {
                    if (!dryRun) {
                        guild.removeRoleFromMember(member, Roles.SubAccount.role).queue();
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

        private void notifyConnection(Member member, String title, String description, Color color, MinecraftDiscordConnection mdc) {
            TextChannel channel = Main.getJDA().getTextChannelById(891021520099500082L);

            if (channel == null) return;

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setAuthor(member.getUser().getAsTag(), "https://discord.com/users/" + member.getId(), member.getUser().getEffectiveAvatarUrl());
            if (mdc != null) {
                embed.addField("MinecraftDiscordConnection", "[%s](https://users.jaoafa.com/%s)".formatted(mdc.player(), mdc.uuid().toString()), false);
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

        private void disableLink(MinecraftDiscordConnection mdc, UUID uuid) {
            PermissionGroup group = getPermissionGroup(uuid);
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE discordlink SET disabled = ?, dead_perm = ?, dead_at = CURRENT_TIMESTAMP WHERE uuid = ? AND disabled = ?")) {
                stmt.setBoolean(1, true);
                stmt.setString(2, group != null ? group.name() : null);
                stmt.setString(3, uuid.toString());
                stmt.setBoolean(4, false);
                stmt.execute();
            } catch (SQLException e) {
                logger.warn("disableLink(%s): failed".formatted(mdc.player + "#" + mdc.uuid), e);
            }
        }

        private PermissionGroup getPermissionGroup(UUID uuid) {
            try {
                try (PreparedStatement statement = conn.prepareStatement("SELECT * FROM permissions WHERE uuid = ?")) {
                    statement.setString(1, uuid.toString());
                    try (ResultSet res = statement.executeQuery()) {
                        if (!res.next()) {
                            return null;
                        }
                        String permissionGroup = res.getString("permission");
                        return Arrays
                            .stream(PermissionGroup.values())
                            .filter(p -> p.name().equalsIgnoreCase(permissionGroup))
                            .findFirst()
                            .orElse(PermissionGroup.UNKNOWN);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }

        enum PermissionGroup {
            ADMIN,
            MODERATOR,
            REGULAR,
            VERIFIED,
            DEFAULT,
            UNKNOWN
        }

        record MinecraftDiscordConnection(String player, UUID uuid, String disid, String discriminator,
                                          Timestamp loginDate,
                                          Timestamp expired_date, Timestamp dead_at, boolean disabled) {

        }

        class Notified {
            final Path path = Path.of("permsync-notified.json");
            final Member member;
            final String memberId;

            Notified(Member member) {
                this.member = member;
                this.memberId = member.getId();
            }

            private boolean isNotified(NotifiedType type) {
                JSONObject object = load();
                return object.has(memberId) && object.getJSONArray(memberId).toList().contains(type.name());
            }

            private void setNotified(NotifiedType type) {
                JSONObject object = load();
                JSONArray userObject = object.has(memberId) ? object.getJSONArray(memberId) : new JSONArray();
                userObject.put(type.name());
                object.put(memberId, userObject);
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