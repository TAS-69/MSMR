package dev.msmr.mixin;

import dev.msmr.ServerStorage;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class ServerSlotMixin {
    @Inject(method="mayPlace",at=@At("HEAD"),cancellable=true)
    private void msmr$accept(ItemStack item,CallbackInfoReturnable<Boolean> ci) { if(ServerStorage.slotCap((Slot)(Object)this,item)==0) ci.setReturnValue(false); }
    @Inject(method="getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I",at=@At("RETURN"),cancellable=true)
    private void msmr$cap(ItemStack item,CallbackInfoReturnable<Integer> ci) { ci.setReturnValue(Math.min(ci.getReturnValue(),ServerStorage.slotCap((Slot)(Object)this,item))); }
}
