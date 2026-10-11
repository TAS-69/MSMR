package dev.msmr.mixin;

import dev.msmr.client.ChestTracker;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.*;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class ChestInteractionMixin {
    @Inject(method="useItemOn",at=@At("RETURN"))
    private void msmr$storageInteraction(LocalPlayer player,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> cir) {
        ChestTracker.interacted(hit.getBlockPos(),cir.getReturnValue().consumesAction());
    }
}
