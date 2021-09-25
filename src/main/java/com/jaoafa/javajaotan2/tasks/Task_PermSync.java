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

import com.jaoafa.javajaotan2.Main;
import com.jaoafa.javajaotan2.lib.JavajaotanData;
import com.jaoafa.javajaotan2.lib.MySQLDBManager;
import com.jaoafa.javajaotan2.lib.SubAccount;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Minecraft-Discord Connect(/link) に基づき、以下の利用者整理処理を行う
 * ここでの1ヶ月は30日とする。
 * (dryRun引数がTrueの場合は対象のメンバーのみを抽出)
 * <p>
 * - 参加してから10分以内に発言のないユーザーをキックする
 * - 参加してから1週間以内にlink・サブアカウント登録・サポートへの問い合わせがない場合はキックする
 * - 参加してから3週間後にサポート問い合わせのみの場合はキックする
 * - linkされているのに、Guildにいない利用者のlinkを解除する
 * - Minecraft-Discord Connectがなされていて、MinecraftConnected役職か付与されていない利用者に対して、MinecraftConnected役職を付与する
 * - Minecraft-Discord Connectがなされておらず、MinecraftConnected役職か付与されている利用者に対して、MinecraftConnected役職を剥奪する
 * - MinecraftConnected役職がついている場合、Verified, Regularの役職に応じて役職を付与する
 * - MinecraftConnected役職がついていない場合、Verified, Community Regular, Regular役職を剥奪する
 * - 最終ログインから2ヶ月が経過している場合、警告リプライを#generalで送信する
 * - 最終ログインから3ヶ月が経過している場合、linkをdisabledにし、MinecraftConnected権限を剥奪する
 * - メインアカウントが1か月以上前に退出しているのにも関わらず、SubAccount役職がついている利用者から本役職を外す。
 * <p>
 * - 解除関連処理時、ExpiredDateが設定されている場合は、期限日は最終ログインから3ヶ月後かExpiredDateのいずれか遅い方を使用する
 * <p>
 */
public class Task_PermSync implements Job {
    boolean dryRun;
    Logger logger;
    Connection conn;
    Role RegularRole;
    Role CommunityRegularRole;
    Role VerifiedRole;
    Role MinecraftConnectedRole;
    Role MailVerifiedRole;
    Role NeedSupportRole;
    Role SubAccountRole;
    TextChannel generalChannel;

    public Task_PermSync() {
        this.dryRun = false;
    }

    public Task_PermSync(boolean dryRun) {
        this.dryRun = dryRun;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Guild guild = Main.getJDA().getGuildById(Main.getConfig().getGuildId());
        if (guild == null) {
            return;
        }

        logger = Main.getLogger();
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        try {
            conn = manager.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        List<MinecraftDiscordConnection> connections = getConnections();
        if (connections == null) {
            return;
        }

        RegularRole = guild.getRoleById(597405176189419554L);
        CommunityRegularRole = guild.getRoleById(888150763421970492L);
        VerifiedRole = guild.getRoleById(597405176969560064L);
        MinecraftConnectedRole = guild.getRoleById(604011598952136853L);
        MailVerifiedRole = guild.getRoleById(597421078817669121L);
        NeedSupportRole = guild.getRoleById(786110419470254102L);
        SubAccountRole = guild.getRoleById(753047225751568474L);

        generalChannel = guild.getTextChannelById(597419057251090443L);
        if (generalChannel == null) {
            return;
        }

        guild
            .loadMembers()
            .onSuccess(members -> members
                .forEach(member -> runMember(connections, member.hasTimeJoined() ? member : guild.retrieveMember(member.getUser()).complete())));
    }

    private void runMember(List<MinecraftDiscordConnection> connections, Member member) {
        Guild guild = member.getGuild();
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
        boolean isRegular = isGrantedRole(member, RegularRole, "Regular");
        boolean isCommunityRegular = isGrantedRole(member, CommunityRegularRole, "CommunityRegular");
        boolean isVerified = isGrantedRole(member, VerifiedRole, "Verified");
        boolean isMinecraftConnected = isGrantedRole(member, MinecraftConnectedRole, "MinecraftConnected");
        boolean isMailVerified = isGrantedRole(member, MailVerifiedRole, "MailVerified");
        boolean isNeedSupport = isGrantedRole(member, NeedSupportRole, "NeedSupport");
        boolean isSubAccount = isGrantedRole(member, SubAccountRole, "SubAccount");

        String minecraftId;
        UUID uuid = null;
        if (mdc != null) {
            minecraftId = mdc.player();
            uuid = mdc.uuid();
            logger.info("[%s] Minecraft link: %s (%s)".formatted(
                member.getUser().getAsTag(),
                minecraftId,
                uuid)
            );
        } else {
            logger.info("[%s] Minecraft link: NOT LINKED".formatted(
                member.getUser().getAsTag()));
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime joinedTime = member.getTimeJoined().atZoneSameInstant(ZoneId.of("Asia/Tokyo")).toLocalDateTime();
        LocalDateTime loginDate = mdc != null ? mdc.loginDate().toLocalDateTime() : null;
        long joinMinutes = ChronoUnit.MINUTES.between(joinedTime, now);
        long joinDays = ChronoUnit.DAYS.between(joinedTime, now);
        logger.info("joinMinutes(Days): %s days (%s minutes)".formatted(joinDays, joinMinutes));

        if (!isMailVerified && !isNeedSupport && joinMinutes >= 10) {
            // 参加してから10分以内に発言のないユーザーをキックする
            kickDiscord(member, "MailVerifiedキック", "10分以上発言がなかったため、キックしました。", Color.PINK);
            return;
        }

        if (!isMinecraftConnected && !isSubAccount && !isNeedSupport && joinDays >= 7) {
            // 参加してから1週間以内にlink・サブアカウント登録・サポートへの問い合わせがない場合はキックする
            kickDiscord(member, "1weekキック", "1週間(7日)以上link・サブアカウント登録・サポートへの問い合わせがなかったため、キックしました。", Color.PINK);
            return;
        }

        if (!isMinecraftConnected && !isSubAccount && isNeedSupport && joinDays >= 21) {
            // 参加してから3週間後にサポート問い合わせのみの場合はキックする
            kickDiscord(member, "3weeksキック (NeedSupport)", "3週間(21日)以上link・サブアカウント登録がなかったため、キックしました。", Color.PINK);
            return;
        }

        SubAccount subAccount = new SubAccount(member);
        if (isSubAccount && !subAccount.isSubAccount()) {
            // SubAccount役職なのにサブアカウントではない
            notifyConnection(member, "SubAccount役職剥奪", "SubAccount役職が付与されていましたが、サブアカウント登録がなされていないため剥奪しました。", Color.RED);
            if (!dryRun) guild.addRoleToMember(member, SubAccountRole).queue();
            isSubAccount = false;
        }

        if (isSubAccount) {
            // メインアカウントが1か月以上前に退出しているのにも関わらず、SubAccount役職がついている利用者から本役職を外す。
            // 多分この実装は不十分
            MinecraftDiscordConnection subMdc = connections
                .stream()
                .filter(c -> c.disid().equals(subAccount.getMainAccount().getUser().getId()))
                .findFirst()
                .orElse(null);
            if (subMdc == null) {
                notifyConnection(member, "SubAccount役職剥奪", "メインアカウントの情報が取得できなかったため、剥奪しました。", Color.YELLOW);
                if (!dryRun) {
                    subAccount.removeMainAccount();
                    guild.addRoleToMember(member, SubAccountRole).queue();
                }
                isSubAccount = false;
            } else if (subMdc.disabled()) {
                long deadDays = ChronoUnit.DAYS.between(subMdc.dead_at().toLocalDateTime(), now);
                if (deadDays >= 30) {
                    // disabled & 30日経過
                    notifyConnection(member, "SubAccount役職剥奪", "メインアカウントが1か月(30日)以上前にlinkを切断しているため、剥奪しました。", Color.YELLOW);
                    if (!dryRun) {
                        subAccount.removeMainAccount();
                        guild.addRoleToMember(member, SubAccountRole).queue();
                    }
                    isSubAccount = false;
                }
            }
        }

        if (isSubAccount && !isMinecraftConnected) {
            return; // サブアカウントの場合、10分チェックとかのみ (Minecraft linkされている場合は除外)
        }

        if (mdc != null && !mdc.disabled() && !isMinecraftConnected) {
            // Minecraft-Discord Connectがなされていて、MinecraftConnected役職か付与されていない利用者に対して、MinecraftConnected役職を付与する
            notifyConnection(member, "MinecraftConnected役職付与", "linkがされていたため、MinecraftConnected役職を付与しました。", Color.BLUE);
            if (!dryRun) guild.addRoleToMember(member, MinecraftConnectedRole).queue();
            isMinecraftConnected = true;
        }
        if ((mdc == null || mdc.disabled()) && isMinecraftConnected) {
            // Minecraft-Discord Connectがなされておらず、MinecraftConnected役職か付与されている利用者に対して、MinecraftConnected役職を剥奪する
            notifyConnection(member, "MinecraftConnected役職剥奪", "linkがされていなかったため、MinecraftConnected役職を剥奪しました。", Color.BLUE);
            if (!dryRun) member.getGuild().addRoleToMember(member, MinecraftConnectedRole).queue();
            isMinecraftConnected = false;
        }

        if (isMinecraftConnected) {
            // MinecraftConnected役職がついている場合、Verified, Regularの役職に応じて役職を付与する

            PermissionGroup group = getPermissionGroup(uuid);
            if (group == null) {
                logger.info("[%s] Minecraft Group: グループの取得に失敗".formatted(
                    member.getUser().getAsTag()));
                return;
            }
            logger.info("[%s] Minecraft Group: %s".formatted(
                member.getUser().getAsTag(), group.name()));

            if (!isVerified && group == PermissionGroup.VERIFIED) {
                notifyConnection(member, "Verified役職付与", "Minecraft鯖内の権限に基づき、Verified役職を付与しました。", Color.CYAN);
                if (!dryRun) guild.addRoleToMember(member, VerifiedRole).queue();
            }
            if (!isRegular && group == PermissionGroup.REGULAR) {
                notifyConnection(member, "Regular役職付与", "Minecraft鯖内の権限に基づき、Regular役職を付与しました。", Color.CYAN);
                if (!dryRun) guild.addRoleToMember(member, RegularRole).queue();
            }

            Timestamp checkTS = getMaxTimestamp(mdc.loginDate(), mdc.expired_date());
            long checkDays = loginDate != null ? ChronoUnit.DAYS.between(checkTS.toLocalDateTime(), now) : -1;
            logger.info("checkDays: %s".formatted(checkDays));
            // 最終ログインから2ヶ月(60日)が経過している場合、警告リプライを#generalで送信する
            if (checkDays >= 60 && !notified.isNotified(Notified.NotifiedType.MONTH2)) {
                notifyConnection(member, "2か月経過", "最終ログインから2か月が経過したため、#generalで通知します。", Color.MAGENTA);
                if (!dryRun) {
                    generalChannel.sendMessage("""
                        <#%s> あなたのDiscordアカウントに接続されているMinecraftアカウント「`%s`」が**最終ログインから2ヶ月経過**致しました。
                        **サーバルール及び個別規約により、3ヶ月を経過すると建築物や自治体の所有権がなくなり、運営によって撤去・移動ができる**ようになり、またMinecraftアカウントとの連携が自動的に解除されます。
                        本日から1ヶ月以内にjao Minecraft Serverにログインがなされない場合、上記のような対応がなされる場合がございますのでご注意ください。""").queue();
                    notified.setNotified(Notified.NotifiedType.MONTH2);
                }
            }

            // 最終ログインから3ヶ月が経過している場合、linkをdisabledにし、MinecraftConnected権限を剥奪する
            if (checkDays >= 90) {
                notifyConnection(member, "3monthリンク切断", "最終ログインから3か月が経過したため、linkを切断し、役職を剥奪します。", Color.ORANGE);
                if (!dryRun) {
                    disableLink(mdc, uuid);
                    guild.removeRoleFromMember(member, MinecraftConnectedRole).queue();
                }
                isMinecraftConnected = false;
            }
            if (isMinecraftConnected && isSubAccount) {
                if (!dryRun) {
                    guild.removeRoleFromMember(member, SubAccountRole).queue();
                }
            }
        } else {
            // MinecraftConnected役職がついていない場合、Verified, Community Regular, Regular役職を剥奪する
            if (isVerified) {
                notifyConnection(member, "Verified役職剥奪", "linkが解除されているため、Verified役職を剥奪しました。", Color.GREEN);
                if (!dryRun) guild.removeRoleFromMember(member, VerifiedRole).queue();
            }
            if (isCommunityRegular) {
                notifyConnection(member, "CommunityRegular役職剥奪", "linkが解除されているため、CommunityRegular役職を剥奪しました。", Color.GREEN);
                if (!dryRun) guild.removeRoleFromMember(member, CommunityRegularRole).queue();
            }
            if (isRegular) {
                notifyConnection(member, "Regular役職剥奪", "linkが解除されているため、Regular役職を剥奪しました。", Color.GREEN);
                if (!dryRun) guild.removeRoleFromMember(member, RegularRole).queue();
            }
        }
    }

    Timestamp getMaxTimestamp(Timestamp a, Timestamp b) {
        if (a.before(b)) {
            return b;
        } else if (b.before(a)) {
            return a;
        } else if (a.equals(b)) {
            return a;
        }
        return null;
    }

    private List<MinecraftDiscordConnection> getConnections() {
        List<MinecraftDiscordConnection> connections = new ArrayList<>();
        try {
            try (PreparedStatement statement = conn.prepareStatement("SELECT * FROM discordlink")) {
                statement.setBoolean(1, false);
                try (ResultSet res = statement.executeQuery()) {
                    connections.add(new MinecraftDiscordConnection(
                        res.getString("player"),
                        UUID.fromString(res.getString("uuid")),
                        res.getString("disid"),
                        res.getString("discriminator"),
                        getLeastLogin(UUID.fromString(res.getString("uuid"))),
                        res.getTimestamp("expired_date"),
                        res.getTimestamp("dead_at"),
                        res.getBoolean("disabled")
                    ));
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
            if (!result.next()) {
                return null;
            }
            return result.getTimestamp("date");
        } catch (SQLException e) {
            return null;
        }
    }

    private boolean isGrantedRole(Member member, Role role, String roleName) {
        boolean isGranted = member
            .getRoles()
            .stream()
            .map(ISnowflake::getIdLong)
            .anyMatch(i -> role.getIdLong() == i);
        if (isGranted) {
            logger.info("[%s] %s".formatted(
                member.getUser().getAsTag(),
                "is%s: true".formatted(roleName)
            ));
        }
        return isGranted;
    }

    private void notifyConnection(Member member, String title, String description, Color color) {
        TextChannel channel = Main.getJDA().getTextChannelById(891021520099500082L);
        if (channel == null) {
            return;
        }
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(color)
            .setAuthor(member.getUser().getAsTag(), "https://discord.com/users/" + member.getId(), member.getUser().getEffectiveAvatarUrl());
        if (dryRun) {
            embed.setFooter("DRY-RUN MODE");
        }
        channel
            .sendMessageEmbeds(embed.build())
            .queue();
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

    private void kickDiscord(Member member, String title, String description, Color color) {
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
        if (!object.has("kick")) {
            object.put("kick", new JSONObject());
        }
        JSONArray kicks = object.getJSONArray("kick");

        if (kicks
            .toList()
            .contains(member.getId())) {
            // 処理
            notifyConnection(member, "[PROCESS] " + title, description, color);
            member.getGuild().kick(member).queue();
            kicks.remove(kicks.toList().indexOf(member.getId()));
        } else {
            // 動作予告
            notifyConnection(member, "[PRE] " + title, "次回処理時、本動作が実施されます。", color);
            kicks.put(member.getId());
        }

        object.put("kick", kicks);
        try {
            Files.writeString(path, object.toString());
        } catch (IOException e) {
            logger.warn("grantDiscordPerm json save failed.", e);
        }
    }

    private void disableLink(MinecraftDiscordConnection mdc, UUID uuid) {
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE discordlink SET disabled = ? WHERE uuid = ? AND disabled = ?")) {
            stmt.setBoolean(1, true);
            stmt.setString(2, uuid.toString());
            stmt.setBoolean(3, true);
            stmt.execute();
        } catch (SQLException e) {
            logger.warn("disableLink(%s): failed".formatted(mdc.player + "#" + mdc.uuid), e);
        }
    }

    record MinecraftDiscordConnection(String player, UUID uuid, String disid, String discriminator, Timestamp loginDate,
                                      Timestamp expired_date, Timestamp dead_at, boolean disabled) {

    }

    enum PermissionGroup {
        ADMIN,
        MODERATOR,
        REGULAR,
        VERIFIED,
        DEFAULT,
        UNKNOWN
    }

    class Notified {
        Path path = Path.of("permsync-notified.json");
        Member member;

        Notified(Member member) {
            this.member = member;
        }

        private boolean isNotified(NotifiedType type) {
            JSONObject object = load();
            return object.has(member.getId()) && object.getJSONObject(member.getId()).has(type.name());
        }

        private void setNotified(NotifiedType type) {
            JSONObject object = load();
            JSONArray userObject = object.has(member.getId()) ? object.getJSONArray(member.getId()) : new JSONArray();
            userObject.put(type.name());
            object.put(member.getId(), userObject);
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
