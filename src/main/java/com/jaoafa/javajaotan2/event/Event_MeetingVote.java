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

package com.jaoafa.javajaotan2.event;

import com.jaoafa.javajaotan2.Main;
import com.jaoafa.javajaotan2.lib.Channels;
import com.jaoafa.javajaotan2.lib.JavajaotanData;
import com.jaoafa.javajaotan2.lib.JavajaotanLibrary;
import com.jaoafa.javajaotan2.lib.MySQLDBManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * #meeting_vote
 * <p>
 * ・対象チャンネルへ投稿がされた場合、投票開始メッセージを送信しピン止めする
 * 　・正規表現 \[Border:([0-9]+)] を含む場合、その値を決議ボーダーとして扱う -> getVoteBorderFromContent()
 * 　・投票の有効会議期限は2週間。それまでに投票が確定しない場合は否認されたものとして扱う -> disapproval()
 * 　・投票開始から1週間経過時点でリマインドする -> remind()
 * ・ピン止めメッセージに :+1: がリアクションされた場合、①の処理を行う (賛成)
 * ・ピン止めメッセージに :-1: がリアクションされた場合、①の処理を行う (反対)
 * ・ピン止めメッセージに :flag_white: がリアクションされた場合、①の処理を行う (白票)
 * ・①処理
 * 　・白票分をマイナス -> getVoteBorder()
 * 　・過半数 -> getVoteBorder()が賛成の場合は承認 -> approval()、反対の場合は否認 -> disapproval()する
 * 　・[API-CITIES- から始まるメッセージの場合は②の処理を行う
 * ・②処理
 * 　・メッセージが正規表現で \[API-CITIES-CREATE-WAITING:([0-9]+)] の場合、新規自治体作成の審議である
 * 　　・承認の場合は運営メンバーに自治体の作成処理を依頼し、完了後に /approvalcity create RequestID を実行させる
 * 　　・否認の場合は該当DB行の status に -1 を入れ、#city_request で否認されたことを知らせる
 * 　・メッセージが正規表現で \[API-CITIES-CHANGE-CORNERS-WAITING:([0-9]+)] の場合、自治体範囲変更の審議である
 * 　　・承認の場合は運営メンバーに自治体の範囲変更処理を依頼し、完了後に /approvalcity corners RequestID を実行させる
 * 　　・否認の場合は該当DB行の status に -1 を入れ、#city_request で否認されたことを知らせる
 * 　・メッセージが正規表現で \[API-CITIES-CHANGE-OTHER-WAITING:([0-9]+)] の場合、自治体情報変更の審議である
 * 　　・承認の場合は該当DB行のデータを更新し、#city_request で承認されたことを知らせる
 * 　　・否認の場合は該当DB行の status に -1 を入れ、#city_request で否認されたことを知らせる
 */
public class Event_MeetingVote extends ListenerAdapter {
    Logger logger = null;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    TextChannel meeting;
    TextChannel cityRequest;

    /** Admin と Moderator の定義 */
    static final List<Long> AdminAndModerators = List.of(
        206692134991036416L, // zakuro
        221498004505362433L, // hiratake
        221991565567066112L, // tomachi
        222337959087702016L, // omelet
        189377054955798528L, // ekusas
        189372008147058688L, // zokasu
        315726390844719114L, // kohona
        290787709721509890L, // nudonge
        310570792691826688L, // ekp
        492088741167366144L // yuua
    );
    /** 日時のテキストフォーマット */
    static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    /** clubjaoafa からの自治体関連リクエストプレフィックス */
    static final String API_CITIES_PREFIX = "[API-CITIES-";
    /** 自治体新規作成リクエスト管理テキストパターン */
    static final Pattern CITIES_CREATE_WAITING_PATTERN = Pattern.compile("\\[API-CITIES-CREATE-WAITING:([0-9]+)]");
    /** 自治体範囲変更リクエスト管理テキストパターン */
    static final Pattern CITIES_CORNERS_WAITING_PATTERN = Pattern.compile("\\[API-CITIES-CHANGE-CORNERS-WAITING:([0-9]+)]");
    /** 自治体情報変更リクエスト管理テキストパターン */
    static final Pattern CITIES_CHANGE_OTHER_WAITING_PATTERN = Pattern.compile("\\[API-CITIES-CHANGE-OTHER-WAITING:([0-9]+)]");

    public void initChannels(JDA jda) {
        this.logger = Main.getLogger();
        this.meeting = jda.getTextChannelById(597423467796758529L);
        this.cityRequest = jda.getTextChannelById(709008822043148340L);
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        initChannels(event.getJDA());
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (Main.getConfig().getGuildId() != event.getGuild().getIdLong()) return;
        if (event.getChannel().getIdLong() != Channels.meeting_vote.getChannelId()) return;
        if (event.getAuthor().isBot()) return;
        if (event.getMember() == null) return;

        Member member = event.getMember();
        TextChannel channel = event.getTextChannel();
        Message message = event.getMessage();
        String content = message.getContentRaw();

        if (message.getType() != MessageType.DEFAULT) return;
        if (message.isPinned()) return;

        if (logger == null) {
            initChannels(event.getJDA());
        }

        // ・対象チャンネルへ投稿がされた場合、投票開始メッセージを送信しピン止めする

        logger.info("New meeting vote!");

        message.pin().queue(
            null,
            e -> message.reply("ピン止めに失敗しました: `%s: %s`".formatted(e.getClass().getName(), e.getMessage())).queue()
        );
        VoteReaction.GOOD.addReaction(message);
        VoteReaction.BAD.addReaction(message);
        VoteReaction.WHITE.addReaction(message);

        ZonedDateTime limit = getVoteLimitDateTime(message);

        // 新規開始メッセージ
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(":new: 新しい投票")
            .setDescription("%s からの新しい審議投票です。".formatted(member.getAsMention()))
            .addField("賛成の場合", "**投票メッセージに対して**:thumbsup:リアクションを付けてください。", false)
            .addField("反対の場合", "**投票メッセージに対して**:thumbsdown:リアクションを付けてください。\n" +
                "反対の場合は <#597423467796758529> に意見理由を必ず書いてください。", false)
            .addField("白票の場合", "**投票メッセージに対して**:flag_white:リアクションを付けてください。\n" +
                "(白票の場合投票権利を放棄し他の人に投票を委ねます)", false)
            .addField("この投票に対する話し合い", "<#597423467796758529> でどうぞ。", false)
            .addField("有効審議期限", "投票の有効会議期限は2週間(%sまで)です。".formatted(limit.format(DATETIME_FORMAT)), false)
            .setColor(Color.YELLOW)
            .setTimestamp(Instant.now());

        embed.addField("決議ボーダー", "この投票の決議ボーダーは %d です。".formatted(getVoteBorderFromContent(content, 0)), false);
        channel
            .sendMessageEmbeds(embed.build())
            .reference(message)
            .mentionRepliedUser(false)
            .queue();
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (Main.getConfig().getGuildId() != event.getGuild().getIdLong()) return;
        if (event.getChannel().getIdLong() != Channels.meeting_vote.getChannelId()) return;

        User user = event.getUser();

        if (user == null) user = event.retrieveUser().complete();
        if (user.isBot()) return;

        MessageReaction.ReactionEmote emote = event.getReactionEmote();

        if (!emote.isEmoji()) return;
        if (!emote.getEmoji().equals(VoteReaction.GOOD.getUnicode())
            && !emote.getEmoji().equals(VoteReaction.BAD.getUnicode())
            && !emote.getEmoji().equals(VoteReaction.WHITE.getUnicode()))
            return;

        Message message = event.retrieveMessage().complete();
        if (VoteReaction.multipleVote(message, user)) {
            event.getReaction().removeReaction(user).queue();
            Message replyMessage = new MessageBuilder()
                .setContent(user.getAsMention())
                .setEmbeds(new EmbedBuilder()
                    .setDescription("賛成・反対・白票はいずれか一つのみリアクションしてください！変更する場合はすでにつけているリアクションを外してからリアクションしてください。")
                    .setFooter("このメッセージは1分で削除されます")
                    .build())
                .build();
            event
                .getChannel()
                .sendMessage(replyMessage)
                .reference(message)
                .mentionRepliedUser(false)
                .delay(1, TimeUnit.MINUTES, scheduler) // delete 1 minute later
                .flatMap(Message::delete)
                .queue();
            return;
        }

        processVotes();
    }

    /**
     * 全投票(ピン止めされているメッセージ)を処理する
     */
    void processVotes() {
        logger.info("processVote()");
        TextChannel channel = Channels.meeting_vote.getChannel();
        if (channel == null) {
            logger.error("meeting_vote channel is not found.");
            return;
        }

        for (Message message : channel.retrievePinnedMessages().complete()) {
            List<User> good = VoteReaction.GOOD.getUsers(message);
            List<User> bad = VoteReaction.BAD.getUsers(message);
            List<User> white = VoteReaction.WHITE.getUsers(message);

            // ・過半数が賛成の場合は承認、反対の場合は否認する
            int border = getVoteBorderFromContent(message.getContentRaw(), white.size());
            if (good.size() >= border)
                approval(message, good, bad, white);
            if (bad.size() >= border)
                disapproval(message, good, bad, white, DisapprovalReason.VOTE);

            ZonedDateTime limit = getVoteLimitDateTime(message);
            if (ZonedDateTime.now().isAfter(limit))
                // ・投票の有効会議期限は2週間。それまでに投票が確定しない場合は否認されたものとして扱う
                disapproval(message, good, bad, white, DisapprovalReason.LIMIT);

            if (ZonedDateTime.now().isAfter(
                message
                    .getTimeCreated()
                    .atZoneSameInstant(ZoneId.of("Asia/Tokyo"))
                    .plus(7, ChronoUnit.DAYS)))
                // ・投票開始から1週間経過時点でリマインドする
                remind(message, good, bad, white);
        }
    }

    /**
     * 承認時処理
     */
    void approval(Message message, List<User> good, List<User> bad, List<User> white) {
        int border = getVoteBorderFromContent(message.getContentRaw(), white.size());

        Function<List<User>, String> getAsTag = users ->
            users.stream().map(User::getAsTag).collect(Collectors.joining(" "));

        message.replyEmbeds(new EmbedBuilder()
            .setTitle("投票承認のお知らせ")
            .setDescription(":+1: 過半数が賛成したため、投票が承認されたことをお知らせします。")
            .addField("賛成 / 反対 / 白票", "%s / %s / %s".formatted(
                good.size(), bad.size(), white.size()
            ), false)
            .addField("決議ボーダー", String.valueOf(border), false)
            .addField("メンバー", "賛成: %s\n反対: %s\n白票: %s".formatted(
                getAsTag.apply(good), getAsTag.apply(bad), getAsTag.apply(white)
            ), false)
            .addField("内容", message.getContentRaw().length() < MessageEmbed.VALUE_MAX_LENGTH
                ? message.getContentRaw()
                : message.getContentRaw().substring(0, MessageEmbed.VALUE_MAX_LENGTH), false)
            .addField("対象メッセージURL", message.getJumpUrl(), false)
            .addField("投票開始日時", message.getTimeCreated().format(DATETIME_FORMAT), false)
            .setColor(Color.GREEN)
            .setTimestamp(Instant.now())
            .build()
        ).queue();
        message.unpin().queue();

        processCityRequest(message, true);
    }

    /**
     * 否認時処理
     */
    void disapproval(Message message, List<User> good, List<User> bad, List<User> white, DisapprovalReason reason) {
        int border = getVoteBorderFromContent(message.getContentRaw(), white.size());
        String why = switch (reason) {
            case VOTE -> "過半数が反対した";
            case LIMIT -> "有効審議期限を過ぎた";
        };
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("投票否認のお知らせ")
            .setDescription(":-1: %sため、投票が否認されたことをお知らせします。".formatted(why))
            .addField("賛成 / 反対 / 白票", "%s / %s / %s".formatted(good.size(), bad.size(), white.size()), false)
            .addField("決議ボーダー", String.valueOf(border), false)
            .addField("メンバー", "賛成: %s\n反対: %s\n白票: %s".formatted(
                good.stream().map(User::getAsTag).collect(Collectors.joining(" ")),
                bad.stream().map(User::getAsTag).collect(Collectors.joining(" ")),
                white.stream().map(User::getAsTag).collect(Collectors.joining(" "))
            ), false)
            .addField("内容", message.getContentRaw().length() < MessageEmbed.VALUE_MAX_LENGTH
                ? message.getContentRaw()
                : message.getContentRaw().substring(0, MessageEmbed.VALUE_MAX_LENGTH), false)
            .addField("対象メッセージURL", message.getJumpUrl(), false)
            .addField("投票開始日時", message.getTimeCreated().format(DATETIME_FORMAT), false)
            .setColor(Color.RED)
            .setTimestamp(Instant.now());
        message.replyEmbeds(embed.build()).queue();
        message.unpin().queue();

        processCityRequest(message, false);
    }

    /**
     * 自治体関連のリクエスト処理
     */
    private void processCityRequest(Message message, boolean approval) {
        String contents = message.getContentRaw();
        if (!contents.startsWith(API_CITIES_PREFIX)) {
            return;
        }
        Matcher matcherCreateWaiting = CITIES_CREATE_WAITING_PATTERN.matcher(contents);
        if (matcherCreateWaiting.find()) {
            processCreateWaiting(approval, Integer.parseInt(matcherCreateWaiting.group(1)));
        }

        Matcher matcherChangeCorners = CITIES_CORNERS_WAITING_PATTERN.matcher(contents);
        if (matcherChangeCorners.find()) {
            processChangeCorners(approval, Integer.parseInt(matcherChangeCorners.group(1)));
        }

        Matcher matcherChangeOther = CITIES_CHANGE_OTHER_WAITING_PATTERN.matcher(contents);
        if (matcherChangeOther.find()) {
            processChangeOther(approval, Integer.parseInt(matcherChangeOther.group(1)));
        }
    }

    private final Function<JSONArray, List<String>> getApprovalFlowBuilder = corners -> new LinkedList<>() {{
        add("サーバにログインします。");
        add("鯖内でコマンドを実行: `//sel poly`");
        for (int i = 0; i < corners.length(); i++) {
            JSONObject corner = corners.getJSONObject(i);
            add("鯖内でコマンドを実行: `/tp %d 100 %d`".formatted(corner.getInt("x"), corner.getInt("z")));
            if (i == 0)
                add("鯖内でコマンドを実行: `//pos1`");
            else
                add("鯖内でコマンドを実行: `//pos2`");
        }
        add("鯖内でコマンドを実行: `//expand vert`");
    }};


    /**
     * 自治体新規登録リクエスト処理
     */
    private void processCreateWaiting(boolean approval, int id) {
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        try {
            Connection conn = manager.getConnection();

            String discordUserId;
            String citiesName;
            String regionName;
            String regionOwner;
            JSONArray corners;
            try (PreparedStatement stmt = conn
                .prepareStatement("SELECT * FROM cities_new_waiting WHERE id = ?")) {
                stmt.setInt(1, id);
                try (ResultSet res = stmt.executeQuery()) {
                    if (!res.next()) {
                        System.out.println("processCreateWaiting(): res.next false");
                        return;
                    }
                    discordUserId = res.getString("discord_userid");
                    citiesName = res.getString("name");
                    regionName = res.getString("regionname");
                    regionOwner = res.getString("player");
                    corners = new JSONArray(res.getString("corners"));
                }
            }

            if (approval) {
                List<String> approvalFlowBuilder = getApprovalFlowBuilder.apply(corners);
                approvalFlowBuilder.add("鯖内でコマンドを実行: `/rg define %s %s`".formatted(regionName, regionOwner));
                approvalFlowBuilder.add("<#597423467796758529>内でコマンド「`/approvalcity create %d`」を実行".formatted(id));

                List<String> approvalFlows = new LinkedList<>();
                int i = 1;
                for (String str : approvalFlowBuilder) {
                    approvalFlows.add(i + ". " + str);
                    i++;
                }

                meeting.sendMessage(
                    "**自治体「`%s`」の新規登録申請が承認されました。これに伴い、運営利用者は以下の作業を順に実施してください。**\n%s".formatted(
                        citiesName,
                        String.join("\n", approvalFlows)
                    )).queue();
            } else {
                cityRequest.sendMessage("<@%s> 自治体「`%s`」の自治体新規登録申請を**否認**しました。(リクエストID: %d)".formatted(discordUserId, citiesName, id)).queue();

                try (PreparedStatement stmt = conn
                    .prepareStatement("UPDATE cities_new_waiting SET status = ? WHERE id = ?")) {
                    stmt.setInt(1, -1);
                    stmt.setInt(2, id);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 自治体範囲変更リクエスト処理
     */
    private void processChangeCorners(boolean approval, int id) {
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        try {
            Connection conn = manager.getConnection();

            int citiesId;
            JSONArray corners;
            try (PreparedStatement stmt = conn
                .prepareStatement("SELECT * FROM cities_corners_waiting WHERE id = ?")) {
                stmt.setInt(1, id);
                try (ResultSet res = stmt.executeQuery()) {
                    if (!res.next()) {
                        System.out.println("processChangeCorners(): res.next false");
                        return;
                    }
                    citiesId = res.getInt("cities_id");
                    corners = new JSONArray(res.getString("corners_new"));
                }
            }

            String discordUserID = getDiscordUserID(citiesId);
            String citiesName = getCitiesName(citiesId);
            String regionName = getRegionName(citiesId);

            if (approval) {
                List<String> approvalFlowBuilder = getApprovalFlowBuilder.apply(corners);
                approvalFlowBuilder.add("鯖内でコマンドを実行: `/rg redefine %s`".formatted(regionName));
                approvalFlowBuilder.add("<#597423467796758529>内でコマンド「`/approvalcity corners %d`」を実行してください。".formatted(id));

                List<String> approvalFlows = new LinkedList<>();
                int i = 1;
                for (String str : approvalFlowBuilder) {
                    approvalFlows.add(i + ". " + str);
                    i++;
                }

                meeting.sendMessage(
                    "**自治体「`%s`」の範囲変更申請が承認されました。これに伴い、運営利用者は以下の作業を順に実施してください。**\n%s".formatted(
                        citiesName,
                        String.join("\n", approvalFlows)
                    )).queue();
            } else {
                cityRequest.sendMessage(
                    "<@%s> 自治体「`%s` (%d)」の自治体範囲変更申請を**否認**しました。(リクエストID: %d)".formatted(
                        discordUserID, citiesName, citiesId, id
                    )).queue();

                try (PreparedStatement stmt = conn
                    .prepareStatement("UPDATE cities_corners_waiting SET status = ? WHERE id = ?")) {
                    stmt.setInt(1, -1);
                    stmt.setInt(2, id);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 自治体情報更新リクエスト処理
     */
    private void processChangeOther(boolean approval, int id) {
        MySQLDBManager manager = JavajaotanData.getMainMySQLDBManager();
        try {
            Connection conn = manager.getConnection();

            LinkedList<String> pre_sql = new LinkedList<>();
            LinkedList<String> setStrings = new LinkedList<>();

            String[] keys = new String[]{
                "name",
                "namekana",
                "regionname",
                "summary",
                "name_origin"
            };

            int citiesId;
            try (PreparedStatement stmt = conn
                .prepareStatement("SELECT * FROM cities_other_waiting WHERE id = ?")) {
                stmt.setInt(1, id);
                try (ResultSet res = stmt.executeQuery()) {
                    if (!res.next()) {
                        System.out.println("processChangeOther(): res.next false");
                        return;
                    }
                    citiesId = res.getInt("cities_id");

                    for (String key : keys) {
                        if (res.getString("%s_new".formatted(key)) == null) {
                            continue;
                        }
                        pre_sql.add("%s = ?".formatted(key));
                        setStrings.add(res.getString("%s_new".formatted(key)));
                    }
                }
            }

            try (PreparedStatement stmt = conn
                .prepareStatement("UPDATE cities SET " + String.join(", ", pre_sql) + " WHERE id = ?")) {
                int i = 1;
                for (String str : setStrings) {
                    stmt.setString(i, str);
                    i++;
                }
                stmt.setInt(i, citiesId);
                System.out.println("SQL: " + stmt);
                stmt.executeUpdate();
            }


            String discordUserID = getDiscordUserID(citiesId);
            String citiesName = getCitiesName(citiesId);

            if (approval) {
                meeting.sendMessage("自治体情報更新のため、次のSQLが実行されました: `%s` (%s)".formatted(
                    "UPDATE cities SET " + String.join(", ", pre_sql) + " WHERE id = ?",
                    setStrings.stream().map("`%s`"::formatted).collect(Collectors.joining(", "))
                )).queue();

                cityRequest.sendMessage("<@%s> 自治体「`%s` (%d)」の自治体情報変更申請を**承認**しました。(リクエストID: %d)".formatted(discordUserID, citiesName, citiesId, id)).queue();

                try (PreparedStatement stmt = conn
                    .prepareStatement("UPDATE cities_other_waiting SET status = ? WHERE id = ?")) {
                    stmt.setInt(1, 1);
                    stmt.setInt(2, id);
                    stmt.executeUpdate();
                }
            } else {
                cityRequest.sendMessage("<@%s> 自治体「`%s` (%d)」の自治体情報変更申請を**否認**しました。(リクエストID: %d)".formatted(discordUserID, citiesName, citiesId, id)).queue();

                try (PreparedStatement stmt = conn
                    .prepareStatement("UPDATE cities_other_waiting SET status = ? WHERE id = ?")) {
                    stmt.setInt(1, -1);
                    stmt.setInt(2, id);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 自治体情報から指定されたカラムにあるデータを取り出します。
     */
    private final BiFunction<String, Integer, String> getStringFromCitiesRecord = (column, cities_id) -> {
        try {
            try (PreparedStatement stmt =
                     JavajaotanData
                         .getMainMySQLDBManager()
                         .getConnection()
                         .prepareStatement("SELECT * FROM cities WHERE id = ?")) {
                stmt.setInt(1, cities_id);
                try (ResultSet res = stmt.executeQuery()) {
                    if (!res.next()) return null;

                    return res.getString(column);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    };

    /**
     * 自治体 ID から自治体所有者の Discord user id を取得
     */
    private String getDiscordUserID(int cities_id) {
        return getStringFromCitiesRecord.apply("discord_userid", cities_id);
    }

    /**
     * 自治体 ID から自治体名を取得
     */
    private String getCitiesName(int cities_id) {
        return getStringFromCitiesRecord.apply("name", cities_id);
    }

    /**
     * 自治体 ID から自治体保護名を取得
     */
    private String getRegionName(int cities_id) {
        return getStringFromCitiesRecord.apply("regionname", cities_id);
    }

    enum DisapprovalReason {
        /** 投票による */
        VOTE,
        /** 期限切れによる */
        LIMIT
    }

    /**
     * リマインド
     */
    void remind(Message message, List<User> good, List<User> bad, List<User> white) {
        // 一度リマインドしたらそれ以降はリマインドしないこと
        // リマインドの判定は絵文字？
        List<User> remindUsers = VoteReaction.REMIND.getUsers(message);

        // リマインド済み
        if (remindUsers.stream()
            .anyMatch(u -> u.getIdLong() == Main.getJDA().getSelfUser().getIdLong())) return;

        String non_voters_mention = AdminAndModerators.stream()
            .filter(uid -> good.stream().noneMatch(u -> u.getIdLong() == uid))
            .filter(uid -> bad.stream().noneMatch(u -> u.getIdLong() == uid))
            .filter(uid -> white.stream().noneMatch(u -> u.getIdLong() == uid))
            .map("<@%s>"::formatted)
            .collect(Collectors.joining(" "));

        ZonedDateTime limit = getVoteLimitDateTime(message);
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(":bangbang: 有効審議期限前のお知らせ")
            .setDescription("有効審議期限が1週間を切った投票があります！投票をお願いします。")
            .addField("有効審議期限", limit.format(DATETIME_FORMAT), false)
            .addField("メッセージURL", message.getJumpUrl(), false)
            .setColor(Color.PINK)
            .setTimestamp(Instant.now());
        message
            .getChannel()
            .sendMessage(new MessageBuilder().setContent(non_voters_mention).setEmbeds(embed.build()).build())
            .reference(message)
            .mentionRepliedUser(false)
            .queue();
        VoteReaction.REMIND.addReaction(message);
    }

    /** リアクションで使う絵文字群 */
    enum VoteReaction {
        GOOD("\uD83D\uDC4D"),
        BAD("\uD83D\uDC4E"),
        WHITE("\uD83C\uDFF3"),
        REMIND("\uD83D\uDCF3");

        String unicode;

        VoteReaction(String unicode) {
            this.unicode = unicode;
        }

        public String getUnicode() {
            return unicode;
        }

        /**
         * リアクションを付けます
         *
         * @param message メッセージ
         */
        public void addReaction(@NotNull Message message) {
            message.addReaction(getUnicode()).queue();
        }

        /**
         * リアクションを付けたユーザーのうち、Botを除外したリストを返します。
         *
         * @param message メッセージ
         *
         * @return リアクションを付けたユーザーのうち、Botを除外したリスト
         */
        public List<User> getUsers(@NotNull Message message) {
            return message
                .retrieveReactionUsers(getUnicode())
                .complete()
                .stream()
                .filter(u -> !u.isBot())
                .collect(Collectors.toList());
        }

        /**
         * メッセージにユーザーがリアクションを付けているかを調べます
         *
         * @param message メッセージ
         * @param user    ユーザー
         *
         * @return リアクションをつけているか
         */
        public boolean isReacted(Message message, User user) {
            return message
                .retrieveReactionUsers(getUnicode())
                .stream()
                .anyMatch(u -> u.getIdLong() == user.getIdLong());
        }

        /**
         * 複数の対象絵文字のリアクションを付けているかを判定します。(リマインド絵文字は除く)
         *
         * @param message メッセージ
         * @param user    ユーザー
         *
         * @return 複数の対象絵文字のリアクションを付けているか (リマインド絵文字は除く)
         */
        static boolean multipleVote(Message message, User user) {
            return Arrays.stream(values())
                .filter(v -> v != REMIND)
                .filter(v -> v.isReacted(message, user))
                .count() > 1;
        }
    }

    /**
     * メッセージから有効審議期限を取得
     */
    ZonedDateTime getVoteLimitDateTime(Message message) {
        return message
            .getTimeCreated()
            .atZoneSameInstant(ZoneId.of("Asia/Tokyo"))
            .plus(14, ChronoUnit.DAYS);
    }

    /**
     * メッセージテキストと白票数から投票ボーダーを算出
     */
    int getVoteBorderFromContent(String content, int white_count) {
        Matcher m = Pattern.compile("\\[Border:([0-9]+)]").matcher(content);

        if (m.find() && JavajaotanLibrary.isInt(m.group(1)))
            return Integer.parseInt(m.group(1));
        else
            return getVoteBorder(white_count);
    }

    /**
     * 白票数から投票ボーダーを算出
     */
    int getVoteBorder(int white_count) {
        return ((AdminAndModerators.size() - white_count) / 2) + 1;
    }
}
