package dev.msmr.mixin;

import dev.msmr.client.ClientRules;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ChestBlockUpdateMixin {
    @Inject(method="setServerVerifiedBlockState",at=@At("HEAD"))
    private void msmr$storageChanged(BlockPos pos,BlockState state,int flags,CallbackInfo ci) {
        ClientRules.storageChanged((ClientLevel)(Object)this,pos,state);
    }
}
