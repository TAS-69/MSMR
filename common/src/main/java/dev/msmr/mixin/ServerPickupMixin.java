package dev.msmr.mixin;

import dev.msmr.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve vanilla/loader pickup hooks while inserting only into permitted slots. */
@Mixin(ItemEntity.class)
public abstract class ServerPickupMixin {
    @Shadow @Final private static EntityDataAccessor<ItemStack> DATA_ITEM;
    @Unique private int msmr$moved=-1;
    @Inject(method="playerTouch",at=@At("HEAD"))
    private void msmr$reset(net.minecraft.world.entity.player.Player player,CallbackInfo ci) { msmr$moved=-1; }
    @Redirect(method="playerTouch",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean msmr$insert(Inventory inventory,ItemStack stack) {
        FilterSnapshot rules=ServerPickupRules.forPlayer(inventory.player);
        if(rules==null || rules.inventory().forPickups().unrestricted() && rules.slots().values().stream().allMatch(r -> r.forPickups().unrestricted())) return inventory.add(stack);
        int before=stack.getCount();
        boolean inserted=PickupFilters.insert(inventory,stack,rules.inventory(),rules.slots());
        msmr$moved=before-stack.getCount();
        if(inserted && !stack.isEmpty()) ((ItemEntity)(Object)this).getEntityData().set(DATA_ITEM,stack.copy(),true);
        return inserted;
    }
    @ModifyArg(method="playerTouch",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;take(Lnet/minecraft/world/entity/Entity;I)V"),index=1)
    private int msmr$animationCount(int vanilla) { return msmr$moved<0?vanilla:msmr$moved; }
    @ModifyArg(method="playerTouch",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;awardStat(Lnet/minecraft/stats/Stat;I)V"),index=1)
    private int msmr$statCount(int vanilla) { return msmr$moved<0?vanilla:msmr$moved; }
}
