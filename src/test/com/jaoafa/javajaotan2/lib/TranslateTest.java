package com.jaoafa.javajaotan2.lib;

import com.jaoafa.javajaotan2.Main;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TranslateTest {

    @Test
    void translateSimple() {
        try {
            Main.setConfig(new JavajaotanConfig());
        } catch (RuntimeException e) {
            e.printStackTrace();
            return;
        }
        Translate.TranslateResult result = Translate.translate(
            Translate.Language.EN,
            Translate.Language.JA,
            "Hello World."
        );
        assertNotNull(result);
        assertEquals("こんにちは世界。", result.result());
        assertEquals(Translate.Language.EN, result.from());
        assertEquals(Translate.Language.JA, result.to());
    }

    @Test
    void translateDetect() {
        try {
            Main.setConfig(new JavajaotanConfig());
        } catch (RuntimeException e) {
            e.printStackTrace();
            return;
        }
        Translate.TranslateResult result = Translate.translate(
            Translate.Language.UNKNOWN,
            Translate.Language.JA,
            "Hello World."
        );
        assertNotNull(result);
        assertEquals("こんにちは世界。", result.result());
        assertEquals(Translate.Language.EN, result.from());
        assertEquals(Translate.Language.JA, result.to());
    }
}