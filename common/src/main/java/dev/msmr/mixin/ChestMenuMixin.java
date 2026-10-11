package dev.msmr.mixin;

import dev.msmr.client.ChestTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ChestMenuMixin {
    @Inject(method="handleOpenScreen",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/MenuScreens;create(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/client/Minecraft;ILnet/minecraft/network/chat/Component;)V"))
    private void msmr$storageMenu(ClientboundOpenScreenPacket packet,CallbackInfo ci) { ChestTracker.opening(packet); }
}
