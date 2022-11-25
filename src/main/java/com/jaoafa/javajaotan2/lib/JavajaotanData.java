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

import com.rollbar.notifier.Rollbar;
import org.jetbrains.annotations.Nullable;

public class JavajaotanData {
    private static MySQLDBManager mainMySQLDBManager = null;
    private static MySQLDBManager zkrhatMySQLDBManager = null;
    private static Rollbar rollbar = null;

    public static MySQLDBManager getMainMySQLDBManager() {
        return mainMySQLDBManager;
    }

    public static void setMainMySQLDBManager(MySQLDBManager mainMySQLDBManager) {
        JavajaotanData.mainMySQLDBManager = mainMySQLDBManager;
    }

    public static MySQLDBManager getZkrhatMySQLDBManager() {
        return zkrhatMySQLDBManager;
    }

    public static void setZkrhatMySQLDBManager(MySQLDBManager zkrhatMySQLDBManager) {
        JavajaotanData.zkrhatMySQLDBManager = zkrhatMySQLDBManager;
    }

    @Nullable
    public static Rollbar getRollbar() {
        return rollbar;
    }

    public static void setRollbar(Rollbar rollbar) {
        JavajaotanData.rollbar = rollbar;
    }
}
