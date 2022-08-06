package com.jaoafa.javajaotan2.lib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CommandArgumentTest {
    /**
     * シンプルな引数の解析テスト<br>
     * <br>
     * <code>arg1 arg2 arg3</code>という引数が与えられた場合、以下の条件が満たされることを確認する。
     *
     * <ul>
     *     <li>{@link CommandArgument#getArrayArgs()} が適切な配列を返すこと</li>
     *     <li>{@link CommandArgument#getListArgs()} が適切なリストを返すこと</li>
     *     <li>{@link CommandArgument#getStreamArgs()} が適切なストリームを返すこと</li>
     *     <li>{@link CommandArgument#has(int)} が適切な値を返すこと</li>
     *     <li>{@link CommandArgument#size()} が適切な値を返すこと</li>
     *     <li>各引数において {@link CommandArgument#getString(int)} が適切な値を返すこと</li>
     *     <li>引数範囲外のインデックスが指定された場合にIllegalArgumentExceptionが発生すること</li>
     * </ul>
     */
    @Test
    void simpleParseTest() {
        CommandArgument args = new CommandArgument("arg1 arg2 arg3");
        Assertions.assertArrayEquals(args.getArrayArgs(), new String[]{"arg1", "arg2", "arg3"});
        Assertions.assertLinesMatch(args.getListArgs(), List.of("arg1", "arg2", "arg3"));
        Assertions.assertLinesMatch(args.getStreamArgs().toList(), List.of("arg1", "arg2", "arg3"));
        Assertions.assertTrue(args.has(0));
        Assertions.assertTrue(args.has(1));
        Assertions.assertTrue(args.has(2));
        Assertions.assertFalse(args.has(3));
        Assertions.assertEquals(args.size(), 3);
        Assertions.assertEquals(args.getString(0), "arg1");
        Assertions.assertEquals(args.getString(1), "arg2");
        Assertions.assertEquals(args.getString(2), "arg3");

        // 引数の範囲外インデックスを指定した場合はIllegalArgumentExceptionが発生しなければならない
        Assertions.assertThrows(IllegalArgumentException.class, () -> args.getString(3));
    }

    /**
     * 一部がクォートされた引数の解析テスト<br>
     * <br>
     * <code>arg1 "a r g 2" arg3</code>という引数が与えられた場合、以下の条件が満たされることを確認する。
     *
     * <ul>
     *     <li>{@link CommandArgument#getArrayArgs()} が適切な配列を返すこと</li>
     *     <li>{@link CommandArgument#size()} が適切な値を返すこと</li>
     *     <li>各引数において {@link CommandArgument#getString(int)} が適切な値を返すこと</li>
     *     <li>引数範囲外のインデックスが指定された場合にIllegalArgumentExceptionが発生すること</li>
     * </ul>
     */
    @Test
    void quotedParseTest() {
        CommandArgument args = new CommandArgument("arg1 \"a r g 2\" arg3");
        Assertions.assertArrayEquals(args.getArrayArgs(), new String[]{"arg1", "a r g 2", "arg3"});
        Assertions.assertEquals(args.size(), 3);
        Assertions.assertEquals(args.getString(0), "arg1");
        Assertions.assertEquals(args.getString(1), "a r g 2");
        Assertions.assertEquals(args.getString(2), "arg3");

        // 引数の範囲外インデックスを指定した場合はIllegalArgumentExceptionが発生しなければならない
        Assertions.assertThrows(IllegalArgumentException.class, () -> args.getString(3));
    }

    /**
     * クォートされていない、ダブルクォーテーションの含まれた引数の解析テスト<br>
     * <br>
     * <code>arg1 \"arg2 arg3</code>という引数が与えられた場合、以下の条件が満たされることを確認する。
     *
     * <ul>
     *     <li>{@link CommandArgument#getArrayArgs()} が適切な配列を返すこと</li>
     *     <li>{@link CommandArgument#size()} が適切な値を返すこと</li>
     *     <li>各引数において {@link CommandArgument#getString(int)} が適切な値を返すこと</li>
     *     <li>引数範囲外のインデックスが指定された場合にIllegalArgumentExceptionが発生すること</li>
     * </ul>
     */
    @Test
    void invalidQuotedTest() {
        CommandArgument args = new CommandArgument("arg1 \"arg2 arg3");
        Assertions.assertArrayEquals(args.getArrayArgs(), new String[]{"arg1", "\"arg2", "arg3"});
        Assertions.assertEquals(args.size(), 3);
        Assertions.assertEquals(args.getString(0), "arg1");
        Assertions.assertEquals(args.getString(1), "\"arg2");
        Assertions.assertEquals(args.getString(2), "arg3");

        // 引数の範囲外インデックスを指定した場合はIllegalArgumentExceptionが発生しなければならない
        Assertions.assertThrows(IllegalArgumentException.class, () -> args.getString(3));
    }

    /**
     * greedyな引数を取得するテスト<br>
     * <br>
     * <code>a r g u m e n t</code>という引数が与えられ、引数インデックス 0 以降を取得する{@link CommandArgument#getGreedyString(int)}を呼び出す場合、以下の条件が満たされることを確認する。
     *
     * <ul>
     *     <li>{@link CommandArgument#getGreedyString(int)} が適切な文字列を返すこと</li>
     * </ul>
     */
    @Test
    void greedyStringTest() {
        CommandArgument args = new CommandArgument("a r g u m e n t");
        Assertions.assertEquals(args.getGreedyString(0), "a r g u m e n t");
    }

    /**
     * 名前付き引数を取得するテスト<br>
     * <br>
     * <code>arg1 arg2 arg3</code>という引数が与えられ、名前付き引数として<code>a, b, c</code>が定義されている場合、以下の条件が満たされることを確認する。
     *
     * <ul>
     *     <li>{@link CommandArgument#getArrayArgs()} が適切な配列を返すこと</li>
     *     <li>{@link CommandArgument#size()} が適切な値を返すこと</li>
     *     <li>各引数において {@link CommandArgument#getString(String)} が適切な値を返すこと</li>
     *     <li>定義されていないキーが指定された場合にIllegalArgumentExceptionが発生すること</li>
     * </ul>
     */
    @Test
    void simpleNamedTest() {
        CommandArgument args = new CommandArgument("arg1 arg2 arg3", List.of("a", "b", "c"));
        Assertions.assertArrayEquals(args.getArrayArgs(), new String[]{"arg1", "arg2", "arg3"});
        Assertions.assertEquals(args.size(), 3);
        Assertions.assertEquals(args.getString("a"), "arg1");
        Assertions.assertEquals(args.getString("b"), "arg2");
        Assertions.assertEquals(args.getString("c"), "arg3");

        // 規定されていないキーを指定した場合はIllegalArgumentExceptionが発生しなければならない
        Assertions.assertThrows(IllegalArgumentException.class, () -> args.getString("d"));
    }

    /**
     * greedyな名前付き引数を取得するテスト<br>
     * <br>
     * <code>a r g u m e n t</code>という引数が与えられ、名前付き引数として<code>a b...</code>が定義されている場合、以下の条件が満たされることを確認する。
     *
     * <ul>
     *     <li>{@link CommandArgument#getString(String)} において、<code>b...</code>を指定した場合に<code>a r g u m e n t</code>が返ること</li>
     * </ul>
     */
    @Test
    void greedyNamedTest() {
        CommandArgument args = new CommandArgument("a r g u m e n t", List.of("a", "b..."));
        Assertions.assertEquals(args.getString("b..."), "a r g u m e n t");
    }

    /**
     * <code>get***(int)</code>メソッドのテスト<br>
     * <br>
     * <code>string 0 2147483648 1.0 true false</code>という引数が与えられた場合、以下の条件が満たされることを確認する。
     *
     * <ul>
     *     <li>{@link CommandArgument#getString(int)} が適切な値を返すこと</li>
     *     <li>{@link CommandArgument#getInt(int)} が適切な値を返すこと</li>
     *     <li>{@link CommandArgument#getLong(int)} が適切な値を返すこと</li>
     *     <li>{@link CommandArgument#getDouble(int)} が適切な値を返すこと</li>
     *     <li>{@link CommandArgument#getBoolean(int)} が適切な値を返すこと</li>
     *     <li>{@link CommandArgument#getInt(int)} において、intとして扱えない値の場合にNumberFormatExceptionが発生すること</li>
     *     <li>{@link CommandArgument#getLong(int)} において、longとして扱えない値の場合にNumberFormatExceptionが発生すること</li>
     *     <li>{@link CommandArgument#getDouble(int)} において、doubleとして扱えない値の場合にNumberFormatExceptionが発生すること</li>
     *     <li>{@link CommandArgument#getBoolean(int)} において、booleanとして扱えない値の場合にIllegalArgumentExceptionが発生すること</li>
     * </ul>
     */
    @Test
    void specifiedTypeTest() {
        CommandArgument args = new CommandArgument("string 0 2147483648 1.0 true false");
        Assertions.assertEquals(args.getString(0), "string");
        Assertions.assertEquals(args.getInt(1), 0);
        Assertions.assertEquals(args.getLong(2), 2147483648L);
        Assertions.assertEquals(args.getDouble(3), 1.0);
        Assertions.assertTrue(args.getBoolean(4));
        Assertions.assertFalse(args.getBoolean(5));

        // int, long, doubleとして扱えない値の場合にはNumberFormatExceptionが発生しなければならない

        // インデックス 0,2,4,5 の値はintとして扱えない
        for (int i : List.of(0, 2, 4, 5)) {
            Assertions.assertThrows(NumberFormatException.class, () -> args.getInt(i));
        }

        // インデックス 0,3,4,5 の値はlongとして扱えない
        for (int i : List.of(0, 3, 4, 5)) {
            Assertions.assertThrows(NumberFormatException.class, () -> args.getLong(i));
        }

        // インデックス 0,2,4,5 の値はdoubleとして扱えない
        for (int i : List.of(0, 1, 2, 4, 5)) {
            Assertions.assertThrows(NumberFormatException.class, () -> args.getDouble(i));
        }

        // booleanではない値の場合にはIllegalArgumentExceptionが発生しなければならない
        // インデックス 0,1,2,3 の値はbooleanとして扱えない
        for (int i : List.of(0, 1, 2, 3)) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> args.getBoolean(i));
        }
    }

    /**
     * <code>get***(String)</code>メソッドのテスト<br>
     * <br>
     * <code>string 0 2147483648 1.0 true false</code>という引数が与えられ、名前付き引数として<code>string, int, long, double, boolean1, boolean2</code>が定義されている場合、以下の条件が満たされることを確認する。
     *
     * <ul>
     *     <li>{@link CommandArgument#getString(int)} が適切な値を返すこと</li>
     *     <li>{@link CommandArgument#getInt(int)} が適切な値を返すこと</li>
     *     <li>{@link CommandArgument#getLong(int)} が適切な値を返すこと</li>
     *     <li>{@link CommandArgument#getDouble(int)} が適切な値を返すこと</li>
     *     <li>{@link CommandArgument#getBoolean(int)} が適切な値を返すこと</li>
     *     <li>{@link CommandArgument#getInt(int)} において、intとして扱えない値の場合にNumberFormatExceptionが発生すること</li>
     *     <li>{@link CommandArgument#getLong(int)} において、longとして扱えない値の場合にNumberFormatExceptionが発生すること</li>
     *     <li>{@link CommandArgument#getDouble(int)} において、doubleとして扱えない値の場合にNumberFormatExceptionが発生すること</li>
     *     <li>{@link CommandArgument#getBoolean(int)} において、booleanとして扱えない値の場合にIllegalArgumentExceptionが発生すること</li>
     * </ul>
     */
    @Test
    void specifiedTypeNamedTest() {
        CommandArgument args = new CommandArgument("string 0 2147483648 1.0 true false", List.of("string", "int", "long", "double", "boolean1", "boolean2"));
        Assertions.assertEquals(args.getString("string"), "string");
        Assertions.assertEquals(args.getInt("int"), 0);
        Assertions.assertEquals(args.getLong("long"), 2147483648L);
        Assertions.assertEquals(args.getDouble("double"), 1.0);
        Assertions.assertTrue(args.getBoolean("boolean1"));
        Assertions.assertFalse(args.getBoolean("boolean2"));

        // int, long, doubleとして扱えない値の場合にはNumberFormatExceptionが発生しなければならない

        // キー string,double,boolean1,boolean2 の値はintとして扱えない
        for (String s : List.of("string", "double", "boolean1", "boolean2")) {
            Assertions.assertThrows(NumberFormatException.class, () -> args.getInt(s));
        }

        // キー string,double,boolean1,boolean2 の値はlongとして扱えない
        for (String s : List.of("string", "double", "boolean1", "boolean2")) {
            Assertions.assertThrows(NumberFormatException.class, () -> args.getLong(s));
        }

        // キー string,long,boolean1,boolean2 の値はdoubleとして扱えない
        for (String s : List.of("string", "long", "boolean1", "boolean2")) {
            Assertions.assertThrows(NumberFormatException.class, () -> args.getDouble(s));
        }

        // booleanではない値の場合にはIllegalArgumentExceptionが発生しなければならない
        // キー string,int,long,double の値はbooleanとして扱えない
        for (String s : List.of("string", "int", "long", "double")) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> args.getBoolean(s));
        }
    }

    /**
     * オプショナルな引数を取得するメソッド <code>getOptional***(int)</code> のテスト<br>
     * <br>
     * <code>string 0 2147483648 1.0 true false</code>という引数が与えられた場合、以下の条件が満たされることを確認する。
     *
     * <ul>
     *     <li>各 <code>getOptional***</code> メソッドにおいて、存在するインデックスをオプショナルで取得した場合インデックスの本来の値が取得できること</li>
     *     <li>各 <code>getOptional***</code> メソッドにおいて、存在しないインデックスをオプショナルで取得した場合、defaultValueの値が取得できること</li>
     *     <li>{@link CommandArgument#getOptionalInt(int, int)} において、intとして扱えない値の場合にNumberFormatExceptionが発生すること</li>
     *     <li>{@link CommandArgument#getOptionalLong(int, long)} において、longとして扱えない値の場合にNumberFormatExceptionが発生すること</li>
     *     <li>{@link CommandArgument#getOptionalDouble(int, double)} において、doubleとして扱えない値の場合にNumberFormatExceptionが発生すること</li>
     *     <li>{@link CommandArgument#getOptionalBoolean(int, boolean)} において、booleanとして扱えない値の場合にIllegalArgumentExceptionが発生すること</li>
     *
     * </ul>
     */
    @Test
    void optionalArgumentTest() {
        CommandArgument args = new CommandArgument("string 0 2147483648 1.0 true false");

        // 存在するインデックスの値を取得
        Assertions.assertEquals(args.getOptionalString(0, "default"), "string");
        Assertions.assertEquals(args.getOptionalInt(1, -1), 0);
        Assertions.assertEquals(args.getOptionalLong(2, -2147483648L), 2147483648L);
        Assertions.assertEquals(args.getOptionalDouble(3, -1.0), 1.0);
        Assertions.assertTrue(args.getOptionalBoolean(4, false));
        Assertions.assertFalse(args.getOptionalBoolean(5, true));

        // 存在しないインデックスの値を取得
        Assertions.assertEquals(args.getOptionalString(6, "default"), "default");
        Assertions.assertEquals(args.getOptionalInt(6, -1), -1);
        Assertions.assertEquals(args.getOptionalLong(6, -2147483648L), -2147483648L);
        Assertions.assertEquals(args.getOptionalDouble(6, -1.0), -1.0);
        Assertions.assertFalse(args.getOptionalBoolean(6, false));
        Assertions.assertTrue(args.getOptionalBoolean(6, true));

        // int, long, doubleとして扱えない値の場合にはNumberFormatExceptionが発生しなければならない

        // インデックス 0,2,4,5 の値はintとして扱えない
        for (int i : List.of(0, 2, 4, 5)) {
            Assertions.assertThrows(NumberFormatException.class, () -> args.getOptionalInt(i, -1));
        }

        // インデックス 0,3,4,5 の値はlongとして扱えない
        for (int i : List.of(0, 3, 4, 5)) {
            Assertions.assertThrows(NumberFormatException.class, () -> args.getOptionalLong(i, -2147483648L));
        }

        // インデックス 0,2,4,5 の値はdoubleとして扱えない
        for (int i : List.of(0, 1, 2, 4, 5)) {
            Assertions.assertThrows(NumberFormatException.class, () -> args.getOptionalDouble(i, -1.0));
        }

        // booleanではない値の場合にはIllegalArgumentExceptionが発生しなければならない
        // インデックス 0,1,2,3 の値はbooleanとして扱えない
        for (int i : List.of(0, 1, 2, 3)) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> args.getOptionalBoolean(i, false));
        }
    }

    /**
     * 名前付きのオプショナルな引数を取得するメソッド <code>getOptional***(String)</code> のテスト<br>
     * <br>
     * <code>string 0 2147483648 1.0 true false</code>という引数が与えられ、名前付き引数として<code>string, int, long, double, boolean1, boolean2</code>が定義されている場合、以下の条件が満たされることを確認する。
     *
     * <ul>
     *     <li>各 <code>getOptional***</code> メソッドにおいて、存在するインデックスをオプショナルで取得した場合インデックスの本来の値が取得できること</li>
     *     <li>各 <code>getOptional***</code> メソッドにおいて、存在しないインデックスをオプショナルで取得した場合、defaultValueの値が取得できること</li>
     *     <li>{@link CommandArgument#getOptionalInt(int, int)} において、intとして扱えない値の場合にNumberFormatExceptionが発生すること</li>
     *     <li>{@link CommandArgument#getOptionalLong(int, long)} において、longとして扱えない値の場合にNumberFormatExceptionが発生すること</li>
     *     <li>{@link CommandArgument#getOptionalDouble(int, double)} において、doubleとして扱えない値の場合にNumberFormatExceptionが発生すること</li>
     *     <li>{@link CommandArgument#getOptionalBoolean(int, boolean)} において、booleanとして扱えない値の場合にIllegalArgumentExceptionが発生すること</li>
     *
     * </ul>
     */
    @Test
    void optionalNamedArgumentTest() {
        CommandArgument args = new CommandArgument("string 0 2147483648 1.0 true false", List.of("string", "int", "long", "double", "boolean1", "boolean2"));

        // 存在するインデックスの値を取得
        Assertions.assertEquals(args.getOptionalString("string", "default"), "string");
        Assertions.assertEquals(args.getOptionalInt("int", -1), 0);
        Assertions.assertEquals(args.getOptionalLong("long", -2147483648L), 2147483648L);
        Assertions.assertEquals(args.getOptionalDouble("double", -1.0), 1.0);
        Assertions.assertTrue(args.getOptionalBoolean("boolean1", false));
        Assertions.assertFalse(args.getOptionalBoolean("boolean2", true));

        // 存在しないインデックスの値を取得
        Assertions.assertEquals(args.getOptionalString("nothing", "default"), "default");
        Assertions.assertEquals(args.getOptionalInt("nothing", -1), -1);
        Assertions.assertEquals(args.getOptionalLong("nothing", -2147483648L), -2147483648L);
        Assertions.assertEquals(args.getOptionalDouble("nothing", -1.0), -1.0);
        Assertions.assertFalse(args.getOptionalBoolean("nothing", false));
        Assertions.assertTrue(args.getOptionalBoolean("nothing", true));

        // int, long, doubleとして扱えない値の場合にはNumberFormatExceptionが発生しなければならない

        // インデックス 0,2,4,5 の値はintとして扱えない
        // キー string,double,boolean1,boolean2 の値はintとして扱えない
        for (String s : List.of("string", "double", "boolean1", "boolean2")) {
            Assertions.assertThrows(NumberFormatException.class, () -> args.getOptionalInt(s, -1));
        }

        // キー string,double,boolean1,boolean2 の値はlongとして扱えない
        for (String s : List.of("string", "double", "boolean1", "boolean2")) {
            Assertions.assertThrows(NumberFormatException.class, () -> args.getOptionalLong(s, -2147483648L));
        }

        // キー string,long,boolean1,boolean2 の値はdoubleとして扱えない
        for (String s : List.of("string", "long", "boolean1", "boolean2")) {
            Assertions.assertThrows(NumberFormatException.class, () -> args.getOptionalDouble(s, -1.0));
        }

        // booleanではない値の場合にはIllegalArgumentExceptionが発生しなければならない
        // キー string,int,long,double の値はbooleanとして扱えない
        for (String s : List.of("string", "int", "long", "double")) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> args.getOptionalBoolean(s, false));
        }
    }
}
