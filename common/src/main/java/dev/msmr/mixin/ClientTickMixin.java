package dev.msmr.mixin;
import dev.msmr.client.ClientTransfers;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class ClientTickMixin {
    @Inject(method="tick",at=@At("TAIL"))
    private void msmr$tick(CallbackInfo ci) { dev.msmr.client.ClientRules.refresh(); ClientTransfers.tick(); }
}
