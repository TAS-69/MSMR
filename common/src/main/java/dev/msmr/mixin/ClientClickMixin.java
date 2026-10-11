package dev.msmr.mixin;

import dev.msmr.Rule;
import dev.msmr.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientClickMixin {
    @Inject(method="handleInventoryMouseClick",at=@At("HEAD"),cancellable=true)
    private void msmr$click(int menuId,int slotId,int button,ClickType type,Player player,CallbackInfo ci) {
        if(ClientTransfers.emitting) return;
        if(ClientTransfers.busy()) { ci.cancel(); return; }
        ClientRules.refresh();
        AbstractContainerMenu menu=player.containerMenu;
        if(menu.containerId!=menuId) return;
        if(type==ClickType.QUICK_CRAFT && ClientRules.hasFilters()) { ci.cancel(); return; }
        if(slotId<0 || slotId>=menu.slots.size()) return;
        Slot slot=menu.getSlot(slotId);
        Rule rule=ClientRules.moveRule(slot);
        ItemStack cursor=menu.getCarried(), existing=slot.getItem();
        if(type==ClickType.QUICK_MOVE) {
            if(ClientTransfers.quickMove(slotId)) ci.cancel();
        } else if(type==ClickType.PICKUP && !cursor.isEmpty()) {
            boolean merge=existing.isEmpty() || ItemStack.isSameItemSameComponents(existing,cursor);
            // Clicking an output to collect into the cursor is extraction, not insertion.
            if(!slot.mayPlace(cursor)) return;
            int amount=merge ? Math.min(button==1?1:cursor.getCount(),Math.max(0,slot.getMaxStackSize(cursor)-existing.getCount())) : cursor.getCount();
            if(!ClickPolicy.permits(rule,existing,cursor,amount) || !ClickPolicy.permits(ClientRules.moveInventoryFilter(slot),existing,cursor,amount)) ci.cancel();
        } else if(type==ClickType.SWAP && (button>=0 && button<9 || button==40)) {
            ItemStack incoming=player.getInventory().getItem(button);
            if(!ClickPolicy.permits(rule,ItemStack.EMPTY,incoming,incoming.getCount()) || !ClientRules.moveInventoryFilter(slot).accepts(incoming)) { ci.cancel(); return; }
            for(Slot hotbar:menu.slots) if(hotbar.container==player.getInventory() && hotbar.getContainerSlot()==button
                && (!ClickPolicy.permits(ClientRules.moveRule(hotbar),ItemStack.EMPTY,existing,existing.getCount()) || !ClientRules.moveInventoryFilter(hotbar).accepts(existing))) { ci.cancel(); return; }
        }
    }
}
