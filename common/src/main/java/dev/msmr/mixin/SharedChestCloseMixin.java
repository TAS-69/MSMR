package dev.msmr.mixin;

import dev.msmr.client.ClientChestSync;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class SharedChestCloseMixin {
    @Inject(method="closeContainer",at=@At("HEAD"),cancellable=true)
    private void msmr$finishEdits(CallbackInfo ci) { if(ClientChestSync.deferClose()) ci.cancel(); }
}
