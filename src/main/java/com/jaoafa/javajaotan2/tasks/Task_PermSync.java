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
import com.jaoafa.javajaotan2.lib.Channels;
import com.jaoafa.javajaotan2.lib.JavajaotanData;
import com.jaoafa.javajaotan2.lib.JavajaotanLibrary;
import com.jaoafa.javajaotan2.lib.MySQLDBManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
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
import java.util.Date;
import java.util.List;
import java.util.*;
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

        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        try {
            conn = manager.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        List<RunPermSync.MinecraftDiscordConnection> connections = getConnections();

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

        Path path = Path.of("nitrotan.json");
        try {
            if (Files.exists(path)) {
                nitrotan = new JSONObject(Files.readString(path));
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    private List<RunPermSync.MinecraftDiscordConnection> getConnections() {
        List<RunPermSync.MinecraftDiscordConnection> connections = new ArrayList<>();
        List<UUID> inserted = new ArrayList<>();
        try {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM discordlink ORDER BY id DESC")) {
                try (ResultSet res = stmt.executeQuery()) {
                    while (res.next()) {
                        if (inserted.contains(UUID.fromString(res.getString("uuid")))) continue;
                        connections.add(new RunPermSync.MinecraftDiscordConnection(
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

    class RunPermSync implements Runnable {
        final List<MinecraftDiscordConnection> connections;
        final Member member;

        public RunPermSync(List<MinecraftDiscordConnection> connections, Member member) {
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
            MinecraftDiscordConnection mdc = connections
                .stream()
                .filter(c -> c.disid().equals(member.getId()))
                .findFirst()
                .orElse(null);

            boolean isRegular = JavajaotanLibrary.isGrantedRole(member, Roles.Regular.role);
            boolean isCommunityRegular = JavajaotanLibrary.isGrantedRole(member, Roles.CommunityRegular.role);
            boolean isVerified = JavajaotanLibrary.isGrantedRole(member, Roles.Verified.role);
            boolean isMinecraftConnected = JavajaotanLibrary.isGrantedRole(member, Roles.MinecraftConnected.role);
            boolean isSubAccount = JavajaotanLibrary.isGrantedRole(member, Roles.SubAccount.role);
            boolean isNitrotan = JavajaotanLibrary.isGrantedRole(member, Roles.Nitrotan.role);

            // Nitroの場合(gif画像がアイコンの場合)、Nitrotanを付与する
            if (member.getUser().getEffectiveAvatarUrl().endsWith(".gif") && !isNitrotan) {
                notifyConnection(member, "Nitrotan役職付与", "アイコンがGIF画像だったので、Nitrotan役職を付与しました。", Color.LIGHT_GRAY, mdc);
                if (!dryRun) guild.addRoleToMember(member, Roles.Nitrotan.role).queue();
                isNitrotan = true;
            }
            // Nitrotan役職がついていて、GIFアイコンでなく、アニメーション/外部絵文字の最終メッセージ送信日時が1週間前の場合、Nitrotan役職を剥奪する
            // アニメーション/外部絵文字の最終メッセージ送信日時が何ミリ秒前か？
            long lastNitroAgo = nitrotan.has(member.getId()) ? new Date().getTime() - nitrotan.getLong(member.getId()) : -1;
            // 7日 * 24時間 * 60分 * 60秒 * 1000秒
            if (isNitrotan && !member.getUser().getEffectiveAvatarUrl().endsWith(".gif") && (lastNitroAgo == -1 || lastNitroAgo > 7 * 24 * 60 * 60 * 1000)) {
                notifyConnection(member, "Nitrotan役職剥奪", "アイコンがGIF画像ではなく、またアニメーション/外部絵文字を使用したメッセージが1週間以上前だったためNitrotan役職を剥奪しました。", Color.LIGHT_GRAY, mdc);
                if (!dryRun) guild.removeRoleFromMember(member, Roles.Nitrotan.role).queue();
            }

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
            long joinDays = ChronoUnit.DAYS.between(joinedTime, now);
            logger.info("[%s] joinDays: %s days".formatted(member.getUser().getAsTag(), joinDays));

            // サブアカウントの場合、10分チェックとかのみ (Minecraft linkされている場合は除外)
            if (isSubAccount && !isMinecraftConnected) return;

            //giveRole,description,isMinecraftConnected
            BiFunction<Boolean, String, Boolean> doMinecraftConnectedManage = (giveRole, description) -> {
                notifyConnection(member, "MinecraftConnected役職" + (giveRole ? "付与" : "剥奪"), description, Color.BLUE, mdc);
                if (!dryRun) {
                    if (giveRole) {
                        guild.addRoleToMember(member, Roles.MinecraftConnected.role).queue();
                    } else {
                        guild.removeRoleFromMember(member, Roles.MinecraftConnected.role).queue();
                    }
                }
                return true;
            };

            if (mdc != null && !mdc.disabled() && !isMinecraftConnected) {
                // Minecraft-Discord Connectがなされていて、MinecraftConnected役職か付与されていない利用者に対して、MinecraftConnected役職を付与する
                isMinecraftConnected = doMinecraftConnectedManage.apply(true, "linkがされていたため、MinecraftConnected役職を付与しました。");
            }
            if ((mdc == null || mdc.disabled()) && isMinecraftConnected) {
                // Minecraft-Discord Connectがなされておらず、MinecraftConnected役職か付与されている利用者に対して、MinecraftConnected役職を剥奪する
                isMinecraftConnected = doMinecraftConnectedManage.apply(false, "linkがされていなかったため、MinecraftConnected役職を剥奪しました。");
            }

            if (isMinecraftConnected && uuid != null) {
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
                    notifyConnection(member, "Verified役職付与", "Minecraft鯖内の権限に基づき、Verified役職を付与しました。", Color.CYAN, mdc);
                    if (!dryRun) guild.addRoleToMember(member, Roles.Verified.role).queue();
                }
                if (!isRegular && group == PermissionGroup.REGULAR) {
                    notifyConnection(member, "Regular役職付与", "Minecraft鯖内の権限に基づき、Regular役職を付与しました。", Color.CYAN, mdc);
                    if (!dryRun) guild.addRoleToMember(member, Roles.Regular.role).queue();
                }
            } else {
                // MinecraftConnected役職がついていない場合、Verified, Community Regular, Regular役職を剥奪する
                List<Roles> roles = new ArrayList<>();

                if (isVerified) roles.add(Roles.Verified);
                if (isCommunityRegular) roles.add(Roles.CommunityRegular);
                if (isRegular) roles.add(Roles.Regular);

                for (Roles role : roles) {
                    notifyConnection(member, "%s役職剥奪".formatted(role.name()), "linkが解除されているため、%s役職を剥奪しました。".formatted(role.name()), Color.GREEN, mdc);
                    if (!dryRun) guild.removeRoleFromMember(member, role.role).queue();
                }
            }
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

        record MinecraftDiscordConnection(String player, UUID uuid, String disid, String discriminator,
                                          Timestamp loginDate,
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
    }

    public enum Roles {
        Regular(597405176189419554L, null),
        CommunityRegular(888150763421970492L, null),
        Verified(597405176969560064L, null),
        MinecraftConnected(604011598952136853L, null),
        MailVerified(597421078817669121L, null),
        NeedSupport(786110419470254102L, null),
        SubAccount(753047225751568474L, null),
        Nitrotan(795153241385861130L, null),
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
