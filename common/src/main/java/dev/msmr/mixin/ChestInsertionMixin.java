package dev.msmr.mixin;

import dev.msmr.ServerStorage;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BaseContainerBlockEntity.class)
public abstract class ChestInsertionMixin implements Container {
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return !ServerStorage.guarded(this,slot) || ServerStorage.cap(this,slot,stack)>getItem(slot).getCount(); }
}
