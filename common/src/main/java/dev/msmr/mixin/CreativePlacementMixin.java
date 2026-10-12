package dev.msmr.mixin;

import dev.msmr.ServerStorage;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Creative palette packets use direct assignments instead of Slot.mayPlace. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CreativePlacementMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleSetCreativeModeSlot",at=@At("HEAD"),cancellable=true)
    private void msmr$creative(ServerboundSetCreativeModeSlotPacket packet,CallbackInfo ci) {
        if(!player.getServer().isSameThread() || !player.gameMode.isCreative() || packet.slotNum()<1 || packet.slotNum()>45 || packet.itemStack().isEmpty()) return;
        var slot=player.inventoryMenu.getSlot(packet.slotNum());
        if(packet.itemStack().getCount()>ServerStorage.slotCap(slot,packet.itemStack())) {
            player.inventoryMenu.sendAllDataToRemote(); ci.cancel();
        }
    }
}
