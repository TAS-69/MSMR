package dev.msmr.fabric.mixin;

import dev.msmr.ServerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.impl.transfer.item.InventoryStorageImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="net.fabricmc.fabric.impl.transfer.item.InventorySlotWrapper",remap=false)
public abstract class FabricStorageMixin {
    @Shadow @Final private InventoryStorageImpl storage;
    @Shadow @Final private int slot;
    @Inject(method="getCapacity(Lnet/fabricmc/fabric/api/transfer/v1/item/ItemVariant;)I",at=@At("RETURN"),cancellable=true,remap=false)
    private void msmr$capacity(ItemVariant item,CallbackInfoReturnable<Integer> ci) {
        var container=((InventoryStorageAccess)storage).msmr$inventory();
        if(ServerStorage.guarded(container,slot)) ci.setReturnValue(Math.min(ci.getReturnValue(),ServerStorage.cap(container,slot,item.toStack(1))));
    }
}
