package dev.msmr.mixin;

import dev.msmr.ServerPickupRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class PlayerRespawnMixin {
    @Inject(method="respawn",at=@At("RETURN"))
    private void msmr$respawn(ServerPlayer original,boolean keepInventory,Entity.RemovalReason reason,CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPickupRules.respawn(original,cir.getReturnValue());
    }
}
