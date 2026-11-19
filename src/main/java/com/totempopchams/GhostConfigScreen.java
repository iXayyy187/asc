package com.totempopchams;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Einstellungsmenü (Standard-Taste: K). */
public class GhostConfigScreen extends Screen {

    private static final String[] COLOR_NAMES = {
            "Blau", "Hellblau", "Cyan", "Grün", "Gelb", "Orange",
            "Rot", "Pink", "Lila", "Weiß", "Grau", "Schwarz"
    };
    private static final int[] COLORS = {
            0x3F7FFF, 0x55AAFF, 0x00E5FF, 0x3DDC55, 0xFFE83D, 0xFF9A2E,
            0xFF3D3D, 0xFF6EC7, 0xA64DFF, 0xFFFFFF, 0x9A9A9A, 0x202020
    };

    private static final int[] OPACITIES = {20, 30, 40, 50, 60, 70, 80, 100};
    private static final int[] LIFETIMES = {2, 3, 4, 6, 8, 10, 15};
    private static final int[] MAX_GHOSTS = {1, 2, 3, 5, 10, 20, 32};

    private final Screen parent;

    private Button opacityButton;
    private Button lifetimeButton;
    private Button maxButton;
    private Button ownButton;
    private Button wallsButton;

    private int top;

    public GhostConfigScreen(Screen parent) {
        super(Component.literal("Totem Pop Chams"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        this.top = Math.max(20, this.height / 2 - 115);

        // Farb-Auswahl: 2 Reihen mit je 6 Farben
        int bw = 52;
        int gap = 4;
        int startX = cx - (6 * bw + 5 * gap) / 2;
        for (int i = 0; i < COLORS.length; i++) {
            final int rgb = COLORS[i];
            int col = i % 6;
            int row = i / 6;
            Component label = Component.literal(COLOR_NAMES[i]).withStyle(style -> style.withColor(rgb));
            Button colorButton = Button.builder(label, btn -> GhostConfig.color = rgb)
                    .bounds(startX + col * (bw + gap), this.top + 20 + row * 24, bw, 20)
                    .build();
            this.addRenderableWidget(colorButton);
        }

        int w = 200;
        int x = cx - w / 2;
        int y = this.top + 80;

        this.opacityButton = Button.builder(Component.empty(), btn -> {
            GhostConfig.opacity = next(OPACITIES, GhostConfig.opacity);
            refresh();
        }).bounds(x, y, w, 20).build();
        this.addRenderableWidget(this.opacityButton);

        this.lifetimeButton = Button.builder(Component.empty(), btn -> {
            GhostConfig.lifetimeSeconds = next(LIFETIMES, GhostConfig.lifetimeSeconds);
            refresh();
        }).bounds(x, y + 24, w, 20).build();
        this.addRenderableWidget(this.lifetimeButton);

        this.maxButton = Button.builder(Component.empty(), btn -> {
            GhostConfig.maxGhosts = next(MAX_GHOSTS, GhostConfig.maxGhosts);
            refresh();
        }).bounds(x, y + 48, w, 20).build();
        this.addRenderableWidget(this.maxButton);

        this.ownButton = Button.builder(Component.empty(), btn -> {
            GhostConfig.ownGhost = !GhostConfig.ownGhost;
            refresh();
        }).bounds(x, y + 72, w, 20).build();
        this.addRenderableWidget(this.ownButton);

        this.wallsButton = Button.builder(Component.empty(), btn -> {
            GhostConfig.throughWalls = !GhostConfig.throughWalls;
            refresh();
        }).bounds(x, y + 96, w, 20).build();
        this.addRenderableWidget(this.wallsButton);

        Button done = Button.builder(Component.literal("Fertig"), btn -> this.onClose())
                .bounds(x, y + 126, w, 20)
                .build();
        this.addRenderableWidget(done);

        refresh();
    }

    private void refresh() {
        this.opacityButton.setMessage(Component.literal("Deckkraft: " + GhostConfig.opacity + " %"));
        this.lifetimeButton.setMessage(Component.literal("Dauer: " + GhostConfig.lifetimeSeconds + " s"));
        this.maxButton.setMessage(Component.literal("Max. Geister gleichzeitig: " + GhostConfig.maxGhosts));
        this.ownButton.setMessage(Component.literal("Eigener Geist: " + (GhostConfig.ownGhost ? "An" : "Aus")));
        this.wallsButton.setMessage(Component.literal("Durch Wände sichtbar: " + (GhostConfig.throughWalls ? "An" : "Aus")));
    }

    private static int next(int[] values, int current) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                return values[(i + 1) % values.length];
            }
        }
        return values[0];
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String title = "Totem Pop Chams - Geist-Einstellungen";
        graphics.text(this.font, title, this.width / 2 - this.font.width(title) / 2, this.top, 0xFFFFFFFF, true);

        String colorText = "Aktuelle Farbe: " + String.format("#%06X", GhostConfig.color & 0xFFFFFF);
        graphics.text(this.font, colorText, this.width / 2 - this.font.width(colorText) / 2, this.top + 68,
                0xFF000000 | (GhostConfig.color & 0xFFFFFF), true);
    }

    @Override
    public void onClose() {
        GhostConfig.save();
        this.minecraft.gui.setScreen(this.parent);
    }
}
