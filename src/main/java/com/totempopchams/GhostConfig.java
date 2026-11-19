package com.totempopchams;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Einstellungen der Mod. Werden in config/totempopchams.json gespeichert. */
public final class GhostConfig {

    /** Geist-Farbe als RGB (0xRRGGBB). */
    public static int color = 0x55AAFF;
    /** Deckkraft in Prozent (10 bis 100). */
    public static int opacity = 55;
    /** Wie lange ein Geist stehen bleibt, in Sekunden. */
    public static int lifetimeSeconds = 4;
    /** Maximale Anzahl gleichzeitiger Geister. */
    public static int maxGhosts = 10;
    /** Soll auch dein eigener Totem-Pop einen Geist erzeugen? */
    public static boolean ownGhost = true;
    /** Geist auch durch Wände sichtbar? */
    public static boolean throughWalls = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Data {
        int color = GhostConfig.color;
        int opacity = GhostConfig.opacity;
        int lifetimeSeconds = GhostConfig.lifetimeSeconds;
        int maxGhosts = GhostConfig.maxGhosts;
        boolean ownGhost = GhostConfig.ownGhost;
        boolean throughWalls = GhostConfig.throughWalls;
    }

    private GhostConfig() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("totempopchams.json");
    }

    public static void load() {
        try {
            Path path = file();
            if (!Files.exists(path)) {
                save();
                return;
            }
            Data d = GSON.fromJson(Files.readString(path), Data.class);
            if (d == null) {
                return;
            }
            color = d.color & 0xFFFFFF;
            opacity = Math.max(10, Math.min(100, d.opacity));
            lifetimeSeconds = Math.max(1, Math.min(30, d.lifetimeSeconds));
            maxGhosts = Math.max(1, Math.min(64, d.maxGhosts));
            ownGhost = d.ownGhost;
            throughWalls = d.throughWalls;
        } catch (IOException | RuntimeException e) {
            // Kaputte Datei: einfach die Standardwerte behalten.
        }
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(new Data()));
        } catch (IOException e) {
            // Speichern ist nicht kritisch.
        }
    }
}
