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

package com.jaoafa.javajaotan2.lib;

import com.jaoafa.javajaotan2.Main;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class JavajaotanConfig {
    final Logger logger;
    String token;
    long ownerId;
    long guildId;
    String gcpKey;
    String customSearchCX;
    String gasTranslateAPIUrl;
    String detectLanguageAPIToken;
    String githubAPIToken;
    String rollbarAccessToken;

    public JavajaotanConfig() throws RuntimeException {
        logger = Main.getLogger();

        File file = new File("config.json"); // カレントディレクトリの config.json をコンフィグファイルとして定義します
        if (!file.exists()) {
            logger.error("コンフィグファイル config.json が見つかりません。");
            throw new RuntimeException();
        }

        try {
            String json = String.join("\n", Files.readAllLines(file.toPath()));
            JSONObject config = new JSONObject(json);

            // - 必須項目の定義（ない場合、RuntimeExceptionが発生して進まない）
            requiredConfig(config, "token");

            // - 設定項目の取得
            token = config.getString("token");
            guildId = config.optLong("guild_id", 597378876556967936L);
            ownerId = config.optLong("owner_id", 221991565567066112L);
            gcpKey = config.optString("gcp_key");
            customSearchCX = config.optString("customSearchCX");
            gasTranslateAPIUrl = config.optString("gasTranslateAPIUrl");
            detectLanguageAPIToken = config.optString("detectLanguageAPIToken");
            githubAPIToken = config.optString("githubAPIToken");
            rollbarAccessToken = config.optString("rollbarAccessToken");

            // -- データベース関連
            if (config.has("main_database")) {
                JSONObject main_database = config.getJSONObject("main_database");
                String hostname = main_database.getString("hostname");
                int port = main_database.getInt("port");
                String username = main_database.getString("username");
                String password = main_database.getString("password");
                String dbname = main_database.getString("database");

                try {
                    JavajaotanData.setMainMySQLDBManager(new MySQLDBManager(hostname, port, username, password, dbname));
                } catch (ClassNotFoundException e) {
                    logger.warn("jaoMain データベース設定の初期化に失敗したため(ClassNotFoundException)、jaoMain データベースを使用する機能は使用できません。");
                }
            } else {
                logger.warn("jaoMain データベースへの接続設定が定義されていないため、jaoMain データベースを使用する機能は使用できません。");
            }

            if (config.has("zakurohat_database")) {
                JSONObject main_database = config.getJSONObject("main_database");
                String hostname = main_database.getString("hostname");
                int port = main_database.getInt("port");
                String username = main_database.getString("username");
                String password = main_database.getString("password");
                String dbname = main_database.getString("database");

                try {
                    JavajaotanData.setZkrhatMySQLDBManager(new MySQLDBManager(hostname, port, username, password, dbname));
                } catch (ClassNotFoundException e) {
                    logger.warn("ZakuroHat データベース設定の初期化に失敗したため(ClassNotFoundException)、ZakuroHat データベースを使用する機能は使用できません。");
                }
            } else {
                logger.warn("ZakuroHat データベースへの接続設定が定義されていないため、ZakuroHat データベースを使用する機能は使用できません。");
            }
        } catch (IOException e) {
            logger.warn("コンフィグファイル config.json を読み取れませんでした: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException();
        } catch (JSONException e) {
            logger.warn("コンフィグファイル config.json の JSON 形式が正しくありません: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    void requiredConfig(JSONObject config, String key) throws RuntimeException {
        if (config.has(key)) {
            return;
        }
        logger.warn(String.format("コンフィグファイル config.json で必須であるキーが見つかりません: %s", key));
        throw new RuntimeException();
    }

    public String getToken() {
        return token;
    }

    public long getGuildId() {
        return guildId;
    }

    public long getOwnerId() {
        return ownerId;
    }

    @Nullable
    public String getGCPKey() {
        return gcpKey;
    }

    @Nullable
    public String getCustomSearchCX() {
        return customSearchCX;
    }

    @Nullable
    public String getGASTranslateAPIUrl() {
        return gasTranslateAPIUrl;
    }

    @Nullable
    public String getDetectLanguageAPIToken() {
        return detectLanguageAPIToken;
    }

    @Nullable
    public String getGitHubAPIToken() {
        return githubAPIToken;
    }

    @Nullable
    public String getRollbarAccessToken() {
        return rollbarAccessToken;
    }
}
