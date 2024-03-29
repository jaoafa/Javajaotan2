package com.jaoafa.javajaotan2.lib;

import com.jaoafa.javajaotan2.Main;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("FieldCanBeLocal")
class DiscordMinecraftLinkTest {
    private final String minecraftName = "mine_book000";
    private final UUID minecraftUUID = UUID.fromString("32ff7cdc-a1b4-450a-aa7e-6af75fe8c37c");
    private final String discordName = "tomachi";
    private final String discordDiscriminator = "0310";
    private final long discordId = 221991565567066112L;

    DiscordMinecraftLink linkForMinecraft;
    DiscordMinecraftLink linkForDiscord;

    @BeforeEach
    void setUp() {
        try {
            Main.setConfig(new JavajaotanConfig());
        } catch (RuntimeException e) {
            return; // CI用
        }
        try {
            linkForMinecraft = DiscordMinecraftLink.get(minecraftUUID);
            linkForDiscord = DiscordMinecraftLink.get(discordId);
        } catch (SQLException e) {
            e.printStackTrace();
            fail();
        }
        assertNotNull(linkForMinecraft);
        assertNotNull(linkForDiscord);
    }

    @Test
    void isFound() {
        if (linkForMinecraft == null || linkForDiscord == null) {
            return; // CI用
        }
        assertTrue(linkForMinecraft.isFound());
        assertTrue(linkForDiscord.isFound());
    }

    @Test
    void isLinked() {
        if (linkForMinecraft == null || linkForDiscord == null) {
            return; // CI用
        }
        assertTrue(linkForMinecraft.isLinked());
        assertTrue(linkForDiscord.isLinked());
    }

    @Test
    void getMinecraftName() {
        if (linkForMinecraft == null || linkForDiscord == null) {
            return; // CI用
        }
        assertEquals(minecraftName, linkForMinecraft.getMinecraftName());
        assertEquals(minecraftName, linkForDiscord.getMinecraftName());
    }

    @Test
    void getMinecraftUUID() {
        if (linkForMinecraft == null || linkForDiscord == null) {
            return; // CI用
        }
        assertEquals(minecraftUUID, linkForMinecraft.getMinecraftUUID());
        assertEquals(minecraftUUID, linkForDiscord.getMinecraftUUID());
    }

    @Test
    void getDiscordId() {
        if (linkForMinecraft == null || linkForDiscord == null) {
            return; // CI用
        }
        assertEquals(Long.toUnsignedString(discordId), linkForMinecraft.getDiscordId());
        assertEquals(Long.toUnsignedString(discordId), linkForDiscord.getDiscordId());
    }

    @Test
    void getDiscordName() {
        if (linkForMinecraft == null || linkForDiscord == null) {
            return; // CI用
        }
        assertEquals(discordName, linkForMinecraft.getDiscordName());
        assertEquals(discordName, linkForDiscord.getDiscordName());
    }

    @Test
    void getDiscordDiscriminator() {
        if (linkForMinecraft == null || linkForDiscord == null) {
            return; // CI用
        }
        assertEquals(discordDiscriminator, linkForMinecraft.getDiscordDiscriminator());
        assertEquals(discordDiscriminator, linkForDiscord.getDiscordDiscriminator());
    }

    @Test
    void getDisconnectPermGroup() {
        if (linkForMinecraft == null || linkForDiscord == null) {
            return; // CI用
        }
        assertNull(linkForMinecraft.getDisconnectPermGroup());
        assertNull(linkForDiscord.getDisconnectPermGroup());
    }

    @Test
    void getAllForMinecraft() throws SQLException {
        if (linkForMinecraft == null || linkForDiscord == null) {
            return; // CI用
        }
        List<DiscordMinecraftLink> connections = DiscordMinecraftLink.getAllForMinecraft();
        DiscordMinecraftLink dml = connections
            .stream()
            .filter(c -> c.getMinecraftUUID().equals(minecraftUUID))
            .findFirst()
            .orElse(null);
        assertNotNull(dml);
        assertTrue(dml.isFound());
        assertTrue(dml.isLinked());
    }

    @Test
    void getAllForDiscord() throws SQLException {
        if (linkForMinecraft == null || linkForDiscord == null) {
            return; // CI用
        }
        List<DiscordMinecraftLink> connections = DiscordMinecraftLink.getAllForMinecraft();
        DiscordMinecraftLink dml = connections
            .stream()
            .filter(c -> c.getDiscordId().equals(Long.toUnsignedString(discordId)))
            .findFirst()
            .orElse(null);
        assertNotNull(dml);
        assertTrue(dml.isFound());
        assertTrue(dml.isLinked());
    }
}