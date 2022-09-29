package com.jaoafa.javajaotan2.lib;

import com.jaoafa.javajaotan2.Main;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MinecraftPermGroupTest {
    MinecraftPermGroup mpg;

    @BeforeEach
    void setUp() throws SQLException {
        try {
            Main.setConfig(new JavajaotanConfig());
        } catch (RuntimeException e) {
            return; // CI用
        }
        mpg = new MinecraftPermGroup(UUID.fromString("32ff7cdc-a1b4-450a-aa7e-6af75fe8c37c"));
    }

    @Test
    void isFound() {
        if (mpg == null) {
            return; // CI用
        }
        assertTrue(mpg.isFound());
    }

    @Test
    void getGroup() {
        if (mpg == null) {
            return; // CI用
        }
        assertEquals(MinecraftPermGroup.Group.ADMIN, mpg.getGroup());
    }

    @Test
    void isTemporary() {
        if (mpg == null) {
            return; // CI用
        }
        assertFalse(mpg.isTemporary());
    }

    @Test
    void getExpiredAt() {
        if (mpg == null) {
            return; // CI用
        }
        assertNull(mpg.getExpiredAt());
    }
}