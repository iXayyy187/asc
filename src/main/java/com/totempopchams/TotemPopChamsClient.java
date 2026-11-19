package com.totempopchams;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class TotemPopChamsClient implements ClientModInitializer {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("totempopchams", "main"));

    private static KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        GhostConfig.load();

        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.totempopchams.open_menu",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_K,
                CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                client.gui.setScreen(new GhostConfigScreen(null));
            }

            if (client.level == null) {
                GhostManager.clear();
            } else if (!client.isPaused()) {
                GhostManager.tick();
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> GhostManager.clear());

        LevelExtractionEvents.END_EXTRACTION.register(GhostRenderer::extract);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(GhostRenderer::draw);

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> GhostRenderer.close());
    }
}
