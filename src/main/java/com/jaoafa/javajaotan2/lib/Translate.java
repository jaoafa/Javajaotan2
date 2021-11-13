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

package com.jaoafa.javajaotan2.lib;

import com.detectlanguage.DetectLanguage;
import com.detectlanguage.errors.APIError;
import com.jaoafa.javajaotan2.Main;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

public class Translate {
    public static TranslateResult translate(Language before, Language after, String text) {
        String url = Main.getConfig().getGASTranslateAPIUrl();
        if (url == null) {
            throw new UndefinedTranslateAPIUrl();
        }
        if (before == Language.UNKNOWN) {
            // 言語検出
            before = detectLanguage(text);
            if (before == Language.UNKNOWN) before = Language.AUTO;
        }
        try {
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = RequestBody.create(
                new JSONObject()
                    .put("text", text)
                    .put("before", before.name().replace("_", "-").toLowerCase(Locale.ROOT))
                    .put("after", after.name().replace("_", "-").toLowerCase(Locale.ROOT))
                    .toString(),
                MediaType.parse("application/json; charset=utf-8")
            );
            Request request = new Request.Builder().url(url).post(requestBody).build();
            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();
            if (body == null) {
                return null;
            }
            JSONObject object = new JSONObject(body.string());
            return new TranslateResult(
                object.getJSONObject("response").getString("result"),
                before,
                after
            );
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static Language detectLanguage(String text) {
        DetectLanguage.apiKey = Main.getConfig().getDetectLanguageAPIToken();
        DetectLanguage.ssl = true;
        try {
            String language = DetectLanguage.simpleDetect(text);
            Main.getLogger().info("detectLanguage: " + language);
            if (language.equals("zh-Hant")) {
                language = "zh-TW";
            }
            return getLanguage(language);
        } catch (APIError e) {
            e.printStackTrace();
            return Language.UNKNOWN;
        }
    }

    static class UndefinedTranslateAPIUrl extends RuntimeException {
        public UndefinedTranslateAPIUrl() {
            super("Translate API URL is not defined.");
        }
    }

    public static Language getLanguage(String language) {
        try {
            return Language.valueOf(language.replace("-", "_").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return Language.UNKNOWN;
        }
    }

    public record TranslateResult(
        String result,
        Language from,
        Language to
    ) {
    }

    public enum Language {
        AUTO("自動検出"),
        AF("アフリカーンス語"),
        SQ("アルバニア語"),
        AM("アムハラ語"),
        AR("アラビア文字"),
        HY("アルメニア語"),
        AZ("アゼルバイジャン語"),
        EU("バスク語"),
        BE("ベラルーシ語"),
        BN("ベンガル文字"),
        BS("ボスニア語"),
        BG("ブルガリア語"),
        CA("カタロニア語"),
        CEB("セブ語"),
        ZH("中国語（簡体）"),
        ZH_CN("中国語（簡体）"),
        ZH_TW("中国語（繁体）"),
        CO("コルシカ語"),
        HR("クロアチア語"),
        CS("チェコ語"),
        DA("デンマーク語"),
        NL("オランダ語"),
        EN("英語"),
        EO("エスペラント語"),
        ET("エストニア語"),
        FI("フィンランド語"),
        FR("フランス語"),
        FY("フリジア語"),
        GL("ガリシア語"),
        KA("グルジア語"),
        DE("ドイツ語"),
        EL("ギリシャ語"),
        GU("グジャラト語"),
        HT("クレオール語（ハイチ）"),
        HA("ハウサ語"),
        HAW("ハワイ語"),
        HE("ヘブライ語"),
        IW("ヘブライ語"),
        HI("ヒンディー語"),
        HMN("モン語"),
        HU("ハンガリー語"),
        IS("アイスランド語"),
        IG("イボ語"),
        ID("インドネシア語"),
        GA("アイルランド語"),
        IT("イタリア語"),
        JA("日本語"),
        JV("ジャワ語"),
        KN("カンナダ語"),
        KK("カザフ語"),
        KM("クメール語"),
        RW("キニヤルワンダ語"),
        KO("韓国語"),
        KU("クルド語"),
        KY("キルギス語"),
        LO("ラオ語"),
        LV("ラトビア語"),
        LT("リトアニア語"),
        LB("ルクセンブルク語"),
        MK("マケドニア語"),
        MG("マラガシ語"),
        MS("マレー語"),
        ML("マラヤーラム文字"),
        MT("マルタ語"),
        MI("マオリ語"),
        MR("マラーティー語"),
        MN("モンゴル語"),
        MY("ミャンマー語（ビルマ語）"),
        NE("ネパール語"),
        NO("ノルウェー語"),
        NY("ニャンジャ語（チェワ語）"),
        OR("オリヤ語"),
        PS("パシュト語"),
        FA("ペルシャ語"),
        PL("ポーランド語"),
        PT("ポルトガル語（ポルトガル、ブラジル）"),
        PA("パンジャブ語"),
        RO("ルーマニア語"),
        RU("ロシア語"),
        SM("サモア語"),
        GD("スコットランド ゲール語"),
        SR("セルビア語"),
        ST("セソト語"),
        SN("ショナ語"),
        SD("シンド語"),
        SI("シンハラ語"),
        SK("スロバキア語"),
        SL("スロベニア語"),
        SO("ソマリ語"),
        ES("スペイン語"),
        SU("スンダ語"),
        SW("スワヒリ語"),
        SV("スウェーデン語"),
        TL("タガログ語（フィリピン語）"),
        TG("タジク語"),
        TA("タミル語"),
        TT("タタール語"),
        TE("テルグ語"),
        TH("タイ語"),
        TR("トルコ語"),
        TK("トルクメン語"),
        UK("ウクライナ語"),
        UR("ウルドゥー語"),
        UG("ウイグル語"),
        UZ("ウズベク語"),
        VI("ベトナム語"),
        CY("ウェールズ語"),
        XH("コーサ語"),
        YI("イディッシュ語"),
        YO("ヨルバ語"),
        ZU("ズールー語"),
        UNKNOWN(null);

        String language_name;

        Language(String language_name) {
            this.language_name = language_name;
        }

        @Override
        public String toString() {
            return language_name + "(" + name().replace("_", "-").toLowerCase(Locale.ROOT) + ")";
        }
    }
}
