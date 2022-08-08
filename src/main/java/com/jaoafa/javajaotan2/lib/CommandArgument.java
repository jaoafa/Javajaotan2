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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class CommandArgument {
    private final String[] args;
    private final List<String> argNames;

    public CommandArgument(String command) {
        this.args = parseArgument(command);
        this.argNames = Collections.emptyList();
    }

    public CommandArgument(String command, List<String> argNames) {
        this.args = parseArgument(command);
        this.argNames = argNames;
    }

    private String[] parseArgument(String str) {
        List<String> args = new ArrayList<>();
        boolean isQuoting = false;
        StringBuilder quoting = new StringBuilder();
        List<String> rawArgs = Arrays.stream(str.split(" "))
            .filter(s -> !s.isEmpty())
            .toList();
        for (int i = 0; i < rawArgs.size(); i++) {
            String arg = rawArgs.get(i);
            if (!isQuoting && arg.startsWith("\"")) {
                if (arg.endsWith("\"")) {
                    args.add(arg.substring(1, arg.length() - 1));
                } else if (rawArgs.stream().skip(i + 1).noneMatch(s -> s.endsWith("\""))) {
                    args.add(arg);
                } else {
                    isQuoting = true;
                    quoting.append(arg.substring(1));
                }
            } else if (isQuoting) {
                if (arg.endsWith("\"")) {
                    quoting.append(" ");
                    quoting.append(arg, 0, arg.length() - 1);
                    args.add(quoting.toString());
                    quoting.setLength(0);
                    isQuoting = false;
                } else {
                    quoting.append(" ");
                    quoting.append(arg);
                }
            } else {
                args.add(arg);
            }
        }
        if (isQuoting || quoting.length() > 0) {
            args.add(quoting.toString());
        }
        return args.toArray(new String[0]);
    }

    /**
     * 指定されたキーに対応するインデックスの値をStringとして返します。<br>
     * キーが「...」で終わる場合、greedyなキーと判断し対応するインデックス以降の値をStringとして返します。
     *
     * @param key キー
     *
     * @return キーに対応するインデックスの値
     *
     * @throws IllegalArgumentException 指定されたキーが存在しない場合、またはキーに対応するインデックスが不適切な場合
     * @see #getString(int)
     * @see #getGreedyString(int)
     * @see #getOptionalString(String, String)
     * @see #getOptionalString(int, String)
     */
    public String getString(String key) {
        if (!argNames.contains(key)) {
            throw new IllegalArgumentException("Argument name not found: " + key);
        }
        if (key.endsWith("...")) {
            return getGreedyString(argNames.indexOf(key));
        }
        return getString(argNames.indexOf(key));
    }

    /**
     * 指定されたインデックスの値をStringとして返します。
     *
     * @param index 取得するインデックス
     *
     * @return 指定されたインデックスの値
     *
     * @throws IllegalArgumentException 指定されたインデックスが不適切の場合（存在しない）
     * @see #getString(String)
     * @see #getOptionalString(int, String)
     * @see #getOptionalString(String, String)
     */
    public String getString(int index) {
        if (index < 0 || index >= args.length) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }
        return args[index];
    }

    /**
     * 引数配列から、指定されたインデックス以降の値をStringとして返します。
     *
     * @param startIndex 取得開始するインデックス
     *
     * @return 指定されたインデックス以降の値
     */
    public String getGreedyString(int startIndex) {
        if (startIndex < 0 || startIndex >= args.length) {
            throw new IllegalArgumentException("Index out of bounds: " + startIndex);
        }
        return Stream.of(args)
            .skip(startIndex)
            .reduce((a, b) -> a + " " + b)
            .orElse("");
    }

    /**
     * 指定されたキーに対応するインデックスの値をIntegerとして返します。
     *
     * @param key キー
     *
     * @return キーに対応するインデックスの値
     *
     * @throws IllegalArgumentException 指定されたキーが存在しない場合、またはキーに対応するインデックスが不適切な場合
     * @throws NumberFormatException    指定されたインデックスの値をintに変換できない場合
     * @see #getInt(int)
     * @see #getOptionalInt(String, int)
     * @see #getOptionalInt(int, int)
     */
    public int getInt(String key) {
        if (!argNames.contains(key)) {
            throw new IllegalArgumentException("Argument name not found: " + key);
        }
        return getInt(argNames.indexOf(key));
    }

    /**
     * 指定されたインデックスの値をintとして返します。
     *
     * @param index 取得するインデックス
     *
     * @return 指定されたインデックスの値
     *
     * @throws IllegalArgumentException 指定されたインデックスが不適切の場合（存在しない）
     * @throws NumberFormatException    指定されたインデックスの値をintに変換できない場合
     * @see #getInt(String)
     * @see #getOptionalInt(int, int)
     * @see #getOptionalInt(String, int)
     */
    public int getInt(int index) {
        if (index < 0 || index >= args.length) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }
        return Integer.parseInt(args[index]);
    }

    /**
     * 指定されたキーに対応するインデックスの値をLongとして返します。
     *
     * @param key キー
     *
     * @return キーに対応するインデックスの値
     *
     * @throws IllegalArgumentException 指定されたキーが存在しない場合、またはキーに対応するインデックスが不適切な場合
     * @throws NumberFormatException    指定されたインデックスの値をlongに変換できない場合
     * @see #getLong(int)
     * @see #getOptionalLong(String, long)
     * @see #getOptionalLong(int, long)
     */
    public long getLong(String key) {
        if (!argNames.contains(key)) {
            throw new IllegalArgumentException("Argument name not found: " + key);
        }
        return getLong(argNames.indexOf(key));
    }

    /**
     * 指定されたインデックスの値をlongとして返します。
     *
     * @param index 取得するインデックス
     *
     * @return 指定されたインデックスの値
     *
     * @throws IllegalArgumentException 指定されたインデックスが不適切の場合（存在しない）
     * @throws NumberFormatException    指定されたインデックスの値をlongに変換できない場合
     * @see #getLong(String)
     * @see #getOptionalLong(int, long)
     * @see #getOptionalLong(String, long)
     */
    public long getLong(int index) {
        if (index < 0 || index >= args.length) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }
        return Long.parseLong(args[index]);
    }

    /**
     * 指定されたキーに対応するインデックスの値をDoubleとして返します。
     *
     * @param key キー
     *
     * @return キーに対応するインデックスの値
     *
     * @throws IllegalArgumentException 指定されたキーが存在しない場合、またはキーに対応するインデックスが不適切な場合
     * @throws NumberFormatException    指定されたインデックスの値をdoubleに変換できない場合
     * @see #getDouble(int)
     * @see #getOptionalDouble(String, double)
     * @see #getOptionalDouble(int, double)
     */
    public double getDouble(String key) {
        if (!argNames.contains(key)) {
            throw new IllegalArgumentException("Argument name not found: " + key);
        }
        return getDouble(argNames.indexOf(key));
    }

    /**
     * 指定されたインデックスの値をdoubleとして返します。
     *
     * @param index 取得するインデックス
     *
     * @return 指定されたインデックスの値
     *
     * @throws IllegalArgumentException 指定されたインデックスが不適切の場合（存在しない）
     * @throws NumberFormatException    指定されたインデックスの値をdoubleに変換できない場合
     * @see #getDouble(String)
     * @see #getOptionalDouble(int, double)
     * @see #getOptionalDouble(String, double)
     */
    public double getDouble(int index) {
        if (index < 0 || index >= args.length) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }
        return Double.parseDouble(args[index]);
    }

    /**
     * 指定されたキーに対応するインデックスの値をBooleanとして返します。
     *
     * @param key キー
     *
     * @return キーに対応するインデックスの値
     *
     * @throws IllegalArgumentException 指定されたインデックスが不適切の場合（存在しない）
     * @see #getBoolean(int)
     * @see #getOptionalBoolean(String, boolean)
     * @see #getOptionalBoolean(int, boolean)
     */
    public boolean getBoolean(String key) {
        if (!argNames.contains(key)) {
            throw new IllegalArgumentException("Argument name not found: " + key);
        }
        return getBoolean(argNames.indexOf(key));
    }

    /**
     * 指定されたインデックスの値をbooleanとして返します。
     *
     * @param index 取得するインデックス
     *
     * @return 指定されたインデックスの値
     *
     * @throws IllegalArgumentException 指定されたインデックスが不適切の場合（存在しない、変換できない）
     * @see #getBoolean(String)
     * @see #getOptionalBoolean(int, boolean)
     * @see #getOptionalBoolean(String, boolean)
     */
    public boolean getBoolean(int index) {
        if (index < 0 || index >= args.length) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }
        if (args[index].equalsIgnoreCase("true")) {
            return true;
        } else if (args[index].equalsIgnoreCase("false")) {
            return false;
        } else {
            throw new IllegalArgumentException("Invalid boolean: " + args[index]);
        }
    }

    /**
     * 指定されたキーに対応するインデックスの値をStringとして返します。<br>
     * 値がない場合は指定されたデフォルト値を返します。
     *
     * @param key          キー
     * @param defaultValue 値がない場合に返しますデフォルト値
     *
     * @return 指定されたインデックスの値、またはデフォルト値
     *
     * @throws IllegalArgumentException 指定されたキーが存在しない場合
     * @see #getString(int)
     * @see #getString(String)
     * @see #getOptionalString(int, String)
     */
    public String getOptionalString(String key, String defaultValue) {
        return getOptionalString(argNames.indexOf(key), defaultValue);
    }

    /**
     * 指定されたインデックスの値をStringとして返します。<br>
     * 値がない場合は指定されたデフォルト値を返します。
     *
     * @param index        取得するインデックス
     * @param defaultValue 値がない場合に返しますデフォルト値
     *
     * @return 指定されたインデックスの値、またはデフォルト値
     *
     * @see #getString(int)
     * @see #getString(String)
     * @see #getOptionalString(String, String)
     */
    public String getOptionalString(int index, String defaultValue) {
        return args.length > index ? args[index] : defaultValue;
    }

    /**
     * 指定されたキーに対応するインデックスの値をintとして返します。<br>
     * 値がない場合は指定されたデフォルト値を返します。
     *
     * @param key          キー
     * @param defaultValue 値がない場合に返しますデフォルト値
     *
     * @return 指定されたインデックスの値、またはデフォルト値
     *
     * @throws IllegalArgumentException 指定されたキーが存在しない場合
     * @see #getInt(int)
     * @see #getInt(String)
     * @see #getOptionalInt(int, int)
     */
    public int getOptionalInt(String key, int defaultValue) {
        if (!argNames.contains(key)) {
            throw new IllegalArgumentException("Argument name not found: " + key);
        }
        return getOptionalInt(argNames.indexOf(key), defaultValue);
    }

    /**
     * 指定されたインデックスの値をintとして返します。<br>
     * 値がない場合は指定されたデフォルト値を返します。
     *
     * @param index        取得するインデックス
     * @param defaultValue 値がない場合に返しますデフォルト値
     *
     * @return 指定されたインデックスの値、またはデフォルト値
     *
     * @see #getInt(int)
     * @see #getInt(String)
     * @see #getOptionalInt(String, int)
     */
    public int getOptionalInt(int index, int defaultValue) {
        return args.length > index ? getInt(index) : defaultValue;
    }

    /**
     * 指定されたキーに対応するインデックスの値をlongとして返します。<br>
     * 値がない場合は指定されたデフォルト値を返します。
     *
     * @param key          キー
     * @param defaultValue 値がない場合に返しますデフォルト値
     *
     * @return 指定されたインデックスの値、またはデフォルト値
     *
     * @throws IllegalArgumentException 指定されたキーが存在しない場合
     * @see #getLong(int)
     * @see #getLong(String)
     * @see #getOptionalLong(int, long)
     */
    public long getOptionalLong(String key, long defaultValue) {
        if (!argNames.contains(key)) {
            throw new IllegalArgumentException("Argument name not found: " + key);
        }
        return getOptionalLong(argNames.indexOf(key), defaultValue);
    }

    /**
     * 指定されたインデックスの値をlongとして返します。<br>
     * 値がない場合は指定されたデフォルト値を返します。
     *
     * @param index        取得するインデックス
     * @param defaultValue 値がない場合に返しますデフォルト値
     *
     * @return 指定されたインデックスの値、またはデフォルト値
     *
     * @see #getLong(int)
     * @see #getLong(String)
     * @see #getOptionalLong(String, long)
     */
    public long getOptionalLong(int index, long defaultValue) {
        return args.length > index ? getLong(index) : defaultValue;
    }

    /**
     * 指定されたキーに対応するインデックスの値をdoubleとして返します。<br>
     * 値がない場合は指定されたデフォルト値を返します。
     *
     * @param key          キー
     * @param defaultValue 値がない場合に返しますデフォルト値
     *
     * @return 指定されたインデックスの値、またはデフォルト値
     *
     * @throws IllegalArgumentException 指定されたキーが存在しない場合
     * @see #getDouble(int)
     * @see #getDouble(String)
     * @see #getOptionalDouble(int, double)
     */
    public double getOptionalDouble(String key, double defaultValue) {
        if (!argNames.contains(key)) {
            throw new IllegalArgumentException("Argument name not found: " + key);
        }
        return getOptionalDouble(argNames.indexOf(key), defaultValue);
    }

    /**
     * 指定されたインデックスの値をdoubleとして返します。<br>
     * 値がない場合は指定されたデフォルト値を返します。
     *
     * @param index        取得するインデックス
     * @param defaultValue 値がない場合に返しますデフォルト値
     *
     * @return 指定されたインデックスの値、またはデフォルト値
     *
     * @see #getDouble(int)
     * @see #getDouble(String)
     * @see #getOptionalDouble(String, double)
     */
    public double getOptionalDouble(int index, double defaultValue) {
        return args.length > index ? getDouble(index) : defaultValue;
    }

    /**
     * 指定されたキーに対応するインデックスの値をbooleanとして返します。<br>
     * 値がない場合は指定されたデフォルト値を返します。
     *
     * @param key          キー
     * @param defaultValue 値がない場合に返しますデフォルト値
     *
     * @return 指定されたインデックスの値、またはデフォルト値
     *
     * @throws IllegalArgumentException 指定されたキーが存在しない場合
     * @see #getBoolean(int)
     * @see #getBoolean(String)
     * @see #getOptionalBoolean(int, boolean)
     */
    public boolean getOptionalBoolean(String key, boolean defaultValue) {
        if (!argNames.contains(key)) {
            throw new IllegalArgumentException("Argument name not found: " + key);
        }
        return getOptionalBoolean(argNames.indexOf(key), defaultValue);
    }

    /**
     * 指定されたインデックスの値をbooleanとして返します。<br>
     * 値がない場合は指定されたデフォルト値を返します。
     *
     * @param index        取得するインデックス
     * @param defaultValue 値がない場合に返しますデフォルト値
     *
     * @return 指定されたインデックスの値、またはデフォルト値
     *
     * @see #getBoolean(int)
     * @see #getBoolean(String)
     * @see #getOptionalBoolean(String, boolean)
     */
    public boolean getOptionalBoolean(int index, boolean defaultValue) {
        if (args.length <= index) {
            return defaultValue;
        }
        return getBoolean(index);
    }

    /**
     * 引数の配列を返します。
     *
     * @return 引数の配列
     *
     * @see #getListArgs()
     * @see #getStreamArgs()
     */
    public String[] getArrayArgs() {
        return args;
    }

    /**
     * 引数のリストを返します。
     *
     * @return 引数のリスト
     *
     * @see #getArrayArgs()
     * @see #getStreamArgs()
     */
    public List<String> getListArgs() {
        return Arrays.asList(args);
    }

    /**
     * 引数のストリームを返します。
     *
     * @return 引数のストリーム
     *
     * @see #getArrayArgs()
     * @see #getListArgs()
     */
    public Stream<String> getStreamArgs() {
        return Arrays.stream(args);
    }

    /**
     * 引数の数を返します。
     *
     * @return 引数の数
     */
    public int size() {
        return args.length;
    }

    /**
     * 引数が存在するかどうかを返します。
     *
     * @return 引数が存在する場合はtrue、そうでない場合はfalse
     */
    public boolean has(int index) {
        return args.length > index;
    }
}
