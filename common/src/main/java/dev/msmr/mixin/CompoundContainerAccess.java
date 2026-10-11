package dev.msmr.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompoundContainer.class)
public interface CompoundContainerAccess {
    @Accessor("container1") Container msmr$first();
    @Accessor("container2") Container msmr$second();
}
