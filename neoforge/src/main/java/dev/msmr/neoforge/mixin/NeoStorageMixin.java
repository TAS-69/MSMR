package dev.msmr.neoforge.mixin;

import dev.msmr.ServerStorage;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=InvWrapper.class,remap=false)
public abstract class NeoStorageMixin {
    @Shadow public abstract Container getInv();
    @Inject(method="insertItem",at=@At("HEAD"),cancellable=true,remap=false)
    private void msmr$insert(int slot,ItemStack stack,boolean simulate,CallbackInfoReturnable<ItemStack> ci) {
        if(slot>=0 && slot<getInv().getContainerSize() && ServerStorage.guarded(getInv(),slot)) ci.setReturnValue(ServerStorage.insert(getInv(),slot,stack,simulate));
    }
}
