package com.totempopchams;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Verwaltet die Geister. Ein Geist ist nur ein Datensatz (Position, Blickrichtung, Alter),
 * gezeichnet wird er in {@link GhostRenderer}. Es können beliebig viele gleichzeitig existieren
 * (bis zur Obergrenze aus den Einstellungen).
 */
public final class GhostManager {

    /** Unveränderliche Momentaufnahme eines Geists für das Zeichnen. */
    public record Snapshot(float x, float y, float z, float yawDegrees, float alpha) {
    }

    private static final class Ghost {
        final double x;
        final double y;
        final double z;
        final float yaw;
        int age = 0;

        Ghost(double x, double y, double z, float yaw) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }
    }

    private static final List<Ghost> GHOSTS = new ArrayList<>();

    private GhostManager() {
    }

    public static void spawn(AbstractClientPlayer player) {
        if (!GhostConfig.ownGhost && player == Minecraft.getInstance().player) {
            return;
        }
        while (GHOSTS.size() >= GhostConfig.maxGhosts && !GHOSTS.isEmpty()) {
            GHOSTS.remove(0);
        }
        GHOSTS.add(new Ghost(player.getX(), player.getY(), player.getZ(), player.yBodyRot));
    }

    public static void tick() {
        int lifetime = GhostConfig.lifetimeSeconds * 20;
        Iterator<Ghost> it = GHOSTS.iterator();
        while (it.hasNext()) {
            Ghost g = it.next();
            if (++g.age >= lifetime) {
                it.remove();
            }
        }
        while (GHOSTS.size() > GhostConfig.maxGhosts) {
            GHOSTS.remove(0);
        }
    }

    public static void clear() {
        GHOSTS.clear();
    }

    /** Liefert die aktuellen Geister samt Deckkraft (blendet am Ende der Lebenszeit aus). */
    public static List<Snapshot> snapshot() {
        int lifetime = Math.max(1, GhostConfig.lifetimeSeconds * 20);
        float base = GhostConfig.opacity / 100.0f;
        List<Snapshot> out = new ArrayList<>(GHOSTS.size());
        for (Ghost g : GHOSTS) {
            float left = (lifetime - g.age) / (lifetime * 0.35f);
            float fade = Math.max(0.0f, Math.min(1.0f, left));
            out.add(new Snapshot((float) g.x, (float) g.y, (float) g.z, g.yaw, base * fade));
        }
        return List.copyOf(out);
    }
}
