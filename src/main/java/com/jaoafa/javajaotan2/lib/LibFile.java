package com.jaoafa.javajaotan2.lib;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class LibFile {
    /**
     * 対象ファイルの内容を読み込みます
     *
     * @param path 読み込むファイルパス
     * @return 対象ファイルの内容
     */
    public static String read(@NotNull String path) {
        String index = null;
        try {
            List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
            index = String.join("\n", lines);
        } catch (IOException e) {
            e.printStackTrace();
            //todo: LibReporter.report(e);
        }
        return index;
    }

    /**
     * 対象ファイルに内容を書き込みます
     *
     * @param path    書き込むファイルパス
     * @param content 書き込む内容
     * @return 成功したかどうか
     */
    public static boolean write(String path, String content) {
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(path)))) {
            pw.println(content);
        } catch (IOException e) {
            //todo: LibReporter.report(e);
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
