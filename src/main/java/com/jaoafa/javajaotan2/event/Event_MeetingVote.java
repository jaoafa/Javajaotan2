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

/**
 * #meeting_vote
 * <p>
 * ※デバッグ処理を作ること。
 * ・対象チャンネルへ投稿がされた場合、投票開始メッセージを送信しピン止めする
 * 　・投票の有効会議期限は2週間。それまでに投票が確定しない場合は否認されたものとして扱う
 * 　・投票開始から1週間経過時点でリマインドする
 * ・ピン止めメッセージに :+1: がリアクションされた場合、①の処理を行う (賛成)
 * ・ピン止めメッセージに :-1: がリアクションされた場合、①の処理を行う (反対)
 * ・ピン止めメッセージに :flag_white: がリアクションされた場合、①の処理を行う (白票)
 * ・①処理
 * 　・白票分をマイナス
 * 　・過半数が賛成の場合は承認、反対の場合は否認する
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
public class Event_MeetingVote {
}
