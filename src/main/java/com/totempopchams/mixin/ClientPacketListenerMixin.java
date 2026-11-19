package com.totempopchams.mixin;

import com.totempopchams.GhostManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Shadow
    private ClientLevel level;

    /** Event-ID 35 = Totem der Unsterblichkeit ausgelöst. */
    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void totempopchams$onTotemPop(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (packet.getEventId() != 35 || this.level == null) {
            return;
        }
        Entity entity = packet.getEntity(this.level);
        if (entity instanceof AbstractClientPlayer player) {
            GhostManager.spawn(player);
        }
    }
}
