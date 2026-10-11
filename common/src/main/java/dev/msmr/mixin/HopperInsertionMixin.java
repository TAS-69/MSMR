package dev.msmr.mixin;

import dev.msmr.ServerStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperInsertionMixin {
    @Inject(method="tryMoveInItem",at=@At("HEAD"),cancellable=true)
    private static void msmr$insert(Container source,Container destination,ItemStack stack,int slot,Direction direction,CallbackInfoReturnable<ItemStack> ci) {
        if(ServerStorage.guarded(destination,slot)) ci.setReturnValue(ServerStorage.insert(destination,slot,stack,false));
    }
}
