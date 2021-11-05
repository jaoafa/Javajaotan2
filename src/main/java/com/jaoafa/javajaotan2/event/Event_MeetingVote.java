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
import com.jaoafa.javajaotan2.lib.JavajaotanLibrary;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.awt.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * #meeting_vote
 * <p>
 * ※デバッグ処理を作ること。
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
    Logger logger;

    Event_MeetingVote() {
        this.logger = Main.getLogger();
    }

    static final String GOOD_EMOJI = "\uD83D\uDC4D";
    static final String BAD_EMOJI = "\uD83D\uDC4E";
    static final String WHITE_EMOJI = "\uD83C\uDFF3";
    static final String REMIND_EMOJI = "\uD83D\uDCF3";
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

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (Main.getConfig().getGuildId() != event.getGuild().getIdLong()) {
            return;
        }
        if (event.getChannel().getIdLong() != Channels.meeting_vote.getChannelId()) {
            return;
        }
        if (event.getAuthor().isBot()) {
            return;
        }
        if (event.getMember() == null) {
            return;
        }
        Member member = event.getMember();
        TextChannel channel = event.getTextChannel();
        Message message = event.getMessage();
        String content = message.getContentRaw();
        if (message.getType() != MessageType.DEFAULT) {
            return;
        }

        if (message.isPinned()) {
            return;
        }
        // ・対象チャンネルへ投稿がされた場合、投票開始メッセージを送信しピン止めする

        logger.info("New meeting vote!");

        message.pin().queue(
            null,
            e -> message.reply("ピン止めに失敗しました: `%s: %s`".formatted(e.getClass().getName(), e.getMessage())).queue()
        );
        message.addReaction(GOOD_EMOJI).queue();
        message.addReaction(BAD_EMOJI).queue();
        message.addReaction(WHITE_EMOJI).queue();

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
            .addField("有効審議期限", "投票の有効会議期限は2週間(%sまで)です。".formatted(limit.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))), false)
            .setColor(Color.YELLOW);

        embed.addField("決議ボーダー", "この投票の決議ボーダーは %d です。".formatted(getVoteBorderFromContent(content, 0)), false);
        channel
            .sendMessageEmbeds(embed.build())
            .reference(message)
            .mentionRepliedUser(false)
            .queue();
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (Main.getConfig().getGuildId() != event.getGuild().getIdLong()) {
            return;
        }
        if (event.getChannel().getIdLong() != Channels.meeting_vote.getChannelId()) {
            return;
        }
        User user = event.getUser();
        if (user == null) {
            user = event.retrieveUser().complete();
        }
        if (user.isBot()) {
            return;
        }
        MessageReaction.ReactionEmote emote = event.getReactionEmote();
        if (!emote.isEmoji()) {
            return;
        }
        if (!emote.getEmoji().equals(GOOD_EMOJI) && !emote.getEmoji().equals(BAD_EMOJI) && !emote.getEmoji().equals(WHITE_EMOJI)) {
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
        List<Message> messages = channel.retrievePinnedMessages().complete();
        for (Message message : messages) {
            List<User> good = message.retrieveReactionUsers(GOOD_EMOJI).complete();
            if (good == null) {
                good = new ArrayList<>();
            }
            good = good.stream().filter(u -> !u.isBot()).collect(Collectors.toList());
            List<User> bad = message.retrieveReactionUsers(BAD_EMOJI).complete();
            if (bad == null) {
                bad = new ArrayList<>();
            }
            bad = bad.stream().filter(u -> !u.isBot()).collect(Collectors.toList());
            List<User> white = message.retrieveReactionUsers(WHITE_EMOJI).complete();
            if (white == null) {
                white = new ArrayList<>();
            }
            white = white.stream().filter(u -> !u.isBot()).collect(Collectors.toList());

            // ・過半数が賛成の場合は承認、反対の場合は否認する
            int border = getVoteBorderFromContent(message.getContentRaw(), white.size());
            if (good.size() >= border) {
                approval(message, good, bad, white);
            }
            if (bad.size() >= border) {
                disapproval(message, good, bad, white, DisapprovalReason.VOTE);
            }

            ZonedDateTime limit = getVoteLimitDateTime(message);
            if (ZonedDateTime.now().isAfter(limit)) {
                // ・投票の有効会議期限は2週間。それまでに投票が確定しない場合は否認されたものとして扱う
                disapproval(message, good, bad, white, DisapprovalReason.LIMIT);
            }

            if (ZonedDateTime.now().isAfter(message
                .getTimeCreated()
                .atZoneSameInstant(ZoneId.of("Asia/Tokyo"))
                .plus(7, ChronoUnit.DAYS))) {
                // ・投票開始から1週間経過時点でリマインドする
                remind(message);
            }
        }
    }

    /**
     * 承認時処理
     */
    void approval(Message message, List<User> good, List<User> bad, List<User> white) {
    }

    /**
     * 否認時処理
     */
    void disapproval(Message message, List<User> good, List<User> bad, List<User> white, DisapprovalReason reason) {
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
    void remind(Message message) {
        // 一度リマインドしたらそれ以降はリマインドしないこと
        // リマインドの判定は絵文字？
    }

    ZonedDateTime getVoteLimitDateTime(Message message) {
        return message
            .getTimeCreated()
            .atZoneSameInstant(ZoneId.of("Asia/Tokyo"))
            .plus(14, ChronoUnit.DAYS);
    }

    int getVoteBorderFromContent(String content, int white_count) {
        Pattern p = Pattern.compile("\\[Border:([0-9]+)]");
        Matcher m = p.matcher(content);
        if (m.find() && JavajaotanLibrary.isInt(m.group(1))) {
            return Integer.parseInt(m.group(1));
        } else {
            return getVoteBorder(white_count);
        }
    }

    int getVoteBorder(int white_count) {
        int admin_and_moderators = AdminAndModerators.size();
        admin_and_moderators = admin_and_moderators - white_count;

        return (admin_and_moderators / 2) + 1;
    }
}
