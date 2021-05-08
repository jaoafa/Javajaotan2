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

import java.text.SimpleDateFormat;
import java.util.Date;

public class JavajaotanLibrary {
    /**
     * 文字列がLong値に変換可能かどうか調べます
     *
     * @param str 調べる文字列
     *
     * @return Long値に変換可能かどうか
     */
    public static boolean isLong(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Dateをyyyy/MM/dd HH:mm:ss形式でフォーマットします。
     *
     * @param date フォーマットするDate
     *
     * @return フォーマットされた結果文字列
     */
    public static String sdfFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return sdf.format(date);
    }


    /**
     * DateをHH:mm:ss形式でフォーマットします。
     *
     * @param date フォーマットするDate
     *
     * @return フォーマットされた結果文字列
     */
    protected static String sdfTimeFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(date);
    }
}
